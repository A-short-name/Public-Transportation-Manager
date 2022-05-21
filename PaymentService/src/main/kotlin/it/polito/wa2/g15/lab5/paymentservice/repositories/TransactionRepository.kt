package it.polito.wa2.g15.lab5.paymentservice.repositories

import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : CoroutineCrudRepository<Transaction,Long> {
}