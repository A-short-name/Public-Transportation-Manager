package it.polito.wa2.g15.lab5.services


import it.polito.wa2.g15.lab5.entities.TicketOrder
import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.prepost.PreAuthorize
import reactor.core.publisher.Mono

interface TicketOrderService {

    /**
     * get all orders of all users
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    suspend fun getAllTicketOrders() : Flow<TicketOrder>

    /**
     * get all orders of the user
     */
    suspend fun getUserTicketOrders(username: Mono<String>) : Flow<TicketOrder>

    /**
     * save the order with pending status
     * @return orderId of the saved order
     */
    suspend fun savePendingOrder(totalPrice: Double, username: String,ticketId :Long, quantity: Int) : TicketOrder
    suspend fun getTicketOrderById(orderId: Long): TicketOrder


}