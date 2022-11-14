package it.polito.wa2.g15.lab4.integration

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.lab4.dtos.TicketDTO
import it.polito.wa2.g15.lab4.dtos.TicketFromCatalogDTO
import it.polito.wa2.g15.lab4.dtos.UserProfileDTO
import it.polito.wa2.g15.lab4.dtos.toDTO
import it.polito.wa2.g15.lab4.entities.TicketPurchased
import it.polito.wa2.g15.lab4.entities.UserDetails
import it.polito.wa2.g15.lab4.repositories.TicketPurchasedRepository
import it.polito.wa2.g15.lab4.repositories.UserDetailsRepository
import it.polito.wa2.g15.lab4.security.JwtUtils
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
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.io.ByteArrayResource
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
import java.io.File
import java.io.IOException
import java.time.*
import java.util.*
import javax.crypto.SecretKey
import javax.imageio.ImageIO

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
    
    @Autowired
    lateinit var jwtTicketUtils: JwtUtils
    
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
    private final val t1Duration = 300 * 60 * 1000L
    
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
    
    @Throws(IOException::class, NotFoundException::class)
    private fun decodeQR(qrCodeimage: File): String {
        val bufferedImage = ImageIO.read(qrCodeimage)
        val source: LuminanceSource = BufferedImageLuminanceSource(bufferedImage)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val result: Result = QRCodeReader().decode(bitmap)
        return result.text
    }
    
    @Test
    fun `customer with ticket`() {
        val c3p0ticket = ticketPurchasedRepository.findAll().first()
        val otherSub = c3p0ticket.getId()!!
        
        // Create valid ticket to test a real jwt
        val t2iatLocalDateTime: LocalDateTime = LocalDateTime.of(2000, Month.DECEMBER, 25, 0, 0)
        val t2expLocalDateTime: LocalDateTime = LocalDateTime.of(2500, Month.DECEMBER, 31, 0, 0)
        val t2iat = Date(t2iatLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))
        val t2exp = Date(t2expLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))
        val t2Zid = "ABC"
        val t2Type = "ORDINAL"
        val t2ValidFrom = ZonedDateTime.now(ZoneId.of("UTC"))
        val t2Duration = 300 * 60 * 1000L
        
        val t2valid = TicketPurchased(
            t2iat,
            t2exp,
            t2Zid,
            "",
            r2d2User,
            t2Type,
            t2ValidFrom,
            t2Duration
        )
        r2d2User.addTicketPurchased(t2valid)
        val ticketdb = ticketPurchasedRepository.save(t2valid)
        val sub = ticketdb.getId()!!
        val realJws = jwtTicketUtils.generateTicketJwt(
            sub,
            t2iat,
            t2exp,
            t2Zid,
            t2Type,
            t2ValidFrom.toEpochSecond(),
            r2d2User.username
        )
        t2valid.jws = realJws
        ticketPurchasedRepository.save(t2valid)

        //print(ticketPurchasedRepository.findAll())
        Assertions.assertEquals(2, ticketPurchasedRepository.count(), "ticket t2 not saved in db")

        // Assert that the user r2d2 can login
        val validR2D2Token = generateJwtToken("R2D2", setOf("CUSTOMER"))

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validR2D2Token")
        
        val request = HttpEntity("", requestHeader)
        
        val response: ResponseEntity<UserProfileDTO> = restTemplate.exchange(
            "http://localhost:$port/my/profile/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "r2d2 should be able to see its profile")
        Assertions.assertEquals(r2d2User.toDTO(), response.body, "user details are wrong")
        
        //try retrieving ticket with non-existing id
        val response2: ResponseEntity<ByteArrayResource> = restTemplate.exchange(
            "http://localhost:$port/my/tickets/999",
            HttpMethod.GET,
            request
        )
        //println(response2)
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response2.statusCode, "found not existing ticket")
        
        //try retrieving ticket owned by a different user
        val response3: ResponseEntity<ByteArrayResource> = restTemplate.exchange(
            "http://localhost:$port/my/tickets/$otherSub",
            HttpMethod.GET,
            request
        )
        //println(response3)
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response3.statusCode, "retrieved ticket owned by different user")
        
        //correct request
        val response4: ResponseEntity<ByteArrayResource> = restTemplate.exchange(
            "http://localhost:$port/my/tickets/$sub",
            HttpMethod.GET,
            request
        )
        //println(response4)
        Assertions.assertEquals(HttpStatus.OK, response4.statusCode, "didn't retrieve ticket")
        
        // Extract jwt contained in the response and save it to a file
        val fs = System.getProperty("file.separator")
        
        val ticketJwtBAResource = response4.body!!
        val ticketJwtIS = ticketJwtBAResource.inputStream
        val bImage = ImageIO.read(ticketJwtIS)
        ImageIO.write(
            bImage,
            "png",
            File(
                "src${fs}test${fs}kotlin${fs}it${fs}polito${fs}wa2${fs}g15${fs}lab4${fs}integration${fs}test_qr.png"
            )
        )
        //println("test_qr.png created")
        
        // Read file containing qr and decode its content (the jwt)
        val file =
            File(
                "src${fs}test${fs}kotlin${fs}it${fs}polito${fs}wa2${fs}g15${fs}lab4${fs}integration${fs}test_qr.png"
            )
        
        val decodedJwt = decodeQR(file)
        //println(decodedText)
        Assertions.assertEquals(t2valid.jws, decodedJwt, "wrong jwt retrieved")
        
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
    fun `put a new user details`() {
        val validC3POToken = generateJwtToken("NewUser", setOf("CUSTOMER"))
        
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validC3POToken")
        
        val request = HttpEntity(
            UserProfileDTO(
                "NewUser",
                "Nuovo mondo",
                LocalDate.of(1990, 10, 10),
                "33333333"
            ), requestHeader
        )
        
        val response: ResponseEntity<Unit> = restTemplate.exchange(
            "http://localhost:$port/my/profile/",
            HttpMethod.PUT,
            request
        )
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "response status code not expected")
        val users = userRepo.findAll()
        Assertions.assertEquals(3, users.count(), "user not aved correctly")
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
            TicketFromCatalogDTO(-1, "ORDINAL", validFrom, zid, quantity), requestHeader
        )
        
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
            TicketFromCatalogDTO(-1, "ORDINAL", validFrom, zid, quantity), requestHeader
        )
        
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
    fun `customer tries to get user birth date`() {
        val validC3POToken = generateJwtToken("R2D2", setOf("CUSTOMER"))
        
        val requestHeader1 = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader1.add(jwtSecurityHeader, "$jwtTokenPrefix $validC3POToken")
        
        val request1 = HttpEntity(null, requestHeader1)
        
        val response1: ResponseEntity<Unit> = restTemplate.exchange(
            "http://localhost:$port/services/user/${c3poUser.username}/birthdate/",
            HttpMethod.GET,
            request1
        )
        
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response1.statusCode, "status code ok")
    }
    
    @Test
    fun `service tries to get user birth date`() {
        val validC3POToken = generateJwtToken("C3PO", setOf("SERVICE"))
        
        val requestHeader1 = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader1.add(jwtSecurityHeader, "$jwtTokenPrefix $validC3POToken")
        
        val request1 = HttpEntity(null, requestHeader1)
        
        val response1: ResponseEntity<LocalDate> = restTemplate.exchange(
            "http://localhost:$port/services/user/${c3poUser.username}/birthdate/",
            HttpMethod.GET,
            request1
        )
        Assertions.assertEquals(HttpStatus.OK, response1.statusCode, "status code not found")
        Assertions.assertEquals(c3poUser.dateOfBirth, response1.body)
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