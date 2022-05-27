package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.BuyTicketDTO
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketOrderException
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketRestrictionException
import it.polito.wa2.g15.lab5.kafka.OrderInformationMessage
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class TicketCatalogServiceImpl : TicketCatalogService {
    @Autowired
    lateinit var ticketItemRepository: TicketItemRepository

    lateinit var ticketItemsCache : MutableList<TicketItem>

    @Autowired
    lateinit var ticketOrderService: TicketOrderService

    @Value("\${kafka.topics.produce}")
    lateinit var topic: String

    @Value("\${ticket.catalog.cache}")
    lateinit var ticketCatalogCacheStatus :String

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, OrderInformationMessage>

    private val logger = KotlinLogging.logger {}

    //Defined in config class
    @Autowired
    lateinit var client : WebClient

    private fun isCacheEnabled():Boolean{
        return ticketCatalogCacheStatus=="enabled"
    }

    override suspend fun initTicketCatalogCache() {
        runBlocking {
            if (ticketCatalogCacheStatus == "enabled") {
                logger.info { "start initialization ticketItem cache ..." }
                ticketItemsCache = ticketItemRepository.findAll().toList().toMutableList()
                logger.info { "... initialization ticketItem cache finished" }
                logger.info { "these are the ticket found in the catalog during the startup:\n $ticketItemsCache" }
            } else ticketItemsCache = mutableListOf()
        }
    }
    override fun getAllTicketItems(): Flow<TicketItem> {
        return if(isCacheEnabled())
            ticketItemsCache.asFlow()
        else
            ticketItemRepository.findAll()
    }

    override suspend fun addNewTicketType(newTicketItemDTO: NewTicketItemDTO) : Long {
        var ticketItem = TicketItem(
                ticketType = newTicketItemDTO.type,
                price = newTicketItemDTO.price,
                minAge = newTicketItemDTO.minAge,
                maxAge = newTicketItemDTO.maxAge,
                duration = newTicketItemDTO.duration
        )

        try {
            ticketItem=ticketItemRepository.save(ticketItem)
            if(isCacheEnabled()) {
                logger.info { "Updating cache..." }
                ticketItemsCache.add(ticketItem)
            }
        } catch (e: Exception) {
            throw Exception("Failed saving ticketItem: ${e.message}")
        }
        
        return ticketItem.id ?: throw InvalidTicketOrderException("order id not saved correctly in the db")
    }

    private fun checkRestriction(userAge: Int, ticketRequested: TicketItem): Boolean {
        if(userAge>ticketRequested.minAge!! && userAge<ticketRequested.maxAge!!)
            return true
        return false
    }

    override suspend fun buyTicket(buyTicketDTO: BuyTicketDTO, ticketId: Long, userName: String): Long = coroutineScope()
    {
        logger.info("ctx: ${this.coroutineContext.job} \t start buying info ")
        val ticketRequested =
                withContext(Dispatchers.IO + CoroutineName("find ticket")) {
                    logger.info("ctx:  ${this.coroutineContext.job} \t searching ticket info")
                    ticketItemRepository.findById(ticketId) ?: throw InvalidTicketOrderException("Ticket Not Found")
                }

        if (ticketHasRestriction(ticketRequested)) {
            val travelerAge =
                    //async(Dispatcher.IO + CoroutineName("find user age")
                    withContext(Dispatchers.IO + CoroutineName("find user age")) {
                        logger.info("ctx:  ${this.coroutineContext.job} \t searching user age")
                        getTravelerAge(userName)
                    }
            if (!checkRestriction(travelerAge, ticketRequested))
                throw InvalidTicketRestrictionException("User $userName is $travelerAge years old and can not buy" +
                        " ticket $ticketId")

        }

        val ticketPrice = ticketRequested.price * buyTicketDTO.zid.length
        val totalPrice = buyTicketDTO.numOfTickets * ticketPrice

        logger.info("ctx: ${this.coroutineContext.job}\t order request received from user $userName for ${buyTicketDTO.numOfTickets} ticket $ticketId" +
                "\n the user want to pay with ${buyTicketDTO.paymentInfo}" +
                "\n\t totalPrice = $totalPrice")

        val order = withContext(Dispatchers.IO + CoroutineName("save pending order")) {

            ticketOrderService.savePendingOrder(
                    totalPrice = totalPrice,
                    username = userName,
                    ticketId = ticketId,
                    quantity = buyTicketDTO.numOfTickets,
                    validFrom = buyTicketDTO.validFrom,
                    zid = buyTicketDTO.zid
            )
        }
        logger.info("order $order set pending")

        publishOrderOnKafka(buyTicketDTO, order)


        order.orderId ?: throw InvalidTicketOrderException("order id not saved correctly in the db")

    }

    private suspend fun getTravelerAge(userName: String): Int {
        val age = client.get()
                .uri("/services/user/$userName/birthdate/")
                .awaitExchange {
                    if (it.statusCode() != HttpStatus.OK)
                        throw InvalidTicketRestrictionException("User info not found")
                    LocalDate.now()
                    ChronoUnit.YEARS.between(it.bodyToMono<LocalDate>().awaitSingle(), LocalDate.now())
                }
        logger.info { "User ($userName) age is: $age" }
        return age.toInt()
    }


    /**
     * publish on kafka the event of the pending order
     */
    private fun publishOrderOnKafka(buyTicketDTO: BuyTicketDTO, ticketOrder: TicketOrder) {
        val message: Message<OrderInformationMessage> = MessageBuilder
                .withPayload(OrderInformationMessage(buyTicketDTO.paymentInfo, ticketOrder.totalPrice, ticketOrder.username, ticketOrder.orderId!!))
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader("X-Custom-Header", "Custom header here")
                .build()
        kafkaTemplate.send(message)
        logger.info("Message sent with success on topic: $topic")
    }

    //Consume message is in order service


    /**
     * if the ticket has some restriction about the age or stuff like that return true, false otherwise
     */
    private fun ticketHasRestriction(ticket : TicketItem): Boolean {

        if(ticket.minAge == null && ticket.maxAge == null)
            return false
        else
            if(ticket.minAge != null && ticket.maxAge != null)
                if(ticket.minAge > ticket.maxAge)
                    throw InvalidTicketRestrictionException("ticket restriction is not valid, min age = ${ticket.minAge} > max age = ${ticket.maxAge}")

        return true
    }
}