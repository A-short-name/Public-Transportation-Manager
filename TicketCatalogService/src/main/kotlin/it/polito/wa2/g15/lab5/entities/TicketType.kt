package it.polito.wa2.g15.lab5.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("ticket_types")
data class TicketType (
    @Id
    val id: Long,
    val type: String,
    val price: Double,
    )
