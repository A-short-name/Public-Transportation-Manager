package it.polito.wa2.g15.lab5.integration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.lab5.MyPostgresSQLContainer
import it.polito.wa2.g15.lab5.dtos.BuyTicketDTO
import it.polito.wa2.g15.lab5.dtos.PaymentInfo
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import it.polito.wa2.g15.lab5.repositories.TicketOrderRepository
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpHeaders
import org.apache.kafka.clients.admin.AdminClient
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ListBodySpec
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
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

        @Container
        var kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1")).apply {
            this.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                ("r2dbc:postgresql://" + postgres.host + ":" + postgres.firstMappedPort + "/" + postgres.databaseName)
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.kafka.bootstrap-servers",kafka::getBootstrapServers)
        }
    }

    @Autowired
    private lateinit var admin: KafkaAdmin

    @Value("\${security.jwtExpirationMs}")
    private lateinit var jwtExpirationMs: String

    @Value("\${security.privateKey.common}")
    private lateinit var validateJwtStringKey: String

    @Autowired
    lateinit var ticketItemRepo: TicketItemRepository

    @Autowired
    lateinit var ticketOrderRepository: TicketOrderRepository

    @Autowired
    lateinit var webTestClient: WebTestClient

    lateinit var webTravelerClient: WebClient

    val tickets = listOf(
        TicketItem(
            null,
            "ORDINAL",
            1.5,
            10,
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
            ZonedDateTime.now(ZoneId.of("UTC")),
            "1"
        )
    )

    val addedTickets: MutableList<TicketItem> = mutableListOf()
    val addedOrders: MutableList<TicketOrder> = mutableListOf()

    @BeforeEach
    fun initDb() = runBlocking {
        println("start init db ...")

        tickets.forEach { addedTickets.add(ticketItemRepo.save(it)) }

        orders.forEach { addedOrders.add(ticketOrderRepository.save(it)) }

        println("... init db finished")

        val topicName = "catalogToPayment"
        val topic1 = TopicBuilder.name(topicName).build()
        admin.createOrModifyTopics(topic1)
    }

    @AfterEach
    fun tearDownDB() = runBlocking {
        println("start tear down db...")
        ticketItemRepo.deleteAll()
        ticketOrderRepository.deleteAll()
        addedOrders.clear()
        addedTickets.clear()
        println("...end tear down db")
    }

    @Test
    fun testCreationOfTopicAtStartup() {
        val client = AdminClient.create(admin.configurationProperties)
        val topicList = client.listTopics().listings().get()
        Assertions.assertNotNull(topicList)
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
            ZonedDateTime.now(ZoneId.of("UTC")),
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
                        setOf("CUSTOMER", "ADMIN")
                    )
                }"
            )
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TicketOrder::class.java)
            .hasSize(addedOrders.size)
            .consumeWith<ListBodySpec<TicketOrder>> {
                val body = it.responseBody!!
                body.forEach { item -> Assertions.assertEquals(addedOrders[0], item) }
            }
    }

    @Test
    fun getAllOrdersFromSpecificUserAsAdmin() {
        /* Unauthorized user */
        webTestClient.get()
            .uri("admin/orders/R2D2/")
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus().isUnauthorized
            .expectBodyList(TicketOrder::class.java)
            .hasSize(0)

        /* Customer tries to access admin API */
        webTestClient.get()
            .uri("admin/orders/R2D2/")
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

        val newOrder = TicketOrder(
            null,
            "PENDING",
            10.0,
            "Giovanni",
            2,
            2,
            ZonedDateTime.now(ZoneId.of("UTC")),
            "1"
        )

        runBlocking { addedOrders.add(ticketOrderRepository.save(newOrder)) }
        /* 1 order of Giovanni and 1 order of r2d2 */

        /* Valid request: only gets r2d2 order */
        webTestClient.get()
            .uri("admin/orders/R2D2/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "BigBoss",
                        setOf("CUSTOMER", "ADMIN")
                    )
                }"
            )
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TicketOrder::class.java)
            .hasSize(1)
            .consumeWith<ListBodySpec<TicketOrder>> {
                val body = it.responseBody!!
                body.forEach { item -> Assertions.assertEquals(addedOrders[0], item) }
            }
    }

    @Test
    fun shopTickets() {
        val exp: LocalDate = LocalDate.now().plusYears(2)
        val paymentInfo = PaymentInfo(
            "7992-7398-713",
            exp,
            "322",
            "BigBoss"
        )

        val zonedDateTime = ZonedDateTime.now()
        val newBuyTicket = BuyTicketDTO(3, paymentInfo, zonedDateTime, "1")
        val wrongBuyTicket =
            mapOf("numOfTickets" to "ERROR", "paymentInfo" to paymentInfo, "zonedDateTime" to zonedDateTime, "zid" to 3)

        /* Unauthorized user */
        webTestClient.post()
            .uri("shop/${addedTickets.first().id}/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(newBuyTicket)
            .exchange()
            .expectStatus().isUnauthorized

        /* Authorized user with wrong input */
        webTestClient.post()
            .uri("shop/${addedTickets.first().id}/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(wrongBuyTicket)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "BigBoss",
                        setOf("CUSTOMER", "ADMIN")
                    )
                }"
            )
            .exchange()
            .expectStatus().isBadRequest

        /* Authorized user with wrong ID */
        webTestClient.post()
            .uri("shop/-1/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(newBuyTicket)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "BigBoss",
                        setOf("CUSTOMER", "ADMIN")
                    )
                }"
            )
            .exchange()
            .expectStatus().isBadRequest

        webTestClient.post()
            .uri("shop/ERROR/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(newBuyTicket)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "BigBoss",
                        setOf("CUSTOMER", "ADMIN")
                    )
                }"
            )
            .exchange()
            .expectStatus().isBadRequest

        webTravelerClient = WebClient
            .builder()
            .baseUrl("http://localhost:8081")
            .defaultHeaders { headers ->
                headers.contentType = MediaType.APPLICATION_JSON
                headers.setBearerAuth(
                    generateJwtToken(
                        "BigBoss",
                        setOf("ADMIN")
                    )
                )
                headers.set(org.springframework.http.HttpHeaders.ACCEPT_ENCODING, MediaType.APPLICATION_JSON_VALUE)
                headers.set("Cookie", "XSRF-TOKEN=224159f4-d4ed-41ff-b726-c6d7a2ad71d6")
                headers.set("X-XSRF-TOKEN", "224159f4-d4ed-41ff-b726-c6d7a2ad71d6")
            }
            .defaultUriVariables(Collections.singletonMap("url", "http://localhost:8081"))
            .build()

        val userBigBoss = UserProfileDTO(
            "BigBoss",
            "via 123",
            LocalDate.of(1980, Month.NOVEMBER, 10),
            "333232323"
        )
        val userIsInserted = runBlocking {
            try {
                var tmpOK = true
                webTravelerClient.put()
                    .uri("/my/profile/")
                    .body(BodyInserters.fromValue(userBigBoss))
                    .awaitExchange {
                        println("insert new user in traveler service and got : ${it.statusCode()}")
                        if (it.statusCode() != HttpStatus.OK) {
                            println("could not insert new user in traveler service : ${it.statusCode()}")
                             tmpOK = false
                        }
                    }
                tmpOK
            }catch(e: Exception){
                false
            }
        }
        Assumptions.assumeTrue(userIsInserted,"user not inserted in traveler service")
        /* Authorized user with valid input */

        var createdOrderId: Long? = null
        //mockare catalogService.getTravelerAge in modo che non venga chiamata
        //e che restituisca l'età dell'utente
        //In un test sarà un'età compatibile, facendo andare avanti il metodo
        //In un test sarà un'età incompatibile con il biglietto -> eccezione -> bad request
        webTestClient.post()
            .uri("shop/${addedTickets.first().id}/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(newBuyTicket)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "BigBoss",
                        setOf("CUSTOMER", "ADMIN")
                    )
                }"
            )
            .exchange()
            .expectStatus().isAccepted
            .expectBody(Long::class.java)
            .consumeWith {
                createdOrderId = it.responseBody!!
            }

        Assertions.assertNotNull(createdOrderId)

        webTestClient.get()
            .uri("orders/$createdOrderId/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(
                HttpHeaders.AUTHORIZATION, "Bearer ${
                    generateJwtToken(
                        "BigBoss",
                        setOf("CUSTOMER", "ADMIN")
                    )
                }"
            )
            .exchange()
            .expectStatus().isOk
            .expectBodyList(TicketOrder::class.java)
            .hasSize(1)
            .consumeWith<ListBodySpec<TicketOrder>> {
                val body = it.responseBody!!
                Assertions.assertEquals("PENDING", body.first().orderState)
                Assertions.assertEquals(body.first().orderId, createdOrderId)
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