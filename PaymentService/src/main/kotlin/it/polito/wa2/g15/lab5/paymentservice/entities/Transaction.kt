package it.polito.wa2.g15.lab5.paymentservice.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("transactions")
data class Transaction (
    @Id
    val id: Long? = null
)