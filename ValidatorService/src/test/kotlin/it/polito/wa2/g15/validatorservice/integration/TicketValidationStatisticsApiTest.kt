package it.polito.wa2.g15.validatorservice.integration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.validatorservice.MyPostgresSQLContainer
import it.polito.wa2.g15.validatorservice.dtos.StatisticDto
import it.polito.wa2.g15.validatorservice.entities.TicketValidation
import it.polito.wa2.g15.validatorservice.repositories.TicketValidationRepository
import it.polito.wa2.g15.validatorservice.security.WebSecurityConfig
import it.polito.wa2.g15.validatorservice.services.EmbeddedSystemRestClientService
import it.polito.wa2.g15.validatorservice.services.ValidationService
import org.junit.jupiter.api.*
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.util.UriComponentsBuilder
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.Month
import java.util.*
import javax.crypto.SecretKey


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketValidationStatisticsApiTest {

    companion object {
        @Container
        val postgres = MyPostgresSQLContainer("postgres:latest")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        }
    }


    @Autowired
    lateinit var repo: TicketValidationRepository

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var csrfTokenRepository: CsrfTokenRepository

    @Autowired
    lateinit var securityConfig: WebSecurityConfig

    @Mock
    lateinit var mockedRestClientSvc: EmbeddedSystemRestClientService

    @Autowired
    lateinit var validationService: ValidationService


    @BeforeEach
    fun initDb() {

        Mockito.`when`(mockedRestClientSvc.getValidationKey())
            .thenReturn("ueCFt3yXHg+6vkRYd4k0aA5q0FV4aPhEMok/2s+JJZI=")
        validationService.embeddedSystemRestClientService = mockedRestClientSvc
        val time = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            1,
            0,
            0
        )
        repo.save(
            TicketValidation(
                username = "R2D2",
                validationTime = time,
                ticketId = 1
            )
        )

        val time2 = LocalDateTime.of(
            2020,
            Month.NOVEMBER.value,
            20,
            10,
            0
        )
        repo.save(
            TicketValidation(
                username = "R2D2",
                validationTime = time2,
                ticketId = 2
            )
        )

        val time3 = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            30,
            20,
            45
        )
        repo.save(
            TicketValidation(
                username = "C3PO",
                validationTime = time3,
                ticketId = 3
            )
        )
    }

    @AfterEach
    fun tearDownDb() {
        repo.deleteAll()
    }

    @Test
    fun `try to access admin api without being logged in`() {
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)

        val createRequest = HttpEntity(
            null,
            requestHeader
        )
        val createResponse : ResponseEntity<Unit> = restTemplate.exchange(
            "http://localhost:$port/get/stats",
            HttpMethod.GET,
            createRequest
        )

        Assertions.assertEquals(HttpStatus.FORBIDDEN, createResponse.statusCode, "Wrong answer from server.")
    }

    @Test
    fun `trying to access admin API as customer`() {
        val token = generateJwtToken("NormalUser",setOf("CUSTOMER"))

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add("Authorization", "Bearer $token")

        val createRequest = HttpEntity(
            null,
            requestHeader
        )
        val createResponse : ResponseEntity<Unit> = restTemplate.exchange(
            "http://localhost:$port/get/stats",
            HttpMethod.GET,
            createRequest
        )

        Assertions.assertEquals(HttpStatus.FORBIDDEN, createResponse.statusCode, "Wrong answer from server.")
    }

    @Test
    fun `successfully get statistics as admin`() {
        val url = "http://localhost:$port/get/stats"
        val token = generateJwtToken("Boss",setOf("SUPERADMIN"))
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)

        requestHeader.add("Authorization", "Bearer $token")

        val startTime = LocalDateTime.of(
            2020,
            Month.NOVEMBER.value,
            30,
            23,
            59
        )
        val endTime = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            31,
            23,
            59
        )

        val createRequest = HttpEntity(
            null,
            requestHeader
        )

        val urlTemplate: String = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("timeStart", "{timeStart}")
            .queryParam("timeEnd", "{timeEnd}")
            .queryParam("nickname", "{nickname}")
            .encode()
            .toUriString()

        val params = mapOf(
            "timeStart" to startTime,
            "timeEnd" to endTime,
            "nickname" to "R2D2"
        )

        val response = restTemplate.exchange(
            urlTemplate,
            HttpMethod.GET,
            createRequest,
            StatisticDto::class.java,
            params
        )

        Assertions.assertEquals(HttpStatus.ACCEPTED, response.statusCode, "Wrong answer from server.")
    }

    @Test
    fun `should find all validated tickets`() {
        val url = "http://localhost:$port/get/stats"
        val token = generateJwtToken("Boss",setOf("SUPERADMIN"))
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)

        requestHeader.add("Authorization", "Bearer $token")

        val res = repo.findAll()

        val createRequest = HttpEntity(
            null,
            requestHeader
        )

        val urlTemplate: String = UriComponentsBuilder.fromHttpUrl(url)
            .encode()
            .toUriString()

        val response = restTemplate.exchange(
            urlTemplate,
            HttpMethod.GET,
            createRequest,
            StatisticDto::class.java,
        )

        Assertions.assertEquals(HttpStatus.ACCEPTED, response.statusCode, "Wrong answer from server.")

        val actualRes = response.body!!

        Assertions.assertEquals(
            res.toList().size,
            actualRes.validations.size,
            "it should return all the validations in the db without filters"
        )


    }

    @Test
    fun `should find validated tickets of a specific user`() {
        val url = "http://localhost:$port/get/stats"
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        val token = generateJwtToken("Boss",setOf("SUPERADMIN"))

        requestHeader.add("Authorization", "Bearer $token")

        val createRequest = HttpEntity(
            null,
            requestHeader
        )

        val urlTemplate: String = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("nickname", "{nickname}")
            .encode()
            .toUriString()

        val params = mapOf(
            "nickname" to "R2D2"
        )

        val response = restTemplate.exchange(
            urlTemplate,
            HttpMethod.GET,
            createRequest,
            StatisticDto::class.java,
            params
        )

        val actualRes = response.body!!.validations
        Assertions.assertEquals(2, actualRes.size, "it should find ticket validations of R2D2")
        Assertions.assertTrue(
            actualRes.stream().map { it.username }.allMatch { it.equals("R2D2") },
            "some validations are from another user: $actualRes"
        )
    }

    @Test
    fun `should find validated tickets in december 2020`() {
        val url = "http://localhost:$port/get/stats"
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        val token = generateJwtToken("Boss",setOf("SUPERADMIN"))

        requestHeader.add("Authorization", "Bearer $token")

        val startTime = LocalDateTime.of(
            2020,
            Month.NOVEMBER.value,
            30,
            23,
            59
        )
        val endTime = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            31,
            23,
            59
        )

        val createRequest = HttpEntity(
            null,
            requestHeader
        )

        val urlTemplate: String = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("timeStart", "{timeStart}")
            .queryParam("timeEnd", "{timeEnd}")
            .encode()
            .toUriString()

        val params = mapOf(
            "timeStart" to startTime,
            "timeEnd" to endTime,
        )

        val response = restTemplate.exchange(
            urlTemplate,
            HttpMethod.GET,
            createRequest,
            StatisticDto::class.java,
            params
        )

        val actualRes = response.body!!.validations
        Assertions.assertEquals(2, actualRes.size, "it should find validations in december")
        Assertions.assertTrue(
            actualRes.stream().map { it.validationTime }
                .allMatch { it.isBefore(endTime) && it.isAfter(startTime) },
            "some dates are out of range: $actualRes"
        )


    }

    @Test
    fun `should find validated tickets in december 2020 of R2D2`() {
        val url = "http://localhost:$port/get/stats"
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        val token = generateJwtToken("Boss",setOf("SUPERADMIN"))

        requestHeader.add("Authorization", "Bearer $token")

        val startTime = LocalDateTime.of(
            2020,
            Month.NOVEMBER.value,
            30,
            23,
            59
        )
        val endTime = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            31,
            23,
            59
        )

        val request = HttpEntity(
            null,
            requestHeader
        )

        val urlTemplate: String = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("timeStart", "{timeStart}")
            .queryParam("timeEnd", "{timeEnd}")
            .queryParam("nickname", "{nickname}")
            .encode()
            .toUriString()

        val params = mapOf(
            "timeStart" to startTime,
            "timeEnd" to endTime,
            "nickname" to "R2D2"
        )

        val response = restTemplate.exchange(
            urlTemplate,
            HttpMethod.GET,
            request,
            StatisticDto::class.java,
            params
        )

        val actualRes = response.body!!.validations
        Assertions.assertEquals(1, actualRes.size, "it should find validations in december")
        Assertions.assertTrue(
            actualRes.stream().allMatch {
                it.validationTime.isBefore(endTime) && it.validationTime.isAfter(startTime) && it.username.equals("R2D2")
            },
            "some dates are out of range or user is not R2D2: $actualRes"
        )
    }

    @Value("\${security.privateKey.common}")
    private lateinit var validateJwtStringKey: String

    @Value("\${security.jwtExpirationMs}")
    private lateinit var jwtExpirationMs: String
    fun generateJwtToken(username: String, roles: Set<String>, expiration: Date = Date(Date().time + jwtExpirationMs.toLong())): String {

        val validateJwtKey: SecretKey by lazy {
            val decodedKey = Decoders.BASE64.decode(validateJwtStringKey)
            Keys.hmacShaKeyFor(decodedKey)
        }

        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date())
            .setExpiration(expiration)
            .claim("roles", roles)
            .signWith(validateJwtKey)
            .compact()
    }
}