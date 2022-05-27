package it.polito.wa2.g15.lab5.paymentservice

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.lab5.paymentservice.dtos.TransactionDTO
import it.polito.wa2.g15.lab5.paymentservice.dtos.toDTO
import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import it.polito.wa2.g15.lab5.paymentservice.repositories.TransactionRepository
import it.polito.wa2.g15.lab5.paymentservice.security.WebFluxSecurityConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*
import javax.crypto.SecretKey

//https://stackoverflow.com/questions/59007414/testcontainers-postgresqlcontainer-with-kotlin-unit-test-not-enough-informatio
class MyPostgresSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgresSQLContainer>(imageName)

//@DataR2dbcTest
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControllerTest {
    
    /*    @TestConfiguration
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
        }*/
    
    companion object {
        @Container
        val postgres = MyPostgresSQLContainer("postgres:latest").apply {
            withDatabaseName("payments")
        }
        
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
    
    @Autowired
    lateinit var client: WebTestClient
    
    var t1 = Transaction(null, "BigBoss", 100.0, "7992-7398-713", "Roberto Boss", 1L)
    var t2 = Transaction(null, "BigBoss", 200.0, "7992-7398-713", "Roberto Boss", 2L)
    var t3 = Transaction(null, "Giovanni", 300.0, "7992-7398-713", "Giovannino Panevino", 3L)
    val userTransactions1: MutableList<TransactionDTO> = mutableListOf()
    val userTransactions2: MutableList<TransactionDTO> = mutableListOf()
    val allTransactions: MutableList<TransactionDTO> = mutableListOf()
    
    @BeforeEach
    fun initDb() {
        runBlocking {
            transactionRepository.deleteAll()
            t1 = transactionRepository.save(t1)
            logger.info("Saved: $t1")
            t2 = transactionRepository.save(t2)
            logger.info("Saved: $t2")
            userTransactions2.add(t1.toDTO())
            userTransactions2.add(t2.toDTO())
            
            t3 = transactionRepository.save(t3)
            logger.info("Saved: $t3")
            userTransactions1.add(t3.toDTO())
            
            allTransactions.add(t1.toDTO())
            allTransactions.add(t2.toDTO())
            allTransactions.add(t3.toDTO())
            Assertions.assertEquals(3, transactionRepository.count(), "transactions not saved in db")
            logger.info(transactionRepository.findAll().toList().toString())
        }
    }
    
    @Test
    fun getUserTransactions() {
        runBlocking {
            val requestHeader1 = securityConfig.generateCsrfHeader(csrfTokenRepository)
            val requestHeader2 = securityConfig.generateCsrfHeader(csrfTokenRepository)
            /* Unauthorized user */
            client.get()
                .uri("/transactions/")
                .headers { httpHeaders ->
                    httpHeaders.addAll(requestHeader1)
                }
                .exchange()
                .expectStatus().isUnauthorized
            
            /* User with 1 transaction */
            val validCustomerToken = generateJwtToken("Giovanni", setOf("CUSTOMER"))
            requestHeader1.add(jwtSecurityHeader, "$jwtTokenPrefix $validCustomerToken")
            
            client.get()
                .uri("/transactions/")
                .headers { httpHeaders ->
                    httpHeaders.addAll(requestHeader1)
                }
                .exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals("Content-Type", "application/x-ndjson")
                .expectBodyList(TransactionDTO::class.java)
                .consumeWith<WebTestClient.ListBodySpec<TransactionDTO>> {
                    //println(it.responseBody)
                    Assertions.assertEquals(userTransactions1, it.responseBody, "Wrong transactions found")
                }
            
            /* User with 2 transactions */
            val validAdminToken = generateJwtToken("BigBoss", setOf("ADMIN"))
            requestHeader2.add(jwtSecurityHeader, "$jwtTokenPrefix $validAdminToken")
            
            client.get()
                .uri("/transactions/")
                .headers { httpHeaders ->
                    httpHeaders.addAll(requestHeader2)
                }
                .exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals("Content-Type", "application/x-ndjson")
                .expectBodyList(TransactionDTO::class.java)
                .consumeWith<WebTestClient.ListBodySpec<TransactionDTO>> {
                    //println(it.responseBody)
                    Assertions.assertEquals(userTransactions2, it.responseBody, "Wrong transactions found")
                }
            
            //.expectBody().jsonPath("field").isEqualTo("value");
            /*                .post()
                            .uri("/resource")
                            .exchange()
                            .expectStatus().isCreated()
                            .expectHeader().valueEquals("Content-Type", "application/json")
                            .expectBody().jsonPath("field").isEqualTo("value");*/
            
        }
    }
    
    @Test
    fun getAllTransactions() {
        runBlocking {
            val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
            
            /* Unauthorized user */
            
            client.get()
                .uri("/admin/transactions/")
                .headers { httpHeaders ->
                    httpHeaders.addAll(requestHeader)
                }
                .exchange()
                .expectStatus().isUnauthorized
            
            /* 3 total transactions */
            val validAdminToken = generateJwtToken("BigBoss", setOf("ADMIN"))
            requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validAdminToken")
            
            val response: MutableList<TransactionDTO> = mutableListOf()
            
            client.get()
                .uri("/admin/transactions/")
                .headers { httpHeaders ->
                    httpHeaders.addAll(requestHeader)
                }
                .exchange()
                .expectStatus().isOk
                .expectHeader().valueEquals("Content-Type", "application/x-ndjson")
                .expectBodyList(TransactionDTO::class.java)
                .consumeWith<WebTestClient.ListBodySpec<TransactionDTO>> {
                    println(it.responseBody)
                    Assertions.assertEquals(allTransactions, it.responseBody, "Wrong transactions found")
                }
        }
    }
    
    @Test
    fun `non admin cannot obtain all transactions`() {
        runBlocking {
            val validCustomerToken = generateJwtToken("Alberto", setOf("CUSTOMER"))
            val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
            requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validCustomerToken")
            
            val response: MutableList<TransactionDTO> = mutableListOf()
            
            client.get()
                .uri("/admin/transactions/")
                .headers { httpHeaders ->
                    httpHeaders.addAll(requestHeader)
                }
                .exchange()
                .expectStatus().isForbidden
        }
    }
    
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