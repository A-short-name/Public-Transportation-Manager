package it.polito.wa2.g15.lab5.services

import com.netflix.discovery.EurekaClient
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
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

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

    @Autowired
    lateinit var discoveryClient : EurekaClient

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
    fun updateStatus(message: OrderProcessedMessage, acknowledgment: Acknowledgment) {
        logger.info("Message received {}", message)
        CoroutineScope(CoroutineName("Obliged coroutines")).also { it.launch { updateStatusSuspendable(message, acknowledgment) } }
    }

    @Transactional //Not useful because of async repo
    suspend fun updateStatusSuspendable(message: OrderProcessedMessage, acknowledgment: Acknowledgment) {
        val pendingTicketOrder: TicketOrder?
        try {
            pendingTicketOrder = ticketOrderRepository.findById(message.orderId)
            } catch (e: Exception){
                throw InvalidTicketOrderException("Error updating ticketOrder status: ${e.message}")
            }
        if(pendingTicketOrder == null)
            throw InvalidTicketOrderException("No ticket order with such id")
        val previousStatus = pendingTicketOrder.orderState

        if(message.accepted) pendingTicketOrder.orderState = "COMPLETED" else pendingTicketOrder.orderState = "CANCELLED"

        try {
            ticketOrderRepository.save(pendingTicketOrder)
            } catch (e: Exception){
            throw InvalidTicketOrderException("Error updating ticketOrder status: ${e.message}")
        }
        try{
            if(pendingTicketOrder.orderState=="COMPLETED" && previousStatus=="PENDING") {
                postTicketInfo(pendingTicketOrder)
                //No exception thrown, so the ticket generation is completed
                acknowledgment.acknowledge()
            }
        } catch (e: Exception){
            //I will consume again the same message. Consider a retry strategy to not loop forever
            throw InvalidTicketOrderException("Error related to traveler service and tickets generation: ${e.message}")
        }
    }

    private suspend fun postTicketInfo(ticketOrder: TicketOrder) {
        val ticket = ticketItemRepository.findById(ticketOrder.ticketId)!!
        val ticketForTraveler= TicketForTravelerDTO(ticket.duration, ticket.ticketType, ticketOrder.validFrom, ticketOrder.zid, ticketOrder.quantity)

        try {
            val instanceInfo = discoveryClient.getNextServerFromEureka("traveler", false)
            val homePageUrl = instanceInfo.homePageUrl

            client.post()
                .uri(homePageUrl + "services/user/${ticketOrder.username}/tickets/add/")
                .bodyValue(ticketForTraveler)
                .awaitExchange {
                    if (it.statusCode() != HttpStatus.OK)
                        throw InvalidTicketRestrictionException("Post for ticket failed")
                }
        }catch(e: RuntimeException) {
            throw InvalidTicketRestrictionException("Traveler service not found")
        }catch(e: WebClientResponseException) {
            throw InvalidTicketRestrictionException("Traveler service not responding properly")
        }

        logger.info { "Ticket post successful" }
    }
}