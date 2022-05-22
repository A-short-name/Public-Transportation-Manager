package it.polito.wa2.g15.lab5

import it.polito.wa2.g15.lab5.dtos.*
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.kafka.OrderInformationMessage
import it.polito.wa2.g15.lab5.services.TicketCatalogService
import it.polito.wa2.g15.lab5.services.TicketOrderService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
class Controller {
    @Autowired
    private lateinit var ticketCatalogService: TicketCatalogService

    @Autowired
    private lateinit var ticketOrderService: TicketOrderService

    private val logger = KotlinLogging.logger {}

    private val principal = ReactiveSecurityContextHolder.getContext()
        .map { obj: SecurityContext -> obj.authentication.principal}
        .cast(UserDetailsDTO::class.java)


    @GetMapping(path = ["/whoami"])
    @PreAuthorize("hasAuthority('CUSTOMER')")
    fun getName(): Mono<String>? {
        return principal.map { p -> p.sub }
    }

    /**
     * Returns a JSON representation of all available tickets. Those tickets
     * are represented as a JSON object consisting of price, ticketId, type ( ordinal or type
     * of pass).
     */
    @GetMapping("tickets/", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    fun availableTickets() : Flow<TicketItemDTO> {
        return ticketCatalogService.getAllTicketItems().map { item -> item.toDTO() }
    }

    /**
     * It accepts a json containing the number of tickets, ticketId,
     * and payment information (credit card number, expiration date, cvv, card holder). Only
     * authenticated users can perform this request. In case those tickets have age
     * restrictions, it asks the TravellerService the user profile in order to check if the
     * operation is permitted. In case it is, it saves the order in the database with the
     * PENDING status, then it transmits the billing information and the total cost of the
     * order to the payment service through a kafka topic, and it returns the orderId. When
     * the Kafka listener receives the outcome of the transaction, the status of order is
     * updated according to what the payment service has stated and, if the operation was
     * successful, the purchased products are added to the list of acquired tickets in the
     * TravellerService.
     * The client to check the order result, must do polling to check the outcome.
     */
    @PostMapping("/shop/{ticket-id}/")
    @PreAuthorize("hasAuthority('CUSTOMER') OR hasAuthority('ADMIN')")
    suspend fun buyTickets(@PathVariable("ticket-id") ticketId: String,
                           @RequestBody buyTicketBody: Mono<BuyTicketDTO>
    ) : Long {

    //Body da passare: listOf.(TicketForTravelerDTO(validFrom= it.validFrom, ticketItemId= ticket-id, zid=it.zid, ticketType=it.type) * numberOfTickets)
    val userName = principal.map { p -> p.sub }
        return ticketCatalogService.buyTicket(buyTicketBody,ticketId.toLong(),userName)
    }

    /**
     * Get the orders of the user
     */
    @GetMapping("orders/", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('CUSTOMER') OR hasAuthority('ADMIN')")
    suspend fun orders() : Flow<TicketOrder> {
        val userName = principal.map { p -> p.sub }
        return ticketOrderService.getUserTicketOrders(userName)
    }

    /**
     * Get a specific order. This endpoint can be used by the client
     * to check the order status after a purchase.
     */
    @GetMapping("orders/{order-id}/", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('CUSTOMER') OR hasAuthority('ADMIN')")
    suspend fun getSpecificOrder(@PathVariable("order-id") orderId: String) : TicketOrder {
        return ticketOrderService.getTicketOrderById(orderId.toLong())
    }

    /**
     * Admin users can add to catalog new available tickets to purchase.
     */
    @PostMapping("admin/tickets/")
    @PreAuthorize("hasAuthority('ADMIN')")
    suspend fun addNewAvailableTicketToCatalog(
        @RequestBody newTicketItemDTO: Mono<NewTicketItemDTO>,
        response: ServerHttpResponse) {

        val ticket = newTicketItemDTO
            .doOnError { response.statusCode = HttpStatus.BAD_REQUEST }
            .awaitSingle()

        try {
            ticketCatalogService.addNewTicketType(ticket)
            response.statusCode = HttpStatus.OK
        }catch(e: Exception) {
            response.statusCode = HttpStatus.BAD_REQUEST
        }
    }

    /**
     * This endpoint retrieves a list of all orders made by all users
     */
    @GetMapping("admin/orders/", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('ADMIN')")
    suspend fun getAllOrder() :Flow<TicketOrder>{
        return ticketOrderService.getAllTicketOrders()
    }

    /**
     * Get orders of a specific user
     */
    @GetMapping("admin/orders/{user-id}/", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('ADMIN')")
    suspend fun getOrdersOfASpecificUser(@PathVariable("user-id") userId: Mono<String>) : Flow<TicketOrder> {
        return ticketOrderService.getUserTicketOrders(userId)
    }

    /*Endpoint to test kafka communication with the payment service*/


    @Value("\${kafka.topics.product}")
    lateinit var topic: String
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("test/kafka/produce/")
    fun testKafkaProduceMessage(@Validated @RequestBody product: OrderInformationMessage, response: ServerHttpResponse) {
        return try {
            log.info("Receiving product request")
            log.info("Sending message to Kafka {}", product)
            val message: Message<OrderInformationMessage> = MessageBuilder
                    .withPayload(product)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader("X-Custom-Header", "Custom header here")
                    .build()
            kafkaTemplate.send(message)
            log.info("Message sent with success")
            response.statusCode = HttpStatus.OK
        } catch (e: Exception) {
            log.error("Exception: {}",e)
            response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}