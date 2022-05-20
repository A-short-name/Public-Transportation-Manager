package it.polito.wa2.g15.lab5.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("ticket_items")
data class TicketItem (
    @Id
    val id: Long? = null,
    val type: String,
    val price: Double,
    )
