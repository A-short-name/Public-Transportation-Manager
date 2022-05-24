package it.polito.wa2.g15.lab5.paymentservice.services

import it.polito.wa2.g15.lab5.paymentservice.dtos.TransactionDTO
import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import kotlinx.coroutines.flow.Flow

interface PaymentService {
    fun getTransactionsByUser(username: String) : Flow<TransactionDTO>
    fun getAllTransactions() : Flow<TransactionDTO>
}