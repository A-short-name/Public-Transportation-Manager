package it.polito.wa2.g15.lab5.services


import it.polito.wa2.g15.lab5.entities.TicketOrder
import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.prepost.PreAuthorize
import java.time.ZonedDateTime

interface TicketOrderService {
    
    /**
     * get all orders of all users
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    suspend fun getAllTicketOrders(): Flow<TicketOrder>
    
    /**
     * get all orders of the user
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER')")
    fun getUserTicketOrders(username: String): Flow<TicketOrder>
    
    /**
     * save the order with pending status
     * @return orderId of the saved order
     */
    suspend fun savePendingOrder(
        totalPrice: Double,
        username: String,
        ticketId: Long,
        quantity: Int,
        validFrom: ZonedDateTime,
        zid: String
    ): TicketOrder
    
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER')")
    suspend fun getTicketOrderById(orderId: Long, username: String): TicketOrder?
    
}