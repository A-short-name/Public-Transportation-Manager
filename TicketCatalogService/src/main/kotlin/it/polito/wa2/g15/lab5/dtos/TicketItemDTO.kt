package it.polito.wa2.g15.lab5.dtos

import it.polito.wa2.g15.lab5.entities.TicketItem
import java.time.Duration
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
    val maxAge: Int?,
    @field:Positive
    val duration: Long = -1
    )

data class TicketInfoDTO(
    val type: String,
    val duration: Long
)
fun TicketItem.toTicketInfoDTO() : TicketInfoDTO{
    return TicketInfoDTO(ticketType, duration)
}