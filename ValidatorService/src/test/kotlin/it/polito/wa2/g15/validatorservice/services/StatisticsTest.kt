package it.polito.wa2.g15.validatorservice.services

import it.polito.wa2.g15.validatorservice.MyPostgresSQLContainer
import it.polito.wa2.g15.validatorservice.dtos.FilterDto
import it.polito.wa2.g15.validatorservice.entities.TicketValidation
import it.polito.wa2.g15.validatorservice.repositories.TicketValidationRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.Month


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketValidationServiceStatisticsTest {

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

    @Autowired
    lateinit var repo: TicketValidationRepository

    @Autowired
    lateinit var service: ValidationService

    @BeforeEach
    fun initDb() {
        var time = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            1,
            0,
            0
        )
        repo.save(
            TicketValidation(
                username = "R2D2",
                validationTime = time,
                ticketId = 1
            )
        )

        var time2 = LocalDateTime.of(
            2020,
            Month.NOVEMBER.value,
            20,
            10,
            0
        )
        repo.save(
            TicketValidation(
                username = "R2D2",
                validationTime = time2,
                ticketId = 2
            )
        )

        var time3 = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            30,
            20,
            45
        )
        repo.save(
            TicketValidation(
                username = "C3PO",
                validationTime = time3,
                ticketId = 3
            )
        )
    }

    @AfterEach
    fun tearDownDb() {
        repo.deleteAll()
    }

    @Test
    fun `should find all validated tickets`() {
        var res = repo.findAll()
        var noFilter = FilterDto(
            timeStart = null,
            timeEnd = null,
            nickname = null
        )
        Assertions.assertEquals(
            res.toList().size,
            service.getStats(noFilter.timeStart, noFilter.timeEnd, noFilter.nickname).size,
            "it should return all the validations in the db without filters"
        )

    }

    @Test
    fun `should find validated tickets in december 2020`() {
        var startTime = LocalDateTime.of(
            2020,
            Month.NOVEMBER.value,
            30,
            23,
            59
        )
        var endTime = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            31,
            23,
            59
        )
        var dateFilter = FilterDto(
            timeStart = startTime,
            timeEnd = endTime,
            nickname = null
        )
        var actualRes = service.getStats(dateFilter.timeStart, dateFilter.timeEnd, dateFilter.nickname)
        Assertions.assertEquals(2, actualRes.size, "it should find validations in december")
        Assertions.assertTrue(
            actualRes.stream().map { it.validationTime }.allMatch { it.isBefore(endTime) && it.isAfter(startTime) },
            "some dates are out of range: $actualRes"
        )
    }

    @Test
    fun `should find validated tickets of a specific user`() {
        var userFilter = FilterDto(
            timeStart = null,
            timeEnd = null,
            nickname = "R2D2"
        )
        var actualRes = service.getStats(userFilter.timeStart, userFilter.timeEnd, userFilter.nickname)
        Assertions.assertEquals(2, actualRes.size, "it should find ticket validations of C3PO")
        Assertions.assertTrue(
            actualRes.stream().map { it.username }.allMatch { it.equals("R2D2") },
            "some validations are from another user: $actualRes"
        )
    }

    @Test
    fun `should find validated tickets in december 2020 of R2D2`() {
        var startTime = LocalDateTime.of(
            2020,
            Month.NOVEMBER.value,
            30,
            23,
            59
        )
        var endTime = LocalDateTime.of(
            2020,
            Month.DECEMBER.value,
            31,
            23,
            59
        )
        var dateUserFilter = FilterDto(
            timeStart = startTime,
            timeEnd = endTime,
            nickname = "R2D2"
        )
        var actualRes = service.getStats(dateUserFilter.timeStart, dateUserFilter.timeEnd, dateUserFilter.nickname)
        Assertions.assertEquals(1, actualRes.size, "it should find validations in december")
        Assertions.assertTrue(
            actualRes.stream().allMatch {
                it.validationTime.isBefore(endTime) && it.validationTime.isAfter(startTime) && it.username.equals("R2D2")
            },
            "some dates are out of range or user is not R2D2: $actualRes"
        )
    }
}
