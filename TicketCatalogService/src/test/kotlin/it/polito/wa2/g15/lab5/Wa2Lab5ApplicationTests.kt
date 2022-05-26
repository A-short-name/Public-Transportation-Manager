package it.polito.wa2.g15.lab5

import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

//https://stackoverflow.com/questions/59007414/testcontainers-postgresqlcontainer-with-kotlin-unit-test-not-enough-informatio
class MyPostgresSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgresSQLContainer>(imageName)

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Wa2Lab5ApplicationTests {


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

    @Test
    fun contextLoads() {
    }


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
    fun myFirstTest() = runBlocking {


        println(
            "saved: " + ticketItemRepo.save(
                TicketItem(
                    null,
                    "ORDINAL",
                    21.0,
                    0,
                    200,
                    2000L,
                )
            )
        )
        println(
            "saved: " + ticketItemRepo.save(
                TicketItem(
                    null,
                    "ORDINAL",
                    25.0,
                    10,
                    200,
                    10L,
                )
            )
        )
        println(ticketItemRepo.findAll().collect {
            println(it)
        })
        Assertions.assertEquals(1, 1)
    }


}
