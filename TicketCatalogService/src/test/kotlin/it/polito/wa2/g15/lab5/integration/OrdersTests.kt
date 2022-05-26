package it.polito.wa2.g15.lab5.integration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.lab5.MyPostgresSQLContainer
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import it.polito.wa2.g15.lab5.repositories.TicketOrderRepository
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpHeaders
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.util.*
import javax.crypto.SecretKey


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrdersTests {

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

    @Value("\${security.jwtExpirationMs}")
    private lateinit var jwtExpirationMs: String
    @Value("\${security.privateKey.common}")
    private lateinit var validateJwtStringKey: String

    @Autowired
    lateinit var ticketItemRepo : TicketItemRepository
    @Autowired
    lateinit var ticketOrderRepository: TicketOrderRepository
    @Autowired
    lateinit var webTestClient: WebTestClient


    @BeforeEach
    fun initDb() = runBlocking {
        println("start init db ...")

        ticketItemRepo.save(TicketItem(
            null,
            "ORDINAL",
            1.5,
            0,
            200,
            120*60
        )
        )
        ticketItemRepo.save(TicketItem(
            null,
            "WEEKEND-PASS",
            5.0,
            0,
            27,
            14*24*60*60
        )
        )

        ticketOrderRepository.save(
            TicketOrder(
                null,
                "PENDING",
                5.0,
                "R2D2",
                2,
                1,
                LocalDate.now(),
                "1"
            )
        )

        println("... init db finished")
    }

    @AfterEach
    fun tearDownDB() = runBlocking{
        println("start tear down db...")
        ticketItemRepo.deleteAll()
        ticketOrderRepository.deleteAll()
        println("...end tear down db")
    }

    @Test
    fun viewUserOrders() = runBlocking{
        webTestClient.get()
            .uri("/orders/")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .exchange()
            .expectStatus().isUnauthorized
            .expectBodyList(TicketOrder::class.java)


        webTestClient.get()
            .uri("/orders/")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(HttpHeaders.AUTHORIZATION,"Bearer ${generateJwtToken(
                "R2D2",
                setOf("CUSTOMER")
            )}")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TicketOrder::class.java)


        Assertions.assertTrue(true)

    }

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