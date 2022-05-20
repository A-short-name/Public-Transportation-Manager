package it.polito.wa2.g15.lab5

import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.dtos.TicketItemDTO
import it.polito.wa2.g15.lab5.dtos.UserDetailsDTO
import it.polito.wa2.g15.lab5.dtos.toDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import it.polito.wa2.g15.lab5.services.TicketCatalogService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorReturn
import javax.validation.Valid

@RestController
class Controller {
    @Autowired
    private lateinit var ticketCatalogService: TicketCatalogService

    @Autowired
    private lateinit var ticketRepo: TicketItemRepository

    private val logger = KotlinLogging.logger {}

    private val principal = ReactiveSecurityContextHolder.getContext()
        .map { obj: SecurityContext -> obj.authentication.principal}
        .cast(UserDetailsDTO::class.java)

    @GetMapping(path = ["/whoami"])
    @PreAuthorize("hasAuthority('CUSTOMER')")
    fun getName(): Mono<String>? {
        return principal.map { p -> p.sub }
    }

    @GetMapping("test/")
    suspend fun testing() : Flow<TicketItemDTO> {
        return ticketCatalogService.getAllTicketTypes()
            .map { item -> item.toDTO() }
    }

    /**
     * Returns a JSON representation of all available tickets. Those tickets
     * are represented as a JSON object consisting of price, ticketId, type ( ordinal or type
     * of pass). A ticketAcquired is represented as a JSON object consisting of the fields
     * “sub” (the unique ticketID), “iat” (issuedAt, a timestamp), “validfrom” (useful for
     * subscriptions), “exp” (expiry timestamp), “zid” (zoneID, the set of transport zones it
     * gives access to), type (ordinal, weekend pass, etc), “jws” (the encoding of the
     * previous information as a signed JWT) [Note that this JWT will be used for providing
     * physical access to the train area and will be signed by a key that has nothing to do
     * with the key used by the LoginService]
     */
    @GetMapping("tickets/")
    fun availableTickets() {
        TODO("Implement this")
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
    fun buyTickets(@PathVariable("ticket-id") ticketId: String) {
        TODO("Implement this")
    }

    /**
     * Get list of all user orders
     */
    @GetMapping("orders/")
    @PreAuthorize("hasAuthority('CUSTOMER') OR hasAuthority('ADMIN')")
    fun orders() {
        TODO("Implement this")
    }

    /**
     * Get a specific order. This endpoint can be used by the client
     * to check the order status after a purchase.
     */
    @GetMapping("orders/{order-id}/")
    @PreAuthorize("hasAuthority('CUSTOMER') OR hasAuthority('ADMIN')")
    fun getSpecificOrder(@PathVariable("order-id") orderId: String) {
        TODO("Implement this")
    }

    /**
     * Admin users can add to catalog new available tickets to purchase.
     */
    @PostMapping("admin/tickets/")
    @PreAuthorize("hasAuthority('ADMIN')")
    suspend fun addNewAvailableTicketToCatalog(
        @RequestBody newTicketItemDTO: Mono<NewTicketItemDTO>,
        response: ServerHttpResponse) {


        ticketCatalogService.addNewTicketType(newTicketItemDTO.awaitSingle())
//            newTicketItemDTO.map { newTicket ->
//                ticketCatalogService.addNewTicketType(newTicket)
//            }.subscribe()
//            try {
//                newTicketItemDTO.block()!!)
//            } catch (ex: Exception) {
//                logger.error { "\tTicket type not valid: ${ex.message}" }
//                response.statusCode = HttpStatus.BAD_REQUEST
//                return

        response.statusCode = HttpStatus.OK
        return
    }

    /**
     * This endpoint retrieves a list of all orders made by all users
     */
    @GetMapping("admin/orders/")
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getAllOrder() {
        TODO("Implement this")
    }

    /**
     * Get orders of a specific user
     */
    @GetMapping("admin/orders/{user-id}/")
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getOrdersOfASpecificUser(@PathVariable("user-id") userId: String) {
        TODO("Implement this")
    }

    fun logBindingResultErrors(bindingResult: BindingResult) {
        val errors: MutableMap<String, String?> = HashMap()
        bindingResult.allErrors.forEach { error: ObjectError ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.getDefaultMessage()
            errors[fieldName] = errorMessage
        }
        logger.debug { errors }
    }
}