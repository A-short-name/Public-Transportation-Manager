package it.polito.wa2.g15.lab5.paymentservice

import it.polito.wa2.g15.lab5.paymentservice.dtos.TransactionDTO
import it.polito.wa2.g15.lab5.paymentservice.dtos.UserDetailsDTO
import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import it.polito.wa2.g15.lab5.paymentservice.services.PaymentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller {
    @Autowired
    private lateinit var paymentService: PaymentService

    private val principal = ReactiveSecurityContextHolder.getContext()
        .map { obj: SecurityContext -> obj.authentication.principal}
        .cast(UserDetailsDTO::class.java)
    /**
     * Get transactions of the current user
     */
    @GetMapping("/transactions/", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAnyAuthority('CUSTOMER','ADMIN')")
    suspend fun getTransactions() : Flow<TransactionDTO> {
        val sub = principal.awaitSingle().sub
        return paymentService.getTransactionsByUser(sub)
    }

    /**
     *  Get transactions of all users
     */
    @GetMapping("/admin/transactions/", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getAllTransactions() : Flow<TransactionDTO> {
        return paymentService.getAllTransactions()
    }
}