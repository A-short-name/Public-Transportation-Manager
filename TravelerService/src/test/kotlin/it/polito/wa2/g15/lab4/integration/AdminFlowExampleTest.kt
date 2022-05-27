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
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.*
import java.util.*
import javax.crypto.SecretKey


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminFlowExampleTest {
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


    final val localBirthDateOfDroids: LocalDate = LocalDate.of(1980, Month.NOVEMBER, 10)
    
    final val c3poUser = UserDetails(
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


    final val t1iatLocalDateTime: LocalDateTime = LocalDateTime.of(2000, Month.DECEMBER, 25, 0, 0)
    final val t1expLocalDateTime: LocalDateTime = LocalDateTime.of(2000, Month.DECEMBER, 31, 0, 0)
    final val t1iat = Date(t1iatLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))
    final val t1exp = Date(t1expLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))
    final val fakeJws = "fakeJws"
    final val t1Zid = "ABC"
    final val t1Type = "ORDINAL"
    final val t1ValidFrom = ZonedDateTime.now(ZoneId.of("UTC"))
    final val t1Duration = 300*60*1000L

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
    fun `get list of username`(){
        val validAdminToken = generateJwtToken("BigBoss",setOf("ADMIN"))

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validAdminToken")

        val request = HttpEntity("",requestHeader)

        val response : ResponseEntity<List<String>> = restTemplate.exchange(
                "http://localhost:$port/admin/travelers/",
                HttpMethod.GET,
                request
        )
        Assertions.assertEquals(HttpStatus.OK,response.statusCode,"response status code not expected")
        Assertions.assertEquals(userRepo.count(), response.body!!.stream().count(),"response body length wrong")

        for (username in response.body!!) {
            Assertions.assertInstanceOf(String::class.java, username)
            Assertions.assertTrue(username==r2d2User.username || username==c3poUser.username, "username is wrong" )
        }
    }

    @Test
    fun `get empty list of username`(){

        val validAdminToken = generateJwtToken("BigBoss",setOf("ADMIN"))

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validAdminToken")

        ticketPurchasedRepository.deleteAll()
        userRepo.deleteAll()

        val request = HttpEntity("", requestHeader)

        val response : ResponseEntity<List<String>> = restTemplate.exchange(
                "http://localhost:$port/admin/travelers/",
                HttpMethod.GET,
                request
        )
        Assertions.assertEquals(HttpStatus.OK,response.statusCode,"response status code not expected")
        Assertions.assertEquals(0, response.body!!.size,"response body length wrong")
    }

    @Test
    fun `get a valid profile from userID`(){
        val r2d2ID = r2d2User.getId()
        Assertions.assertNotNull(r2d2ID)
        Assertions.assertTrue(r2d2ID != 0L)

        val validAdminToken = generateJwtToken("BigBoss",setOf("ADMIN"))

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validAdminToken")

        val request = HttpEntity("",requestHeader)

        val response : ResponseEntity<UserProfileAdminViewDTO> = restTemplate.exchange(
                "http://localhost:$port/admin/traveler/$r2d2ID/profile/",
                HttpMethod.GET,
                request
        )
        Assertions.assertEquals(HttpStatus.OK,response.statusCode,"response status code not expected")
        Assertions.assertEquals(r2d2User.toUserProfileAdminViewDTO(), response.body)
    }

    @Test
    fun `no profile by invalid userID`(){
        val invalidUserId = -1

        val validAdminToken = generateJwtToken("BigBoss",setOf("ADMIN"))

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validAdminToken")

        val request = HttpEntity("",requestHeader)

        val response : ResponseEntity<Unit> = restTemplate.exchange(
                "http://localhost:$port/admin/traveler/$invalidUserId/profile/",
                HttpMethod.GET,
                request
        )
        Assertions.assertEquals(HttpStatus.NOT_FOUND,response.statusCode,"response status code not expected")
    }

    @Test
    fun `get tickets from userID`(){
        val r2d2ID = r2d2User.getId()
        Assertions.assertNotNull(r2d2ID)
        Assertions.assertTrue(r2d2ID != 0L)

        val validAdminToken = generateJwtToken("BigBoss",setOf("ADMIN"))

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validAdminToken")

        val request = HttpEntity("",requestHeader)

        val response : ResponseEntity<Set<TicketDTO>> = restTemplate.exchange(
                "http://localhost:$port/admin/traveler/$r2d2ID/tickets/",
                HttpMethod.GET,
                request
        )
        Assertions.assertEquals(HttpStatus.OK,response.statusCode,"response status code not expected")
        Assertions.assertEquals(r2d2User.ticketPurchased.map { it.toDTO() }.toSet(), response.body)
        Assertions.assertEquals(0, response.body!!.size)

        r2d2User.addTicketPurchased(t1Expired)

        ticketPurchasedRepository.save(t1Expired)
        Assertions.assertNotEquals(r2d2User.ticketPurchased.map { it.toDTO() }, response.body)
        Assertions.assertTrue(r2d2User.ticketPurchased.count()==1)

        val response2 : ResponseEntity<Set<TicketDTO>> = restTemplate.exchange(
                "http://localhost:$port/admin/traveler/$r2d2ID/tickets/",
                HttpMethod.GET,
                request
        )
        Assertions.assertEquals(HttpStatus.OK,response2.statusCode,"response status code not expected")
        Assertions.assertEquals(r2d2User.ticketPurchased.map { it.toDTO() }.toSet(), response2.body!!)
    }

    @Test
    fun `get tickets from userID after a purchase`(){
        val r2d2ID = r2d2User.getId()
        Assertions.assertNotNull(r2d2ID)
        Assertions.assertTrue(r2d2ID != 0L)

        val validAdminToken = generateJwtToken("BigBoss",setOf("ADMIN"))

        /*  Purchase a bunch of ticket */
        val validC3POToken = generateJwtToken("C3PO",setOf("SERVICE"))

        val requestHeader1 = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader1.add(jwtSecurityHeader, "$jwtTokenPrefix $validC3POToken")
        val zid = "123"
        val quantity = 5
        val validFrom = ZonedDateTime.now(ZoneId.of("UTC"))

        val request1 = HttpEntity(
            TicketFromCatalogDTO(-1,"ORDINAL", validFrom,zid,quantity),requestHeader1)

        val response1 : ResponseEntity<Set<TicketDTO>> = restTemplate.postForEntity(
                "http://localhost:$port/services/user/${r2d2User.username}/tickets/add/",
                request1
        )
        Assertions.assertEquals(HttpStatus.ACCEPTED,response1.statusCode,"status code not accepted")

        val requestHeader2 = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader2.add(jwtSecurityHeader, "$jwtTokenPrefix $validAdminToken")

        val request2 = HttpEntity("",requestHeader2)

        val response2 : ResponseEntity<Set<TicketDTO>> = restTemplate.exchange(
                "http://localhost:$port/admin/traveler/$r2d2ID/tickets/",
                HttpMethod.GET,
                request2
        )
        Assertions.assertEquals(HttpStatus.OK,response2.statusCode,"response status code not expected")
        Assertions.assertEquals(quantity, response2.body!!.count())
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
