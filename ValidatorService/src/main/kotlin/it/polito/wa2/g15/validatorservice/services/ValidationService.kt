package it.polito.wa2.g15.validatorservice.services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.validatorservice.dtos.FilterDto
import it.polito.wa2.g15.validatorservice.dtos.TicketDTO
import it.polito.wa2.g15.validatorservice.entities.TicketFields
import it.polito.wa2.g15.validatorservice.entities.TicketValidation
import it.polito.wa2.g15.validatorservice.exceptions.InvalidZoneException
import it.polito.wa2.g15.validatorservice.exceptions.TimeTicketException
import it.polito.wa2.g15.validatorservice.exceptions.ValidationException
import it.polito.wa2.g15.validatorservice.repositories.TicketValidationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.ZonedDateTime
import javax.crypto.SecretKey

@Service
class ValidationService {

    @Autowired
    lateinit var ticketValidationRepository: TicketValidationRepository

    @Value("{validator.zone}")
    lateinit var validZones: String

    @Value("\${security.path.privateKey}")
    lateinit var keyPath: String

    @Value("\${security.path.privateKey}")
    lateinit var clientZone: String

    val key: SecretKey by lazy {
        val secretString = File(keyPath).bufferedReader().use { it.readLine() }
        val decodedKey = Decoders.BASE64.decode(secretString)
        Keys.hmacShaKeyFor(decodedKey)
    }

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
        //TODO: lancia eccezione se il biglietto già è stato validato
        //if ticket is not valid an exception is thrown and the save shouldn't be performed
        ticketValidationRepository.save(
            TicketValidation(
                username = nickname,
                validationTime = LocalDateTime.now(),
                ticketId = ticket.sub
            )
        )
    }

    fun validateTicket(signedJwt: String) {

        lateinit var jwt: Jws<Claims>
        try {
            jwt = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(signedJwt)
        } catch (e: Exception) {
            // Expiration date is checked automatically by the JWT library:
            // https://github.com/jwtk/jjwt#standard-claims
            // https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.4
            throw ValidationException("Invalid jwt\n\t ${e.message}")
        }

        jwt.body[TicketFields.EXPIRATION] as? Int ?: throw ValidationException("no expiration is found")

        val validityZone = jwt.body[TicketFields.VALID_ZONES] as? String
            ?: throw ValidationException("validity zone not found")

        if (clientZone.isEmpty() || !validityZone.contains(clientZone.toRegex()))
            throw ValidationException("client zone $clientZone not present in valid zones of the ticket")

        // Comment the next 3 lines to obtain the server version used for the noSubject benchmarks
        val ticketId = jwt.body[TicketFields.SUBJECT] as? Int ?: throw ValidationException("no ticket id is found")
        //TODO: save and update the used tikcets and check it based on ticket type
//        if(!repository.addTicket(ticketId))
//            throw ValidationException("ticket $ticketId already used")

        println("\t ticket ${jwt.body[TicketFields.SUBJECT]} is valid")
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
