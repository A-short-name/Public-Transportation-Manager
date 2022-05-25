package it.polito.wa2.g15.lab5.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Table("ticket_orders")
data class TicketOrder (
        @Id
        val orderId: Long? = null,

        var orderState: String,

        val totalPrice: Double,
        val username: String,
        val ticketId: Long,
        val quantity: Int,
        @NotNull
        val validFrom: LocalDate,
        @NotBlank
        val zid: String
    )
