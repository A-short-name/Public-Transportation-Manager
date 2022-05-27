package it.polito.wa2.g15.lab5.integration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.lab5.MyPostgresSQLContainer
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.dtos.TicketItemDTO
import it.polito.wa2.g15.lab5.dtos.toDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import it.polito.wa2.g15.lab5.services.TicketCatalogService
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
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*
import javax.crypto.SecretKey


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogTests {

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
    lateinit var ticketCatalogService: TicketCatalogService

    @Autowired
    lateinit var ticketItemRepo: TicketItemRepository

    @Autowired
    lateinit var client: WebTestClient

    val tickets = listOf(
        TicketItem(
            null,
            "ORDINAL",
            20.0,
            0,
            200,
            2000L,
        )
    )

    val addedTickets = mutableListOf<TicketItem>()

    @BeforeEach
    fun initDb() = runBlocking {
        println("start init db ...")

        tickets.forEach{ addedTickets.add(ticketItemRepo.save(it)) }
        ticketCatalogService.initTicketCatalogCache()

        println("... init db finished")
    }


    @AfterEach
    fun tearDownDb() = runBlocking {
        println("start tear down db...")
        ticketItemRepo.deleteAll()
        addedTickets.clear()
        println("...end tear down db")
    }


    @Test
    fun validAdminTicketsAPI() {
        val newTicket = NewTicketItemDTO(25.0,"ORDINAL",0,200,2000L)
        client.post()
            .uri("admin/tickets/")
            .bodyValue(newTicket)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(HttpHeaders.AUTHORIZATION,"Bearer " + generateJwtToken("BigBoss", setOf("ADMIN","CUSTOMER")))
            .exchange()
            .expectStatus().isAccepted
            .expectBody(Long::class.java)
            .consumeWith {
                Assertions.assertEquals(2L,it.responseBody!!)
            }
    }

    @Test
    fun invalidAdminTicketsAPI() {
        val invalidTickets = listOf(
            mapOf("price" to null,"type" to "ORDINAL","minAge" to 0, "maxAge" to 200,"duration" to 2000L),
            mapOf("price" to 0,"type" to "ORDINAL","minAge" to 0, "maxAge" to 200,"duration" to 2000L),
            mapOf("price" to "x","type" to "ORDINAL","minAge" to 0, "maxAge" to 200,"duration" to 2000L),
            
            mapOf("price" to 1,"type" to "","minAge" to 0, "maxAge" to 200,"duration" to 2000L),
            mapOf("price" to 1,"type" to null,"minAge" to 0, "maxAge" to 200,"duration" to 2000L),
            
            mapOf("price" to 1,"type" to "ORDINAL","minAge" to -1, "maxAge" to 200,"duration" to 2000L),
            mapOf("price" to 1,"type" to "ORDINAL","minAge" to "x", "maxAge" to 200,"duration" to 2000L),
            
            mapOf("price" to 1,"type" to "ORDINAL","minAge" to 0, "maxAge" to -1,"duration" to 2000L),
            mapOf("price" to 1,"type" to "ORDINAL","minAge" to 0, "maxAge" to "x","duration" to 2000L),
            
            mapOf("price" to 1,"type" to "ORDINAL","minAge" to 0, "maxAge" to 200,"duration" to -1),
            mapOf("price" to 1,"type" to "ORDINAL","minAge" to 0, "maxAge" to 200,"duration" to "2000L"),
            mapOf("price" to 1,"type" to "ORDINAL","minAge" to 0, "maxAge" to 200,"duration" to null)
        )
    
//        {
//            "type": "Settimanale",
//            "price": Error
//        }
        invalidTickets.forEach {
            client.post()
                .uri("admin/tickets/")
                .bodyValue(it)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwtToken("BigBoss", setOf("ADMIN", "CUSTOMER")))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Test
    fun unauthorizedAdminTicketsAPI() {
        val newTicket = NewTicketItemDTO(25.0,"ORDINAL",0,200,2000)
        client.post()
            .uri("admin/tickets/")
            .bodyValue(newTicket)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(HttpHeaders.AUTHORIZATION,"Bearer InvalidToken")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun forbiddenAdminTicketsAPI() {
        val newTicket = NewTicketItemDTO(25.0,"ORDINAL",0,200,2000)
        client.post()
                .uri("admin/tickets/")
                .bodyValue(newTicket)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.AUTHORIZATION,"Bearer "+generateJwtToken("Giovanni", setOf("CUSTOMER")))
                .exchange()
                .expectStatus().isForbidden
    }

    @Test
    fun getAvailableTicketsAPI() {
        // TODO fix this
        client.get()
            .uri("tickets/")
            .accept(MediaType.APPLICATION_NDJSON)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            //.header(HttpHeaders.AUTHORIZATION,"Bearer " + generateJwtToken("Giovanni", setOf("CUSTOMER")))
            .exchange()
            .expectStatus().isOk
            .expectBody(TicketItemDTO::class.java)
            .consumeWith {
                val body = it.responseBody!!
                //Assertions.assertNotEquals(body,TicketItem(2,"ORR",25.0,30,30,-1).toDTO())
                Assertions.assertEquals(body,addedTickets[0].toDTO())
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
