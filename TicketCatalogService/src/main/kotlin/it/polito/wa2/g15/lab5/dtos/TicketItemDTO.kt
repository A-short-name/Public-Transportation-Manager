package it.polito.wa2.g15.lab5.dtos

import it.polito.wa2.g15.lab5.entities.TicketItem
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive


data class TicketItemDTO(
    @field:NotNull
    @field:Positive
    val ticketId: Long,

    @field:NotNull
    @field:Positive
    val price: Double,

    @field:NotBlank(message = "Type can't be empty or null")
    val type: String
)

fun TicketItem.toDTO() : TicketItemDTO {
    return TicketItemDTO(id!!, price, ticketType)
}

data class NewTicketItemDTO(
    @field:NotNull
    @field:Positive
    val price: Double,

    @field:NotBlank(message = "Type can't be empty or null")
    val type: String,
    @field:Positive
    val minAge: Int?,
    @field:Positive
    val maxAge: Int?
)