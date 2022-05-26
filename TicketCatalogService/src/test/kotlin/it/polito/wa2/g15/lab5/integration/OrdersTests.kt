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
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ListBodySpec
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

    val tickets = listOf(
        TicketItem(
            null,
            "ORDINAL",
            1.5,
            0,
            200,
            120 * 60
        ), TicketItem(
            null,
            "WEEKEND-PASS",
            5.0,
            0,
            27,
            14 * 24 * 60 * 60
        )
    )

    val orders = listOf(
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

    val addedTickets : MutableList<TicketItem> = mutableListOf()
    val addedOrders : MutableList<TicketOrder> = mutableListOf()

    @BeforeEach
    fun initDb() = runBlocking {
        println("start init db ...")

        tickets.forEach { addedTickets.add(ticketItemRepo.save(it)) }

        orders.forEach { addedOrders.add(ticketOrderRepository.save(it)) }

        println("... init db finished")
    }

    @AfterEach
    fun tearDownDB() = runBlocking{
        println("start tear down db...")
        ticketItemRepo.deleteAll()
        ticketOrderRepository.deleteAll()
        addedOrders.clear()
        addedTickets.clear()
        println("...end tear down db")
    }

    @Test
    fun viewUserOrders() {
        /* Unauthorized user */
        webTestClient.get()
            .uri("/orders/")
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus().isUnauthorized
            .expectBodyList(TicketOrder::class.java)
            .hasSize(0)

        /* User with one order */
        webTestClient.get()
            .uri("/orders/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "R2D2",
                        setOf("CUSTOMER")
                    )
                }"
            )
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TicketOrder::class.java)
            .hasSize(1)
            .consumeWith<ListBodySpec<TicketOrder>> {
                val body = it.responseBody!!
                Assertions.assertEquals(body.first(), addedOrders[0])
            }

        val newOrder = TicketOrder(
            null,
            "PENDING",
            10.0,
            "R2D2",
            2,
            2,
            LocalDate.now(),
            "1"
        )

        runBlocking { addedOrders.add(ticketOrderRepository.save(newOrder)) }

        /* User with two orders */
        webTestClient.get()
            .uri("/orders/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "R2D2",
                        setOf("CUSTOMER")
                    )
                }"
            )
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TicketOrder::class.java)
            .hasSize(2)
            .consumeWith<ListBodySpec<TicketOrder>> {
                val body = it.responseBody!!
                Assertions.assertEquals(body[0], addedOrders[0])
                Assertions.assertEquals(body[1], addedOrders[1])
            }

        /* User with no orders */
        webTestClient.get()
            .uri("/orders/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "R3D3",
                        setOf("CUSTOMER")
                    )
                }"
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBodyList(TicketOrder::class.java)
            .hasSize(0)
    }

    @Test
    fun getTicketOrdersByOrderId() {
        /* Unauthorized user */
        webTestClient.get()
            .uri("orders/${addedOrders.first().orderId}/")
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus().isUnauthorized
            .expectBodyList(TicketOrder::class.java)
            .hasSize(0)

        /* Authorized a User with an order created by him */
        webTestClient.get()
            .uri("orders/${addedOrders.first().orderId}/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "R2D2",
                        setOf("CUSTOMER")
                    )
                }"
            )
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TicketOrder::class.java)
            .hasSize(1)
            .consumeWith<ListBodySpec<TicketOrder>> {
                val body = it.responseBody!!
                Assertions.assertEquals(body.first(), addedOrders[0])
            }

         /*Authorized User with an order NOT created by him */
        webTestClient.get()
            .uri("orders/${addedOrders.first().orderId}/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "R3D3",
                        setOf("CUSTOMER")
                    )
                }"
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBodyList(TicketOrder::class.java)
            .hasSize(0)

        /* Authorized User with an invalid Long id order */
        webTestClient.get()
            .uri("orders/-1/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "R2D2",
                        setOf("CUSTOMER")
                    )
                }"
            )
            .exchange()
            .expectStatus().isNotFound
            .expectBodyList(TicketOrder::class.java)
            .hasSize(0)

        /* Authorized User with an invalid "NOT Long" id order */
        webTestClient.get()
            .uri("orders/INVALID/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "R2D2",
                        setOf("CUSTOMER")
                    )
                }"
            )
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun getAllOrdersFromUsersAsAdmin() {
        /* Unauthorized user */
        webTestClient.get()
            .uri("admin/orders/")
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus().isUnauthorized
            .expectBodyList(TicketOrder::class.java)
            .hasSize(0)

        /* Customer tries to access admin API */
        webTestClient.get()
            .uri("admin/orders/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "R2D2",
                        setOf("CUSTOMER")
                    )
                }"
            )
            .exchange()
            .expectStatus().isForbidden
            .expectBodyList(TicketOrder::class.java)
            .hasSize(0)

        /* Valid request */
        webTestClient.get()
            .uri("admin/orders/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "BigBoss",
                        setOf("CUSTOMER","ADMIN")
                    )
                }"
            )
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TicketOrder::class.java)
            .hasSize(addedOrders.size)
            .consumeWith<ListBodySpec<TicketOrder>> {
                val body = it.responseBody!!
                body.forEach { item -> Assertions.assertEquals(item,addedOrders[addedOrders.indexOf(item)]) }
            }
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