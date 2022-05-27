package it.polito.wa2.g15.lab5.paymentservice.kafka

import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.CreditCardNumber
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
        @field:CreditCardNumber(ignoreNonDigitCharacters = true)
        val creditCardNumber: String,

        @field:NotBlank
        val cardHolder: String
)