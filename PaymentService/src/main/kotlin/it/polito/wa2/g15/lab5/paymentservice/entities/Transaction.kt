package it.polito.wa2.g15.lab5.paymentservice.entities

import org.hibernate.validator.constraints.CreditCardNumber
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

@Table("transactions")
data class Transaction (
    @Id
    val id: Long? = null,
    @NotBlank
    val username: String,
    @Positive
    val totalCost: Double,

    @NotBlank
    @CreditCardNumber(ignoreNonDigitCharacters = true)
    val creditCardNumber: String,
    @NotBlank
    val cardHolder: String,

    @Positive
    val orderId: Long
)