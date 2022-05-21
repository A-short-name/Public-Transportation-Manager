package it.polito.wa2.g15.lab5.paymentservice.services

import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import kotlinx.coroutines.flow.Flow

interface PaymentService {
    suspend fun getTransactionsByUser(username: String) : Flow<Transaction>
    suspend fun getAllTransactions() : Flow<Transaction>
}