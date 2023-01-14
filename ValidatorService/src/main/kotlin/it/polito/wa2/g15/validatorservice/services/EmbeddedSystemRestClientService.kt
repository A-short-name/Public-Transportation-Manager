package it.polito.wa2.g15.validatorservice.services

import com.netflix.discovery.EurekaClient
import it.polito.wa2.g15.validatorservice.dtos.UserLoginRequestDTO
import it.polito.wa2.g15.validatorservice.dtos.UserLoginResponseDTO
import it.polito.wa2.g15.validatorservice.exceptions.ValidatorLoginException
import it.polito.wa2.g15.validatorservice.security.WebSecurityConfig
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
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

    @Autowired
    lateinit var discoveryClient : EurekaClient

    fun getValidationKey(): String {
        if(loginToken==""){
            logger.info("Contacting Login Service to perform authentication...")
            performLogin()
        }
        logger.info("Contacting Traveler Service to obtain validation key...")
        return askValidationKeyToTravelerService()
    }

    private fun askValidationKeyToTravelerService(): String {
        try {
            val instanceInfo = discoveryClient.getNextServerFromEureka("traveler", false)
            val baseUri = instanceInfo.homePageUrl
            val travelerServiceUri = baseUri + "validation/secret"

            val restTemplate = RestTemplate()
            val headerBySecConfig = securityConfig.generateCsrfHeader(csrfTokenRepository)
            headerBySecConfig.contentType = MediaType.APPLICATION_JSON
            headerBySecConfig.setBearerAuth(loginToken)
            val request2 = HttpEntity(
                "",
                headerBySecConfig
            )

            val response: ResponseEntity<String> = restTemplate.exchange(
                travelerServiceUri,
                HttpMethod.GET,
                request2
            )
            if (response.statusCode != HttpStatus.OK)
                throw ValidatorLoginException(
                    "can not retrieve validation key from traveler service" +
                            "\ntraveler service response: $response"
                )
            if (response.body == null)
                throw ValidatorLoginException(
                    "can not retrieve validation key from traveler service" +
                            "\nbecause the key received is null"
                )
            return response.body!!
        }catch(e: RuntimeException) {
            throw ValidatorLoginException("Traveler service not found")
        }catch(e: RestClientException) {
            throw ValidatorLoginException("Problems contacting traveler service")
        }
    }


    fun performLogin() {
        try {
            val instanceInfo = discoveryClient.getNextServerFromEureka("login", false)
            val baseUri = instanceInfo.homePageUrl
            val loginUri = baseUri + "user/login"

            val restTemplate = RestTemplate()
            val headerBySecConfig = securityConfig.generateCsrfHeader(csrfTokenRepository)
            headerBySecConfig.contentType = MediaType.APPLICATION_JSON

            val request2 = HttpEntity(
                UserLoginRequestDTO(nickname = validatorUserName, password = validatorPassword),
                headerBySecConfig
            )

            val response: ResponseEntity<UserLoginResponseDTO> = restTemplate.postForEntity(
                loginUri, request2
            )

            when (response.statusCode) {
                HttpStatus.OK -> loginToken = response.body!!.token
                HttpStatus.NOT_FOUND -> throw ValidatorLoginException("login server not found in uri: $loginUri")
                HttpStatus.TOO_MANY_REQUESTS -> throw ValidatorLoginException("login server too many login requests in uri: $loginUri")
                else -> throw ValidatorLoginException("response not ok to uri: $loginUri\n got response: $response")
            }

        }catch(e: RuntimeException) {
            throw ValidatorLoginException("Login service not found")
        }catch(e: RestClientException) {
            throw ValidatorLoginException("Problems contacting login service")
        }
    }
}