package it.polito.wa2.g15.lab5.dtos

import it.polito.wa2.g15.lab5.entities.TicketItem
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero


data class TicketItemDTO(
    @field:NotNull
    @field:Positive
    val ticketId: Long,

    @field:NotNull
    @field:Positive
    val price: Double,

    @field:NotBlank(message = "Type can't be empty or null")
    val type: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TicketItemDTO

        if (ticketId != other.ticketId) return false
        if (price != other.price) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ticketId.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

fun TicketItem.toDTO() : TicketItemDTO {
    return TicketItemDTO(id!!, price, ticketType)
}

data class NewTicketItemDTO(
    @field:NotNull
    @field:Positive
    val price: Double,

    @field:NotBlank(message = "Type can't be empty or null")
    val type: String,
    @field:PositiveOrZero
    val minAge: Int?,
    @field:PositiveOrZero
    val maxAge: Int?,
    @field:Positive
    val duration: Long = -1
    )
