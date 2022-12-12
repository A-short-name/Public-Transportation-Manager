package it.polito.wa2.g15.validatorservice.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.validatorservice.MyPostgresSQLContainer
import it.polito.wa2.g15.validatorservice.entities.TicketFields
import it.polito.wa2.g15.validatorservice.exceptions.ValidationException
import it.polito.wa2.g15.validatorservice.repositories.TicketValidationRepository
import org.junit.jupiter.api.*
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.crypto.SecretKey


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ValidationTest {
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

    @Autowired
    lateinit var service: ValidationService

    @Value("\${security.privateKey.traveler}")
    private lateinit var validateJwtStringKey: String

    @Mock
    lateinit var mockedRestClientSvc: EmbeddedSystemRestClientService


    @BeforeEach
    fun initDb() {


        Mockito.`when`(mockedRestClientSvc.getValidationKey())
            .thenReturn(validateJwtStringKey)
        service.embeddedSystemRestClientService = mockedRestClientSvc
    }

    @AfterEach
    fun tearDownDb() {
        repo.deleteAll()
    }

    @Test
    fun `should valid a pass ticket`() {
        val time = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            1,
            0,
            0
        )
        val validFrom: ZonedDateTime = ZonedDateTime.of(
            time, ZoneId.systemDefault()
        )
        val signedJwtTicket = generateJwtTicket(
            userName = "R2D2",
            validZone = "ABC",
            sub = 1,
            validFrom = validFrom.toEpochSecond(),
            exp = Date(Date().time + 100_000_000L),
            type = "MONTHLY-PASS",
            iat = Date(Date().time - 100_000L)
        )
        val clientZid = "ABC"
        assertDoesNotThrow("Validation should not throw exception") {
            service.validateTicket(
                signedJwtTicket,
                clientZid
            )
        }
    }

    @Test
    fun `should valid an ordinal ticket`() {
        val time = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            1,
            0,
            0
        )
        val validFrom: ZonedDateTime = ZonedDateTime.of(
            time, ZoneId.systemDefault()
        )
        val signedJwtTicket = generateJwtTicket(
            userName = "R2D2",
            validZone = "ABC",
            sub = 1,
            validFrom = validFrom.toEpochSecond(),
            exp = Date(Date().time + 100_000_000L),
            type = "ORDINAL",
            iat = Date(Date().time - 100_000L)
        )
        val clientZid = "ABC"
        assertDoesNotThrow("Validation should not throw exception") {
            service.validateTicket(
                signedJwtTicket,
                clientZid
            )
        }
    }

    @Test
    fun `should invalid ordinal ticket because it is already used`() {
        val time = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            1,
            0,
            0
        )
        val validFrom: ZonedDateTime = ZonedDateTime.of(
            time, ZoneId.systemDefault()
        )
        val signedJwtTicket = generateJwtTicket(
            userName = "R2D2",
            validZone = "ABC",
            sub = 1,
            validFrom = validFrom.toEpochSecond(),
            exp = Date(Date().time + 100_000_000L),
            type = "ORDINAL",
            iat = Date(Date().time - 100_000L)
        )
        val clientZid = "ABC"
        assertDoesNotThrow("Validation should not throw exception") {
            service.validateTicket(
                signedJwtTicket,
                clientZid
            )
        }
        assertThrows<ValidationException>("Validation throw exception because of multiple validation of the same ordinal ticket")
        { service.validateTicket(signedJwtTicket, clientZid) }
    }

    @Test
    fun `should valid pass ticket multiple times`() {
        val time = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            1,
            0,
            0
        )
        val validFrom: ZonedDateTime = ZonedDateTime.of(
            time, ZoneId.systemDefault()
        )
        val signedJwtTicket = generateJwtTicket(
            userName = "R2D2",
            validZone = "ABC",
            sub = 1,
            validFrom = validFrom.toEpochSecond(),
            exp = Date(Date().time + 100_000_000L),
            type = "MONTHLY-PASS",
            iat = Date(Date().time - 100_000L)
        )
        val clientZid = "ABC"
        assertDoesNotThrow("Validation should not throw exception") {
            service.validateTicket(
                signedJwtTicket,
                clientZid
            )
        }
        assertDoesNotThrow("Validation should not throw exception") {
            service.validateTicket(
                signedJwtTicket,
                clientZid
            )
        }
        assertDoesNotThrow("Validation should not throw exception") {
            service.validateTicket(
                signedJwtTicket,
                clientZid
            )
        }

    }

    @Test
    fun `invalid expired ticket`() {
        val time = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            1,
            0,
            0
        )
        val validFrom: ZonedDateTime = ZonedDateTime.of(
            time, ZoneId.systemDefault()
        )
        val signedJwtTicket = generateJwtTicket(
            userName = "R2D2",
            validZone = "ABC",
            sub = 1,
            validFrom = validFrom.toEpochSecond(),
            exp = Date(Date().time - 100L),
            type = "MONTHLY-PASS",
            iat = Date(Date().time - 100_000L)
        )
        val clientZid = "ABC"
        assertThrows<ValidationException>("Validation throw exception because of expiration")
        { service.validateTicket(signedJwtTicket, clientZid) }
    }

    @Test
    fun `invalid not yet valid ticket`() {
        val time = LocalDateTime.now().plusDays(3)
        val validFrom: ZonedDateTime = ZonedDateTime.of(
            time, ZoneId.systemDefault()
        )
        val signedJwtTicket = generateJwtTicket(
            userName = "R2D2",
            validZone = "ABC",
            sub = 1,
            validFrom = validFrom.toEpochSecond(),
            exp = Date(Date().time + 100_000_000L),
            type = "MONTHLY-PASS",
            iat = Date(Date().time - 100_000L)
        )
        val clientZid = "ABC"
        assertThrows<ValidationException>("Validation throw exception because of is not yet valid (validFrom)")
        { service.validateTicket(signedJwtTicket, clientZid) }
    }

    @Test
    fun `invalid zone ticket`() {
        val time = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            1,
            0,
            0
        )
        val validFrom: ZonedDateTime = ZonedDateTime.of(
            time, ZoneId.systemDefault()
        )
        val signedJwtTicket = generateJwtTicket(
            userName = "R2D2",
            validZone = "Z",
            sub = 1,
            validFrom = validFrom.toEpochSecond(),
            exp = Date(Date().time + 100_000_000L),
            type = "MONTHLY-PASS",
            iat = Date(Date().time - 100_000L)
        )
        val clientZid = "ABC"
        assertThrows<ValidationException>("Validation throw exception because of invalid zone")
        { service.validateTicket(signedJwtTicket, clientZid) }
    }


    fun generateJwtTicket(
        userName: String,
        validZone: String,
        sub: Int,
        validFrom: Long,
        type: String,
        iat: Date,
        exp: Date
    ): String {

        val validateJwtKey: SecretKey by lazy {
            val decodedKey = Decoders.BASE64.decode(validateJwtStringKey)
            Keys.hmacShaKeyFor(decodedKey)
        }

        return Jwts.builder()
            .setSubject(sub.toString())
            .setIssuedAt(iat)
            .setExpiration(exp)
            .claim(TicketFields.TYPE, type)
            .claim(TicketFields.VALID_FROM, validFrom)
            .claim(TicketFields.USERNAME, userName)
            .claim(TicketFields.VALID_ZONES, validZone)
            .signWith(validateJwtKey)
            .compact()
    }
}