package it.polito.wa2.g15.lab5.paymentservice.services

import it.polito.wa2.g15.lab5.paymentservice.dtos.TransactionDTO
import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.prepost.PreAuthorize

interface PaymentService {
    @PreAuthorize("hasAnyAuthority('CUSTOMER','ADMIN','SUPERADMIN')")
    fun getTransactionsByUser(username: String): Flow<TransactionDTO>
    
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    fun getAllTransactions(): Flow<TransactionDTO>
}