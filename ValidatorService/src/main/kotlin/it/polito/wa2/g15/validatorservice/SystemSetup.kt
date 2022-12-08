package it.polito.wa2.g15.validatorservice

import it.polito.wa2.g15.validatorservice.dtos.UserLoginRequestDTO
import it.polito.wa2.g15.validatorservice.dtos.UserLoginResponseDTO
import it.polito.wa2.g15.validatorservice.exceptions.ValidatorLoginException
import it.polito.wa2.g15.validatorservice.security.WebSecurityConfig
import it.polito.wa2.g15.validatorservice.services.ValidationService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.*
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Component
class SystemSetup {
    private val logger = KotlinLogging.logger {}

    var loginToken: String = ""


    @Value("\${embeddedsystem.credentials.username}")
    lateinit var validatorUserName: String

    @Value("\${embeddedsystem.credentials.password}")
    lateinit var validatorPassword: String

    @Autowired
    lateinit var validatorService: ValidationService

    @Autowired
    lateinit var securityConfig: WebSecurityConfig

    @Autowired
    lateinit var csrfTokenRepository: CsrfTokenRepository


    //TODO: manage the uri by spring cloud
    val loginUri: String = "http://localhost:8080/user/login"

    @EventListener(ApplicationReadyEvent::class)
    fun systemConfiguration() {
        logger.info("Contacting Login Service to perform authentication...")

        val bool = false;
        // TODO: Test the Call login service's API
        // Validatrice logga nel LoginService con l'api /user/login e riceve il ruolo di embedded system

        performLogin()

        // TODO: Contact Traveler Service's API (utilizza il token settato dalla perform login)
        // Contatta il TravelerService con il ruolo di embedded system e chiede il segreto per
        // validare i biglietti ad una nuova api /secret/get
        val key = "ueCFt3yXHg+6vkRYd4k0aA5q0FV4aPhEMok/2s+JJZI=" //stringa ricevuta dal travelerService
        validatorService.setKey(key)

        if (bool)
            logger.info("Successfully authenticated as Embedded user and secret received")
        else
            logger.error("Failed to perform authentication. Not implemented!")
    }

    fun performLogin() {
        var loginCounter = 0;
        var response: ResponseEntity<UserLoginResponseDTO>
        do {
            val restTemplate = RestTemplate()
            val headerBySecConfig = securityConfig.generateCsrfHeader(csrfTokenRepository)
            headerBySecConfig.contentType = MediaType.APPLICATION_JSON

            val request2 = HttpEntity(
                UserLoginRequestDTO(nickname = validatorUserName, password = validatorPassword),
                headerBySecConfig
            )

            response = restTemplate.postForEntity(
                loginUri, request2
            )
            loginCounter++
            Thread.sleep(1_000L * loginCounter)
        } while (response.statusCode == HttpStatus.NOT_FOUND && loginCounter < 20)


        when (response.statusCode) {
            HttpStatus.OK -> loginToken = response.body!!.token
            HttpStatus.NOT_FOUND -> throw ValidatorLoginException("login server not found in uri: $loginUri")
            HttpStatus.TOO_MANY_REQUESTS -> throw ValidatorLoginException("login server too many login requests in uri: $loginUri")
            else -> throw ValidatorLoginException("response not ok to uri: $loginUri\n got response: $response")
        }
    }

}