package it.polito.wa2.g15.validatorservice.services

import it.polito.wa2.g15.validatorservice.dtos.FilterDto
import it.polito.wa2.g15.validatorservice.dtos.StatisticDto
import it.polito.wa2.g15.validatorservice.dtos.TicketDTO
import it.polito.wa2.g15.validatorservice.exceptions.InvalidZoneException
import it.polito.wa2.g15.validatorservice.exceptions.TimeTicketException
import it.polito.wa2.g15.validatorservice.repositories.TicketValidationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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
    fun getStats(filter: FilterDto): StatisticDto {
        TODO("Not yet implemented")
    }

    fun validate(userID: Long, ticket: TicketDTO) {
        isTicketValid(ticket)

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
