package it.polito.wa2.g15.validatorservice.repositories

import it.polito.wa2.g15.validatorservice.entities.TicketValidation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface TicketValidationRepository : CrudRepository<TicketValidation, Long> {
    fun findTicketValidationsByUsername(username: String): List<TicketValidation>
    fun findTicketValidationsByValidationTimeIsBetween(
        timeStart: LocalDateTime,
        timeEnd: LocalDateTime
    ): List<TicketValidation>

    fun findTicketValidationsByUsernameAndValidationTimeIsBetween(
        username: String,
        timeStart: LocalDateTime,
        timeEnd: LocalDateTime
    ): List<TicketValidation>
}
