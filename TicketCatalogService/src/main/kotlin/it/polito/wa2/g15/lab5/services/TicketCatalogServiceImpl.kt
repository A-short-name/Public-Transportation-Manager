package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.BuyTicketDTO
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketOrderException
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketRestrictionException
import it.polito.wa2.g15.lab5.kafka.OrderInformationMessage
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import it.polito.wa2.g15.lab5.security.JwtUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext


@Service
class TicketCatalogServiceImpl : TicketCatalogService {
    @Autowired
    lateinit var ticketItemRepository: TicketItemRepository

    @Autowired
    lateinit var ticketOrderService: TicketOrderService

    @Value("\${kafka.topics.produce}")
    lateinit var topic: String
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, OrderInformationMessage>

    private val logger = KotlinLogging.logger {}

    //Defined in config class
    @Autowired
    lateinit var client : WebClient



    override fun getAllTicketItems(): Flow<TicketItem> {
        return ticketItemRepository.findAll()
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
        val ctx: CoroutineContext = Dispatchers.IO

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
                .uri("/services/user/$userName/profile/")
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