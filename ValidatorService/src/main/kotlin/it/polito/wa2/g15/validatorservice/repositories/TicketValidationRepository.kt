package it.polito.wa2.g15.validatorservice.repositories

import it.polito.wa2.g15.validatorservice.entities.TicketValidation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
interface TicketValidationRepository : CrudRepository<TicketValidation, Long> {
    fun findTicketValidationsByUsername(username: String): List<TicketValidation>
    fun findTicketValidationsByValidationTimeBetweenTimeStartAndTimeEnd(
        timeStart: ZonedDateTime,
        timeEnd: ZonedDateTime
    ): List<TicketValidation>

    fun findTicketValidationsByUsernameAndValidationTimeBetweenTimeStartAndTimeEnd(
        username: String,
        timeStart: ZonedDateTime,
        timeEnd: ZonedDateTime
    ): List<TicketValidation>
}
