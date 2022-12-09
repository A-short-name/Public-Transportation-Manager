package it.polito.wa2.g15.validatorservice.services

import it.polito.wa2.g15.validatorservice.dtos.UserLoginRequestDTO
import it.polito.wa2.g15.validatorservice.dtos.UserLoginResponseDTO
import it.polito.wa2.g15.validatorservice.exceptions.ValidatorLoginException
import it.polito.wa2.g15.validatorservice.security.WebSecurityConfig
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Service
class EmbeddedSystemRestClientService {
    private val logger = KotlinLogging.logger {}

    var loginToken: String = ""


    @Value("\${embeddedsystem.credentials.username}")
    lateinit var validatorUserName: String

    @Value("\${embeddedsystem.credentials.password}")
    lateinit var validatorPassword: String


    @Autowired
    lateinit var securityConfig: WebSecurityConfig

    @Autowired
    lateinit var csrfTokenRepository: CsrfTokenRepository

    //TODO: manage the uri by spring cloud
    val loginUri: String = "http://localhost:8080/user/login"

    fun getValidationKey(): String {
        logger.info("Contacting Login Service to perform authentication...")
        performLogin()
        return askValidationKeyToTravelerService()
    }

    private fun askValidationKeyToTravelerService(): String {
        // TODO: Contact Traveler Service's API (utilizza il token settato dalla perform login)
        // Contatta il TravelerService con il ruolo di embedded system e chiede il segreto per
        // validare i biglietti ad una nuova api /secret/get
        TODO("Not yet implemented")
    }


    fun performLogin() {
        var response: ResponseEntity<UserLoginResponseDTO>

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

        when (response.statusCode) {
            HttpStatus.OK -> loginToken = response.body!!.token
            HttpStatus.NOT_FOUND -> throw ValidatorLoginException("login server not found in uri: $loginUri")
            HttpStatus.TOO_MANY_REQUESTS -> throw ValidatorLoginException("login server too many login requests in uri: $loginUri")
            else -> throw ValidatorLoginException("response not ok to uri: $loginUri\n got response: $response")
        }
    }
}