package it.polito.wa2.g15.lab5.dtos

import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

data class BuyTicketDTO (

    @field:NotNull
    @field:Positive
    val numOfTickets: Int,

    @field:NotNull
    @field:Positive
    val paymentInfo: PaymentInfo

    )



data class PaymentInfo(
    @field:NotBlank
    val creditCardNumber: String,

    val exp: LocalDate,

    @field:NotBlank
    val csv: String,

    )