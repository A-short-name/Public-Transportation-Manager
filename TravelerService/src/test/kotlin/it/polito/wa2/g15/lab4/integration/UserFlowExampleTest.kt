package it.polito.wa2.g15.lab4.integration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.lab4.dtos.*
import it.polito.wa2.g15.lab4.entities.TicketPurchased
import it.polito.wa2.g15.lab4.entities.UserDetails
import it.polito.wa2.g15.lab4.repositories.TicketPurchasedRepository
import it.polito.wa2.g15.lab4.repositories.UserDetailsRepository
import it.polito.wa2.g15.lab4.security.WebSecurityConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.*
import java.util.*
import javax.crypto.SecretKey

//https://stackoverflow.com/questions/59007414/testcontainers-postgresqlcontainer-with-kotlin-unit-test-not-enough-informatio
class MyPostgresSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgresSQLContainer>(imageName)

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserFlowExampleTest {
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

    @Value("\${ticket.generation}")
    lateinit var ticketGeneration: String

    @Value("\${security.token.prefix}")
    lateinit var jwtTokenPrefix: String
    
    @Value("\${security.header}")
    lateinit var jwtSecurityHeader: String
    
    @LocalServerPort
    protected var port: Int = 0
    
    @Autowired
    lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    lateinit var userRepo: UserDetailsRepository
    
    @Autowired
    lateinit var ticketPurchasedRepository: TicketPurchasedRepository
    
    @Autowired
    lateinit var securityConfig: WebSecurityConfig
    
    @Autowired
    lateinit var csrfTokenRepository: CsrfTokenRepository

    private final val localBirthDateOfDroids: LocalDate = LocalDate.of(1980, Month.NOVEMBER, 10)

    private final val c3poUser = UserDetails(
        "C3PO",
        "C3PO",
        "Tatooine",
        localBirthDateOfDroids,
        "3334593945",
        mutableSetOf()
    )
    val r2d2User = UserDetails(
        "R2D2",
        "R2D2",
        "Tatooine",
        localBirthDateOfDroids,
        "3314593945",
        mutableSetOf()
    )

    private final val t1iatLocalDateTime: LocalDateTime = LocalDateTime.of(2000, Month.DECEMBER, 25, 0, 0)
    private final val t1expLocalDateTime: LocalDateTime = LocalDateTime.of(2000, Month.DECEMBER, 31, 0, 0)
    private final val t1iat = Date(t1iatLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))
    private final val t1exp = Date(t1expLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))
    private final val fakeJws = "fakeJws"
    private final val t1Zid = "ABC"
    private final val t1Type = "ORDINAL"
    private final val t1ValidFrom = ZonedDateTime.now(ZoneId.of("UTC"))
    private final val t1Duration = 300*60*1000L
    
    val t1Expired = TicketPurchased(
        t1iat,
        t1exp,
        t1Zid,
        fakeJws,
        c3poUser,
        t1Type,
        t1ValidFrom,
        t1Duration
    )

    @BeforeEach
    fun initDb() {
        
        if (userRepo.count() == 0L) {
            c3poUser.addTicketPurchased(t1Expired)
            userRepo.save(r2d2User)
            userRepo.save(c3poUser)
            ticketPurchasedRepository.save(t1Expired)
            Assertions.assertEquals(1, ticketPurchasedRepository.count(), "ticket not saved in db")
            Assertions.assertEquals(2, userRepo.count(), "user not saved in db")
        }
    }
    
    @AfterEach
    fun tearDownDb() {
        ticketPurchasedRepository.deleteAll()
        userRepo.deleteAll()
    }
    
    @Test
    fun `customer without tickets details test`() {
        
        val validR2D2Token = generateJwtToken("R2D2", setOf("CUSTOMER"))
        
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validR2D2Token")
        
        val request = HttpEntity("", requestHeader)
        
        val response: ResponseEntity<UserProfileDTO> = restTemplate.exchange(
            "http://localhost:$port/my/profile/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "response status code not expected")
        
        Assertions.assertEquals(r2d2User.toDTO(), response.body, "wrong profile user")
        
        Assertions.assertEquals(r2d2User.toDTO(), response.body, "user is wrong")
        val response2: ResponseEntity<Set<TicketDTO>> = restTemplate.exchange(
            "http://localhost:$port/my/tickets/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(0, response2.body!!.size, "wrong num of tickets")
    }

    @Test
    fun `put a new user details`(){
        val validC3POToken = generateJwtToken("NewUser", setOf("CUSTOMER"))

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validC3POToken")

        val request = HttpEntity(UserProfileDTO(
            "NewUser",
            "Nuovo mondo",
            LocalDate.of(1990,10,10),
            "33333333"
        ), requestHeader)

        val response: ResponseEntity<Unit> = restTemplate.exchange(
            "http://localhost:$port/my/profile/",
            HttpMethod.PUT,
            request
        )
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "response status code not expected")
        val users = userRepo.findAll()
        Assertions.assertEquals(3,users.count(),"user not aved correctly")
    }

    @Test
    fun `customer with tickets test`() {
        val validC3POToken = generateJwtToken("C3PO", setOf("CUSTOMER"))
        
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validC3POToken")
        
        val request = HttpEntity("", requestHeader)
        
        val response: ResponseEntity<UserProfileDTO> = restTemplate.exchange(
            "http://localhost:$port/my/profile/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "response status code not expected")
        
        Assertions.assertEquals(c3poUser.toDTO(), response.body, "user details are wrong")
        
        val response2: ResponseEntity<Set<TicketDTO>> = restTemplate.exchange(
            "http://localhost:$port/my/tickets/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.OK, response2.statusCode, "response status code not expected")
        
        for (ticketDto in response2.body!!)
            Assertions.assertEquals(t1Expired.toDTO(), ticketDto, "ticket zid is wrong")
        
    }
    
    @Test
    fun `customer tries to purchase tickets`() {

        val validR2D2Token = generateJwtToken("R2D2", setOf("CUSTOMER"))
        
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validR2D2Token")

        val zid = "123"
        val quantity = 5
        val validFrom = ZonedDateTime.now(ZoneId.of("UTC"))

        val request1 = HttpEntity(
            TicketFromCatalogDTO(-1,"ORDINAL", validFrom,zid,quantity),requestHeader)
        
        val response1 = restTemplate.postForEntity<Set<TicketDTO>>(
            "http://localhost:$port/services/user/${r2d2User.username}/tickets/add/",
            request1
        )
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response1.statusCode, "status code accepted")
    }
    
    @Test
    fun userNotFoundInDb() {
        val validTrudyToken = generateJwtToken("TRUDY", setOf("CUSTOMER"))
        
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validTrudyToken")
        
        val request = HttpEntity("", requestHeader)
        
        val response: ResponseEntity<UserProfileDTO> = restTemplate.exchange(
            "http://localhost:$port/my/profile/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.statusCode, "response status code not expected")
        
        val response2: ResponseEntity<Set<TicketDTO>> = restTemplate.exchange(
            "http://localhost:$port/my/tickets/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response2.statusCode, "response status code not expected")
        
    }
    
    @Test
    fun `expired jwt token of user`() {
        
        val expiredLocalDate: LocalDateTime = LocalDateTime.of(1999, Month.DECEMBER, 31, 0, 0)
        val expiredDate = Date(expiredLocalDate.toEpochSecond(ZoneOffset.ofHours(0)))
        val expiredC3POToken = generateJwtToken(
            "C3PO",
            setOf("CUSTOMER"),
            expiredDate
        )
        
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $expiredC3POToken")
        
        val request = HttpEntity("", requestHeader)
        
        val response: ResponseEntity<Set<TicketDTO>> = restTemplate.exchange(
            "http://localhost:$port/my/profile/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.statusCode, "response status code not expected")
        
        val response2: ResponseEntity<Set<TicketDTO>> = restTemplate.exchange(
            "http://localhost:$port/my/tickets/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response2.statusCode, "response status code not expected")
        
    }
    
    @Test
    fun `an admin tries customer api`() {
        val validR2D2Token = generateJwtToken("R2D2", setOf("ADMIN"))
        
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validR2D2Token")
        val quantity = 5
        val validFrom = ZonedDateTime.now(ZoneId.of("UTC"))

        val zid = "123"
        val request1 = HttpEntity(
            TicketFromCatalogDTO(-1,"ORDINAL", validFrom,zid,quantity),requestHeader)
        
        val response1 = restTemplate.postForEntity<Set<TicketDTO>>(
            "http://localhost:$port/services/user/${r2d2User.username}/tickets/add/",
            request1
        )
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response1.statusCode, "status code ok")
    }
    
    @Test
    fun `a customer tries admin api`() {
        
        val validC3POToken = generateJwtToken(
            "C3PO",
            setOf("CUSTOMER")
        )
        
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validC3POToken")
        
        val request = HttpEntity("", requestHeader)
        
        val response: ResponseEntity<List<String>> = restTemplate.exchange(
            "http://localhost:$port/admin/travelers/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.statusCode, "response status code not expected")
        
        val response2: ResponseEntity<List<String>> = restTemplate.exchange(
            "http://localhost:$port/admin/travelers/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response2.statusCode, "response status code not expected")
        
    }

    @Test
    fun `customer tries to get user birth date`(){
        val validC3POToken = generateJwtToken("R2D2",setOf("CUSTOMER"))

        val requestHeader1 = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader1.add(jwtSecurityHeader, "$jwtTokenPrefix $validC3POToken")


        val request1 = HttpEntity(null,requestHeader1)

        val response1 : ResponseEntity<Unit> = restTemplate.exchange(
            "http://localhost:$port/services/user/${c3poUser.username}/birthdate/",
            HttpMethod.GET,
            request1
        )

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response1.statusCode, "status code ok")
    }

    @Test
    fun `service tries to get user birth date`(){
        val validC3POToken = generateJwtToken("C3PO",setOf("SERVICE"))

        val requestHeader1 = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader1.add(jwtSecurityHeader, "$jwtTokenPrefix $validC3POToken")


        val request1 = HttpEntity(null,requestHeader1)

        val response1 : ResponseEntity<LocalDate> = restTemplate.exchange(
            "http://localhost:$port/services/user/${c3poUser.username}/birthdate/",
            HttpMethod.GET,
            request1
        )
        Assertions.assertEquals(HttpStatus.OK, response1.statusCode, "status code not found")
        Assertions.assertEquals(c3poUser.dateOfBirth,response1.body)
    }
    
    @Value("\${security.privateKey.common}")
    private lateinit var validateJwtStringKey: String
    
    @Value("\${security.jwtExpirationMs}")
    private lateinit var jwtExpirationMs: String
    fun generateJwtToken(
        username: String,
        roles: Set<String>,
        expiration: Date = Date(Date().time + jwtExpirationMs.toLong())
    ): String {
        
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