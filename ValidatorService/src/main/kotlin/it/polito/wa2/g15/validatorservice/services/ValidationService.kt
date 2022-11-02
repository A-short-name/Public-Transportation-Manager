package it.polito.wa2.g15.validatorservice.services

import it.polito.wa2.g15.validatorservice.dtos.FilterDto
import it.polito.wa2.g15.validatorservice.dtos.TicketDTO
import it.polito.wa2.g15.validatorservice.entities.TicketValidation
import it.polito.wa2.g15.validatorservice.exceptions.InvalidZoneException
import it.polito.wa2.g15.validatorservice.exceptions.TimeTicketException
import it.polito.wa2.g15.validatorservice.repositories.TicketValidationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
class ValidationService {

    @Autowired
    lateinit var ticketValidationRepository: TicketValidationRepository

    @Value("{validator.zone}")
    lateinit var validZones: String

    /**
     * returns statistics of this validator
     *
     * @param filter the filters are applied only if the field of the filter object aren't null,
     * otherwise that specific filter will be ignored
     */
    fun getStats(filter: FilterDto): List<TicketValidation> {
        if (!filter.nickname.isNullOrBlank())
            return if (filter.timeEnd != null && filter.timeStart != null)
                ticketValidationRepository.findTicketValidationsByUsernameAndValidationTimeIsBetween(
                    username = filter.nickname,
                    timeStart = filter.timeStart,
                    timeEnd = filter.timeEnd
                )
            else
                ticketValidationRepository.findTicketValidationsByUsername(
                    filter.nickname
                )
        else
            return if (filter.timeEnd != null && filter.timeStart != null)
                ticketValidationRepository.findTicketValidationsByValidationTimeIsBetween(
                    timeEnd = filter.timeEnd, timeStart = filter.timeStart
                )
            else
                ticketValidationRepository.findAll().toList()
    }

    fun validate(nickname: String, ticket: TicketDTO) {
        isTicketValid(ticket)
        ticketValidationRepository.save(
            TicketValidation(
                username = nickname,
                validationTime = LocalDateTime.now(),
                ticketId = ticket.sub
            )
        )
    }

    private fun isTicketValid(ticket: TicketDTO) {
        TODO("Import lab2 validation function")
        if (!validZones.contains(ticket.zid, ignoreCase = true)) {
            throw InvalidZoneException("zone " + ticket.zid + " is not valid for this machine")
        }
        if (!ticket.validFrom.isBefore(ZonedDateTime.now())) {
            throw TimeTicketException("ticket is not yet valide, it will be valide from " + ticket.validFrom.toString())
        }

    }
}
