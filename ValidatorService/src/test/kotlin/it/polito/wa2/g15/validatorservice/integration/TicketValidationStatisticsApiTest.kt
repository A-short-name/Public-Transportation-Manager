package it.polito.wa2.g15.validatorservice.integration

import it.polito.wa2.g15.validatorservice.MyPostgresSQLContainer
import it.polito.wa2.g15.validatorservice.dtos.FilterDto
import it.polito.wa2.g15.validatorservice.dtos.StatisticDto
import it.polito.wa2.g15.validatorservice.entities.TicketValidation
import it.polito.wa2.g15.validatorservice.repositories.TicketValidationRepository
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.Month


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketValidationStatisticsApiTest {

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

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

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
    fun unauthorizedAdminTicketsAPI() {
        var noFilter = FilterDto(
            timeStart = null,
            timeEnd = null,
            nickname = null
        )
        fail("TODO: Test security access")
    }

    @Test
    fun `should find all validated tickets`() {
        var res = repo.findAll()
        var noFilter = FilterDto(
            timeStart = null,
            timeEnd = null,
            nickname = null
        )
        //TODO: use security
//        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
//        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validR2D2Token")

        var request = HttpEntity(
            noFilter
        )
        val response = restTemplate.postForEntity<StatisticDto>(
            "http://localhost:$port/get/stats",
            request
        )
        val actualRes = response.body!!

        Assertions.assertEquals(HttpStatus.ACCEPTED, response.statusCode, "status code ok")
        Assertions.assertEquals(
            res.toList().size,
            actualRes.validations.size,
            "it should return all the validations in the db without filters"
        )


    }

    @Test
    fun `should find validated tickets of a specific user`() {
        var userFilter = FilterDto(
            timeStart = null,
            timeEnd = null,
            nickname = "R2D2"
        )
        //TODO: use security
//        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
//        requestHeader.add(jwtSecurityHeader, "$jwtTokenPrefix $validR2D2Token")

        var request = HttpEntity(
            userFilter
        )
        val response = restTemplate.postForEntity<StatisticDto>(
            "http://localhost:$port/get/stats",
            request
        )
        val actualRes = response.body!!.validations
        Assertions.assertEquals(2, actualRes.size, "it should find ticket validations of C3PO")
        Assertions.assertTrue(
            actualRes.stream().map { it.username }.allMatch { it.equals("R2D2") },
            "some validations are from another user: $actualRes"
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

        var request = HttpEntity(
            dateFilter
        )
        val response = restTemplate.postForEntity<StatisticDto>(
            "http://localhost:$port/get/stats",
            request
        )
        val actualRes = response.body!!.validations
        Assertions.assertEquals(2, actualRes.size, "it should find validations in december")
        Assertions.assertTrue(
            actualRes.stream().map { it.validationTime }
                .allMatch { it.isBefore(endTime) && it.isAfter(startTime) },
            "some dates are out of range: $actualRes"
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

        var request = HttpEntity(
            dateUserFilter
        )
        val response = restTemplate.postForEntity<StatisticDto>(
            "http://localhost:$port/get/stats",
            request
        )
        val actualRes = response.body!!.validations
        Assertions.assertEquals(1, actualRes.size, "it should find validations in december")
        Assertions.assertTrue(
            actualRes.stream().allMatch {
                it.validationTime.isBefore(endTime) && it.validationTime.isAfter(startTime) && it.username.equals("R2D2")
            },
            "some dates are out of range or user is not R2D2: $actualRes"
        )

    }
}