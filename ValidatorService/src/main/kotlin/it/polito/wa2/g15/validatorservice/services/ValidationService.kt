package it.polito.wa2.g15.validatorservice.services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.WeakKeyException
import it.polito.wa2.g15.validatorservice.entities.TicketFields
import it.polito.wa2.g15.validatorservice.entities.TicketValidation
import it.polito.wa2.g15.validatorservice.exceptions.ValidationException
import it.polito.wa2.g15.validatorservice.repositories.TicketValidationRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import java.time.*
import javax.crypto.SecretKey

@Service
class ValidationService {

    @Autowired
    lateinit var ticketValidationRepository: TicketValidationRepository

    @Autowired
    lateinit var embeddedSystemRestClientService: EmbeddedSystemRestClientService

    private val logger = KotlinLogging.logger {}

    lateinit var key: SecretKey
    var isValidationKeyValid = false


    fun systemConfig() {
        var key = ""
        try {
            key = embeddedSystemRestClientService.getValidationKey()
            logger.info("secret received: $key")
        } catch (e: RestClientException) {
            logger.info("Validation is inactive because it can not retrieve the key used to validate tickets")
            logger.error("can not connect to other services: ${e.message}")
        } finally {
            setKey(key)
        }
    }

    fun setKey(validateJwtStringKey: String) {
        val decodedKey = Decoders.BASE64.decode(validateJwtStringKey)
        try {
            key = Keys.hmacShaKeyFor(decodedKey)
            isValidationKeyValid = true
        } catch (e: WeakKeyException) {
            logger.error("Validation Key error: ${e.message}")
            isValidationKeyValid = false
        }
    }

    /**
     * returns statistics of this validator
     *
     * @param timeStart start of the time interval
     * @param timeEnd end of the time interval
     * @param nickname nickname of the traveler
     * otherwise that specific filter will be ignored
     */
    @PreAuthorize("hasAnyAuthority('SUPERADMIN', 'ADMIN')")
    fun getStats(timeStart: LocalDateTime?, timeEnd: LocalDateTime?, nickname: String?): List<TicketValidation> {
        if (!nickname.isNullOrBlank())
            return if (timeEnd != null && timeStart != null)
                ticketValidationRepository.findTicketValidationsByUsernameAndValidationTimeIsBetween(
                    username = nickname,
                    timeStart = timeStart,
                    timeEnd = timeEnd
                )
            else
                ticketValidationRepository.findTicketValidationsByUsername(
                    nickname
                )
        else
            return if (timeEnd != null && timeStart != null)
                ticketValidationRepository.findTicketValidationsByValidationTimeIsBetween(
                    timeEnd = timeEnd, timeStart = timeStart
                )
            else
                ticketValidationRepository.findAll().toList()
    }

    fun validateTicket(signedJwt: String, clientZone: String) {
        if (!isValidationKeyValid) {
            systemConfig()
            if (!isValidationKeyValid)
                throw ValidationException("this service is not ready for validation because validation key is not valid")
        }
        lateinit var jwt: Jws<Claims>
        try {
            jwt = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(signedJwt)
        } catch (e: Exception) {
            // Expiration date is checked automatically by the JWT library:
            // https://github.com/jwtk/jjwt#standard-claims
            // https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.4
            throw ValidationException("Invalid jwt\n\t ${e.message}")
        }

        val ticketValidFromTs = (jwt.body[TicketFields.VALID_FROM] as Int).toLong()
        //jwt.body.get(TicketFields.VALID_FROM, Long.javaClass)
//jwt restituisce un intero
//            jwt.body[TicketFields.VALID_FROM] as? Long ?: throw ValidationException("no valid from is found")

        val ticketType = jwt.body[TicketFields.TYPE] as? String ?: throw ValidationException("no ticket type is found")

        //zone time is taken from the system, if you want to manage the different zone times it should managed here
        // and in the travelerService module (where the ticket is generated)
        val ticketValidFrom = ZonedDateTime.ofInstant(Instant.ofEpochSecond(ticketValidFromTs), ZoneId.systemDefault())
        // Comment the next 3 lines to obtain the server version used for the noSubject benchmarks

        val ticketBuyerUsername =
            jwt.body[TicketFields.USERNAME] as? String ?: throw ValidationException("no ticket username is found")

        val ticketValidZone = jwt.body[TicketFields.VALID_ZONES] as? String
            ?: throw ValidationException("validity zone not found")

        if (clientZone.isEmpty() || !ticketValidZone.contains(clientZone.toRegex()))
            throw ValidationException("client zone $clientZone not present in valid zones of the ticket")

//        val ticketId = jwt.body[TicketFields.SUBJECT] as? Int ?: throw ValidationException("no ticket id is found")

        val ticketId = jwt.body.subject.toIntOrNull() ?: throw ValidationException("not valid subject")

        if (ticketValidFrom.isAfter(ZonedDateTime.now()))
            throw ValidationException("ticket is not yet valid, it will be valid from " + ticketValidFrom)

        checkTicketType(ticketType, ticketId)

        //TODO: check if a ticket was used few minutes ago (in order to avoid validation of multiple travelers with the same pass)

        ticketValidationRepository.save(
            TicketValidation(
                username = ticketBuyerUsername,
                validationTime = LocalDateTime.now(),
                ticketId = ticketId
            )
        )

        logger.info("\t ticket ${jwt.body[TicketFields.SUBJECT]} is valid")
    }

    /**
     *  Managing Travel cards:
     * (at the moment we only have weekend-pass and ordinal)
     * here we can manage:
     *  - PASS: the ticket can be used more than one time
     *  - WEEKEND: the ticket can be used only during weekend
     *  - otherwise: ticket can be used only 1 time whenever the traveler wants
     */
    private fun checkTicketType(ticketType: String, ticketId: Int) {
        val isWeekend =
            DayOfWeek.SUNDAY == ZonedDateTime.now().dayOfWeek || DayOfWeek.SATURDAY == ZonedDateTime.now().dayOfWeek
        if (ticketType.contains("WEEKEND") && !isWeekend)
            throw ValidationException("ticket of type $ticketType is valid only during the weekend")

        if (!ticketType.contains("PASS")) {
            val isTicketUsed = ticketValidationRepository.existsByTicketId(ticketId)
            if (isTicketUsed)
                throw ValidationException("ticket is already used, ticket of type $ticketType can not be used multiple times")
        }
    }
}
