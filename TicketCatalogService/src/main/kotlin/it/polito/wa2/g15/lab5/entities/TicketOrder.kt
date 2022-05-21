package it.polito.wa2.g15.lab5.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("ticket_orders")
data class TicketOrder (
    @Id
    val orderId: Long? = null,

    val orderState: String,

    val totalPrice: Double,
    val username: String,
    val ticketId: Long,
    val quantity: Int

    )
