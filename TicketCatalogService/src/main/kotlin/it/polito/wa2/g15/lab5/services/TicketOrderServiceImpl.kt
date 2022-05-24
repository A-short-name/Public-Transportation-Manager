package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketOrderException
import it.polito.wa2.g15.lab5.kafka.OrderProcessedMessage
import it.polito.wa2.g15.lab5.repositories.TicketOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TicketOrderServiceImpl : TicketOrderService {

    @Autowired
    lateinit var ticketOrderRepository: TicketOrderRepository

    private val logger = KotlinLogging.logger {}
    override suspend fun getAllTicketOrders(): Flow<TicketOrder> {
        return ticketOrderRepository.findAll()
    }

    override suspend fun getUserTicketOrders(username: Mono<String>): Flow<TicketOrder> {
        return ticketOrderRepository.findTicketOrdersByUsername(username.awaitSingle())
    }

    override suspend fun savePendingOrder(totalPrice: Double, username: String,ticketId :Long, quantity: Int): TicketOrder {
        val ticketOrder = TicketOrder(
            orderState = "PENDING",
            totalPrice = totalPrice,
            username = username,
            ticketId = ticketId,
            quantity = quantity
        )
        logger.info("save pending order: $ticketOrder")
        return ticketOrderRepository.save(ticketOrder)
    }

    override suspend fun getTicketOrderById(orderId: Long): TicketOrder {
        return ticketOrderRepository.findById(orderId) ?: throw InvalidTicketOrderException("ticket order $orderId not found")
    }


    @KafkaListener(topics = ["\${kafka.topics.consume}"], groupId = "ppr")
    suspend fun consumeMessage(message: OrderProcessedMessage) {
        logger.info("Message received {}", message)
        val pendingTicketOrder: TicketOrder?
        try {
            pendingTicketOrder = ticketOrderRepository.findById(message.orderId)
            } catch (e: Exception){
                throw InvalidTicketOrderException("Error updating ticketOrder status: ${e.message}")
            }
        if(pendingTicketOrder == null)
            throw InvalidTicketOrderException("No ticket order with such id")
        if(message.accepted) pendingTicketOrder.orderState = "COMPLETED" else pendingTicketOrder.orderState = "CANCELLED"
    }
}