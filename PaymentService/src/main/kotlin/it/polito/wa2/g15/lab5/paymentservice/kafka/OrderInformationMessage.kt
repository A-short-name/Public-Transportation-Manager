package it.polito.wa2.g15.lab5.paymentservice.kafka

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import javax.validation.constraints.NotBlank

data class OrderInformationMessage(
        @JsonProperty("billing_info")
        val billingInfo: PaymentInfo,
        @JsonProperty("total_cost")
        val totalCost: Double,
        val username: String,
        @JsonProperty("order_id")
        val orderId: Long
)

data class PaymentInfo(
        @field:NotBlank
        val creditCardNumber: String,

        val exp: LocalDate,

        @field:NotBlank
        val csv: String,

        @field:NotBlank
        val cardHolder: String

        )