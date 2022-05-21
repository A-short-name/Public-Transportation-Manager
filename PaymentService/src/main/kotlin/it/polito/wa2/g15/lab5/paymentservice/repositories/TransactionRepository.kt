package it.polito.wa2.g15.lab5.paymentservice.repositories

import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : CoroutineCrudRepository<Transaction,Long> {
    fun getTransactionsByUsername(username: String) : Flow<Transaction>
}