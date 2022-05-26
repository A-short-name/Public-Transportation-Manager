package it.polito.wa2.g15.lab5.paymentservice

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.g15.lab5.paymentservice.dtos.TransactionDTO
import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import it.polito.wa2.g15.lab5.paymentservice.repositories.TransactionRepository
import it.polito.wa2.g15.lab5.paymentservice.security.WebFluxSecurityConfig
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.Before
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.*
import java.util.*
import javax.crypto.SecretKey

class MyPostgresSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgresSQLContainer>(imageName)

//@DataR2dbcTest
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class ControllerTest {
    
    @TestConfiguration
    class TestConfig {
        @Bean
        fun initializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {
            val initializer = ConnectionFactoryInitializer()
            initializer.setConnectionFactory(connectionFactory)
            val populator = CompositeDatabasePopulator()
            populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("schema.sql")))
            initializer.setDatabasePopulator(populator)
            return initializer
        }
    }
    
    companion object {
        @Container
        val postgres = MyPostgresSQLContainer("postgres:latest")
        
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                ("r2dbc:postgresql://" + postgres.host + ":" + postgres.firstMappedPort + "/" + postgres.databaseName)
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
        }
    }
    
    @LocalServerPort
    var port: Int = 0
    
    @Autowired
    lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    lateinit var transactionRepository: TransactionRepository
    
    @Autowired
    lateinit var csrfTokenRepository: ServerCsrfTokenRepository
    
    @Autowired
    lateinit var securityConfig: WebFluxSecurityConfig
    
    @Value("\${security.header}")
    lateinit var jwtSecurityHeader: String
    
    @Value("\${security.token.prefix}")
    lateinit var jwtTokenPrefix: String
    
    private val logger = KotlinLogging.logger {}
    
    val t1 = Transaction(1, "BigBoss", 100.0, "7992-7398-713", "Roberto Boss", 1L)
    val t2 = Transaction(2, "BigBoss", 200.0, "7992-7398-713", "Roberto Boss", 2L)
    
    @BeforeEach
    fun initDb() {
        runBlocking {
/*            transactionRepository.deleteAll()
            transactionRepository.save(t1)
            logger.info("Saved: $t1")
            transactionRepository.save(t2)
            logger.info("Saved: $t2")
            Assertions.assertEquals(2, transactionRepository.count(), "transactions not saved in db")*/
        }
    }
    
    @Test
    fun getUserTransactions() {
        val validAdminToken = generateJwtToken("BigBoss", setOf("ADMIN"))
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validAdminToken")
        
        val request = HttpEntity("", requestHeader)
        
        val response: ResponseEntity<TransactionDTO> = restTemplate.exchange(
            "/transactions/",
            HttpMethod.GET,
            request
        )
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "response status code not expected")
        
        /*
        Assertions.assertEquals(r2d2User.toDTO(), response.body, "wrong profile user")
        */
    }
    /*    @AfterEach
        fun tearDownDb() {
            ticketPurchasedRepository.deleteAll()
            userRepo.deleteAll()
        }*/
    
    /*
        @Autowired
        lateinit var securityConfig: WebSecurityConfig
        
        @Autowired
        lateinit var csrfTokenRepository: CsrfTokenRepository
        */
    /*    @Autowired
        lateinit var connectionFactory:ConnectionFactory
        
        var client: DatabaseClient = DatabaseClient.builder()
                .connectionFactory(connectionFactory)
                //.bindMarkers(() -> BindMarkersFactory.named(":", "", 20).create())
                .namedParameters(true)
                .build();*/
    
    /*    @Autowired
        var posts: PostRepository? = null*/
    
    /*@Test
        fun testDatabaseClientExisted() {
            Assertions.assertNotNull(client)
        }
        
        @Test
        fun testPostRepositoryExisted() {
            assertNotNull(posts)
        }
        
        @Test
        fun existedOneItemInPosts() {
            assertThat(posts.count().block()).isEqualTo(1)
        }
        
        @Test
        fun testInsertAndQuery() {
            client.insert()
                .into("posts") //.nullValue("id", Integer.class)
                .value("title", "mytesttitle")
                .value("content", "testcontent")
                .then().block(Duration.ofSeconds(5))
            posts.findByTitleContains("%testtitle")
                .take(1)
                .`as`(StepVerifier::create)
                .consumeNextWith { p -> assertEquals("mytesttitle", p.getTitle()) }
                .verifyComplete()
        }*/
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