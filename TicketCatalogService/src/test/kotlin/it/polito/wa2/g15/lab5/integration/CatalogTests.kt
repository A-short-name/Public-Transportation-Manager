package it.polito.wa2.g15.lab5.integration

import it.polito.wa2.g15.lab5.MyPostgresSQLContainer
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpHeaders
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


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

    @Autowired
    lateinit var ticketItemRepo: TicketItemRepository

    @Autowired
    lateinit var client: WebTestClient

    @BeforeEach
    fun initDb() = runBlocking {
        println("start init db ...")
        ticketItemRepo.save(
            TicketItem(
                null,
                "ORDINAL",
                20.0,
                0,
                200,
                2000L,
            )
        )

        println("... init db finished")
    }


    @AfterEach
    fun tearDownDb() = runBlocking {
        println("start tear down db...")
        ticketItemRepo.deleteAll()
        println("...end tear down db")
    }


    @Test
    fun validAdminTicketsAPI() {
        val newTicket = NewTicketItemDTO(25.0,"ORDINAL",0,200,2000)
        client.post()
            .uri("admin/tickets/")
            .bodyValue(newTicket)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header("X-XSRF-TOKEN","224159f4-d4ed-41ff-b726-c6d7a2ad71d6")
            .header("Cookie","XSRF-TOKEN=224159f4-d4ed-41ff-b726-c6d7a2ad71d6")
            .header(HttpHeaders.AUTHORIZATION,"Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJCaWdCb3NzIiwiaWF0IjoxNjUyNDU2MjQ5LCJleHAiOjE5MjQ5MDU2MDAsInJvbGVzIjpbIkFETUlOIl19.z8RClg7GgpT4e-OjihMJbflIWiDxzrdrYNhL2HteE_A")
            .exchange()
            .expectStatus().isOk
    }


}
