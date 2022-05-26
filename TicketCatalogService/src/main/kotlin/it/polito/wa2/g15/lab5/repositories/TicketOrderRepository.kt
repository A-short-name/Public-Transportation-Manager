package it.polito.wa2.g15.lab5.repositories

import it.polito.wa2.g15.lab5.entities.TicketOrder
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TicketOrderRepository : CoroutineCrudRepository<TicketOrder, Long> {

    fun findTicketOrdersByUsername(username: String) : Flow<TicketOrder>

    fun findTicketOrderByOrderIdAndUsername(orderId: Long, username: String) : Flow<TicketOrder>
}