package it.polito.wa2.g15.lab5.paymentservice.services

import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import it.polito.wa2.g15.lab5.paymentservice.repositories.TransactionRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PaymentServiceImpl : PaymentService {
    @Autowired
    lateinit var transactionRepository: TransactionRepository

    override suspend fun getTransactionsByUser(username: String) : Flow<Transaction> {
        return transactionRepository.getTransactionsByUsername(username)
    }

    override suspend fun getAllTransactions() : Flow<Transaction> {
        return transactionRepository.findAll()
    }

}