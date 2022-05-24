package it.polito.wa2.g15.lab5.paymentservice.dtos

import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import org.springframework.data.annotation.Id
import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive


data class TransactionDTO(
        val id: Long,
        val username: String,
        val totalCost: Double,
        val orderId: Long
)

fun Transaction.toDTO(): TransactionDTO{
    return TransactionDTO(id= this.id!!, username= this.username, totalCost=this.totalCost, orderId=this.orderId)
}


