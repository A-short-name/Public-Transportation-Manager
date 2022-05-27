package it.polito.wa2.g15.lab5.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime
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
        val validFrom: ZonedDateTime,
        @NotBlank
        val zid: String
    ) {
        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as TicketOrder

                if (orderId != other.orderId) return false
                if (orderState != other.orderState) return false
                if (totalPrice != other.totalPrice) return false
                if (username != other.username) return false
                if (ticketId != other.ticketId) return false
                if (quantity != other.quantity) return false
                if (validFrom.toLocalDateTime().withNano(0) != other.validFrom.toLocalDateTime().withNano(0)) return false
                if (zid != other.zid) return false

                return true
        }

        override fun hashCode(): Int {
                var result = orderId?.hashCode() ?: 0
                result = 31 * result + orderState.hashCode()
                result = 31 * result + totalPrice.hashCode()
                result = 31 * result + username.hashCode()
                result = 31 * result + ticketId.hashCode()
                result = 31 * result + quantity
                result = 31 * result + validFrom.toLocalDateTime().withNano(0).hashCode()
                result = 31 * result + zid.hashCode()
                return result
        }
}
