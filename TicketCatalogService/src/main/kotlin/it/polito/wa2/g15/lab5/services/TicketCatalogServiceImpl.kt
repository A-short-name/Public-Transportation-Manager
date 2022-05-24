package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.BuyTicketDTO
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketOrderException
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketRestrictionException
import it.polito.wa2.g15.lab5.kafka.OrderInformationMessage
import it.polito.wa2.g15.lab5.kafka.OrderProcessedMessage
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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

    override fun getAllTicketItems() : Flow<TicketItem> {
        return ticketItemRepository.findAll()
    }

    override suspend fun addNewTicketType(newTicketItemDTO: NewTicketItemDTO) {
        val ticketItem = TicketItem(
            ticketType = newTicketItemDTO.type,
            price = newTicketItemDTO.price,
            minAge = newTicketItemDTO.minAge,
            maxAge = newTicketItemDTO.maxAge
        )

        try {
            ticketItemRepository.save(ticketItem)
        } catch (e: Exception) {
            throw Exception("Failed saving ticketItem: ${e.message}")
        }
    }

    override suspend fun buyTicket(mBuyTicketDTO: Mono<BuyTicketDTO>, ticketId: Long, mUserName: Mono<String>) : Long {

        /*
        val userFuture= async {
            getTravelerInfo(mUserName)
        }
        */


        val ticket = ticketItemRepository.findById(ticketId) ?: throw InvalidTicketOrderException("Ticket Not Found")

        val buyTicketDTO = mBuyTicketDTO.awaitSingle()
        val ticketPrice = ticket.price
        val totalPrice = buyTicketDTO.numOfTickets * ticketPrice
        if(ticketHasRestriction(ticket)){

            getTravelerInfo(mUserName)

            TODO("implement the cancellation of the other coroutine because the user can not perform the order")
        }
        val username = mUserName.awaitSingle()
        logger.info("order request received from user $username for ${buyTicketDTO.numOfTickets } ticket $ticketId" +
                "\n the user want to pay with ${buyTicketDTO.paymentInfo}" +
                "\n\t totalPrice = $totalPrice")

        val order = ticketOrderService.savePendingOrder(
            totalPrice = totalPrice,
            username = username,
            ticketId = ticketId,
            quantity = buyTicketDTO.numOfTickets

        )
        logger.info("order $order set pending")

        publishOrderOnKafka(buyTicketDTO, order)

        return order.orderId ?: throw InvalidTicketOrderException("order id not saved correctly in the db")


    }

    private fun getTravelerInfo(mUserName: Mono<String>) {
        // Use this to contact the travelerService:
        // Client is a webClient (val client = WebClient.create() ??) It should be in the consturctor of the controller
//        client
//            .get()
//            .uri("/suspend")
//            .accept(MediaType.APPLICATION_JSON)
//            .awaitExchange()
//            .awaitBody<Banner>()
        TODO("Not yet implemented")
    }

    /**
     * publish on kafka the event of the pending order
     */
    private fun publishOrderOnKafka(buyTicketDTO: BuyTicketDTO, ticketOrder: TicketOrder) {
        val message: Message<OrderInformationMessage> = MessageBuilder
                .withPayload(OrderInformationMessage(buyTicketDTO.paymentInfo, ticketOrder.totalPrice, ticketOrder.username, ticketOrder.ticketId))
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