package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.TicketForTravelerDTO
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketOrderException
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketRestrictionException
import it.polito.wa2.g15.lab5.kafka.OrderProcessedMessage
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import it.polito.wa2.g15.lab5.repositories.TicketOrderRepository
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import java.time.ZonedDateTime

@Service
class TicketOrderServiceImpl : TicketOrderService {

    @Autowired
    lateinit var ticketOrderRepository: TicketOrderRepository
    /*  Not possible for circular dependencies
    @Autowired
    lateinit var ticketCatalogService: TicketCatalogServiceImpl
    */
    @Autowired
    lateinit var ticketItemRepository: TicketItemRepository

    private val logger = KotlinLogging.logger {}
    override suspend fun getAllTicketOrders(): Flow<TicketOrder> {
        return ticketOrderRepository.findAll()
    }

    @Autowired
    lateinit var client: WebClient

    override fun getUserTicketOrders(username: String): Flow<TicketOrder> {

            return ticketOrderRepository.findTicketOrdersByUsername(username)
    }

    override suspend fun savePendingOrder(totalPrice: Double, username: String, ticketId :Long, quantity: Int, validFrom: ZonedDateTime, zid:String): TicketOrder {
        val ticketOrder = TicketOrder(
            orderState = "PENDING",
            totalPrice = totalPrice,
            username = username,
            ticketId = ticketId,
            quantity = quantity,
            validFrom = validFrom,
            zid = zid
        )
        logger.info("save pending order: $ticketOrder")
        return ticketOrderRepository.save(ticketOrder)
    }

    override suspend fun getTicketOrderById(orderId: Long, username: String): TicketOrder? {
        return ticketOrderRepository.findTicketOrderByOrderIdAndUsername(orderId, username).firstOrNull()
    }


    @KafkaListener(topics = ["\${kafka.topics.consume}"], groupId = "onlyOneGroup")
    fun updateStatus(message: OrderProcessedMessage) {
        logger.info("Message received {}", message)
        CoroutineScope(CoroutineName("Obliged coroutines")).also { it.launch { updateStatusSuspendable(message) } }
    }
    suspend fun updateStatusSuspendable(message: OrderProcessedMessage) {
        val pendingTicketOrder: TicketOrder?
        try {
            pendingTicketOrder = ticketOrderRepository.findById(message.orderId)
            } catch (e: Exception){
                throw InvalidTicketOrderException("Error updating ticketOrder status: ${e.message}")
            }
        if(pendingTicketOrder == null)
            throw InvalidTicketOrderException("No ticket order with such id")
        if(message.accepted) pendingTicketOrder.orderState = "COMPLETED" else pendingTicketOrder.orderState = "CANCELLED"

        try {
            ticketOrderRepository.save(pendingTicketOrder)
            if(pendingTicketOrder.orderState=="COMPLETED")
                postTicketInfo(pendingTicketOrder)
        } catch (e: Exception){
            throw InvalidTicketOrderException("Error updating ticketOrder status: ${e.message}")
        }
    }

    private suspend fun postTicketInfo(ticketOrder: TicketOrder) {
        val ticket = ticketItemRepository.findById(ticketOrder.ticketId)!!
        val ticketForTraveler= TicketForTravelerDTO(ticket.duration, ticket.ticketType, ticketOrder.validFrom, ticketOrder.zid, ticketOrder.quantity)

        client.post()
            .uri("/services/user/${ticketOrder.username}/tickets/add/")
            .bodyValue(ticketForTraveler)
            .awaitExchange {
                if (it.statusCode() != HttpStatus.ACCEPTED)
                    throw InvalidTicketRestrictionException("Post for ticket failed")
            }
        logger.info { "Ticket post successful" }
    }
}