package it.polito.wa2.g15.lab5.paymentservice

import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller {

    /**
     * Get transactions of the current user
     */
    @GetMapping("transactions/", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAnyAuthority('CUSTOMER','ADMIN')")
    suspend fun getTransactions() {
        TODO("Implement this")
    }

    /**
     *  Get transactions of all users
     */
    @GetMapping("admin/transactions/", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    @PreAuthorize("hasAuthority('ADMIN')")
    suspend fun getAllTransactions() {
        TODO("Implement this")
    }
}