package it.polito.wa2.g15.lab4.integration.service

import it.polito.wa2.g15.lab4.dtos.FilterDto
import it.polito.wa2.g15.lab4.entities.TicketPurchased
import it.polito.wa2.g15.lab4.entities.UserDetails
import it.polito.wa2.g15.lab4.exceptions.TravelerException
import it.polito.wa2.g15.lab4.integration.MyPostgresSQLContainer
import it.polito.wa2.g15.lab4.repositories.TicketPurchasedRepository
import it.polito.wa2.g15.lab4.repositories.UserDetailsRepository
import it.polito.wa2.g15.lab4.services.TravelerService
import org.junit.Ignore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Timestamp
import java.time.*
import java.util.*


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StatisticsTest {

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
    private lateinit var travelerService : TravelerService

    @Autowired
    lateinit var userRepo: UserDetailsRepository

    @Autowired
    lateinit var ticketPurchasedRepository: TicketPurchasedRepository

    final val localBirthDateOfDroids: LocalDate = LocalDate.of(1980, Month.NOVEMBER, 10)

    final val c3poUser = UserDetails(
        "C3PO",
        "C3PO",
        "Tatooine",
        localBirthDateOfDroids,
        "3334593945",
        mutableSetOf()
    )
    val r2d2User = UserDetails(
        "R2D2",
        "R2D2",
        "Tatooine",
        localBirthDateOfDroids,
        "3314593945",
        mutableSetOf()
    )

    final val t1 = LocalDateTime.of(2000, Month.DECEMBER, 25, 0, 0)
    final val t2 = LocalDateTime.of(2000, Month.DECEMBER, 31, 0, 0)
    final val t3 = LocalDateTime.of(2001, Month.JANUARY, 10, 0, 0)

    final val timeBetweent1Andt2 = LocalDateTime.of(2000, Month.DECEMBER, 28, 0, 0)
    final val timeBeforet1 = LocalDateTime.of(2000, Month.OCTOBER, 25, 0, 0)
    final val timeAftert2 = LocalDateTime.of(2001, Month.JANUARY, 8, 0, 0)

    final val t1iatLocalDateTime: LocalDateTime = t1
    final val t1expLocalDateTime: LocalDateTime = t2
/*    final val t1iat = Date(t1iatLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))
    final val t1exp = Date(t1expLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))*/
    final val t1iat = Date.from(t1iatLocalDateTime.toInstant(ZoneOffset.ofHours(0)))
    final val t1exp = Date.from(t1expLocalDateTime.toInstant(ZoneOffset.ofHours(0)))
    final val fakeJws = "fakeJws"
    final val t1Zid = "ABC"
    final val t1Type = "ORDINAL"
    final val t1ValidFrom = ZonedDateTime.now(ZoneId.of("UTC"))
    final val t1Duration = 300*60*1000L

    final val t2iatLocalDateTime: LocalDateTime = t2
    final val t2expLocalDateTime: LocalDateTime = t3
    final val t2iat = Date.from(t2iatLocalDateTime.toInstant(ZoneOffset.ofHours(0)))
    final val t2exp = Date.from(t2expLocalDateTime.toInstant(ZoneOffset.ofHours(0)))
    final val t2Zid = "ABC"
    final val t2Type = "ORDINAL"
    final val t2ValidFrom = ZonedDateTime.now(ZoneId.of("UTC"))
    final val t2Duration = 300*60*1000L

    val t1Expired = TicketPurchased(
        t1iat,
        t1exp,
        t1Zid,
        fakeJws,
        c3poUser,
        t1Type,
        t1ValidFrom,
        t1Duration
    )

    val t2Expired = TicketPurchased(
        t2iat,
        t2exp,
        t2Zid,
        fakeJws,
        c3poUser,
        t2Type,
        t2ValidFrom,
        t2Duration
    )

    @BeforeEach
    fun initDb() {

        val r2d2User = UserDetails(
            "R2D2",
            "R2D2",
            "Tatooine",
            localBirthDateOfDroids,
            "3314593945",
            mutableSetOf()
        )

        if (userRepo.count() == 0L) {
            c3poUser.addTicketPurchased(t1Expired)
            c3poUser.addTicketPurchased(t2Expired)
            userRepo.save(r2d2User)
            userRepo.save(c3poUser)
            ticketPurchasedRepository.save(t1Expired)
            ticketPurchasedRepository.save(t2Expired)
            Assertions.assertEquals(2, ticketPurchasedRepository.count(), "ticket not saved in db")
            Assertions.assertEquals(2, userRepo.count(), "user not saved in db")
        }
    }

    @AfterEach
    fun tearDownDb() {
        ticketPurchasedRepository.deleteAll()
        userRepo.deleteAll()
    }

    @Test
    @WithMockUser(username = "JOHN", authorities = ["ADMIN"] )
    fun `admin get global stats`() {
        val gloabalStatsInfo = travelerService.getStats(null, null, null)
        Assertions.assertEquals(2,gloabalStatsInfo.count(),"ticket purchased info not found")
    }

    @Test
    @WithMockUser(username = "JOHN", authorities = ["ADMIN"] )
    fun `admin get stats for single user`() {
        val r2d2nickname = r2d2User.username
        Assertions.assertNotNull(r2d2nickname)

        val userR2d2Filter = FilterDto(
            timeStart = null,
            timeEnd = null,
            nickname = r2d2nickname
        )

        val c3ponickname = c3poUser.username
        Assertions.assertNotNull(c3ponickname)
        val userC3poFilter = FilterDto(
            timeStart = null,
            timeEnd = null,
            nickname = c3ponickname
        )

        val singleUserC3poStatsInfo = travelerService.getStats(userR2d2Filter.timeStart, userR2d2Filter.timeEnd, userR2d2Filter.nickname)
        val singleUserR2d2StatsInfo = travelerService.getStats(userC3poFilter.timeStart, userC3poFilter.timeEnd, userC3poFilter.nickname)

        Assertions.assertEquals(0,singleUserC3poStatsInfo.count(),"ticket purchased info not found")
        Assertions.assertEquals(2,singleUserR2d2StatsInfo.count(),"ticket purchased info not found")
    }

    @Test
    @WithMockUser(username = "JOHN", authorities = ["ADMIN"] )
    fun `admin get range date stats for user`() {
        val c3ponickname = c3poUser.username
        Assertions.assertNotNull(c3ponickname)
        val dateFilterBetween2Tickets = FilterDto(
            timeStart = timeBeforet1,
            timeEnd = timeBetweent1Andt2,
            nickname = c3ponickname
        )
        val globalTRStatsInfo2 = travelerService.getStats(dateFilterBetween2Tickets.timeStart, dateFilterBetween2Tickets.timeEnd, dateFilterBetween2Tickets.nickname)
        Assertions.assertEquals(1,globalTRStatsInfo2.count(),"ticket purchased between t1 and t2 not found")
        Assertions.assertTrue(
            globalTRStatsInfo2.map { it.iat }.stream()
                .allMatch { it.before(Date.from(timeBetweent1Andt2.toInstant(ZoneOffset.ofHours(0))))
                        && it.after(Date.from(timeBeforet1.toInstant(ZoneOffset.ofHours(0)))) },
            "some dates are out of range"
        )
    }

    @Test
    @WithMockUser(username = "JOHN", authorities = ["ADMIN"] )
    fun `admin get global range date stats`() {
        val dateFilterBetween2Tickets = FilterDto(
            timeStart = timeBeforet1,
            timeEnd = timeBetweent1Andt2,
            nickname = null
        )
        val globalTRStatsInfo2 = travelerService.getStats(dateFilterBetween2Tickets.timeStart, dateFilterBetween2Tickets.timeEnd, dateFilterBetween2Tickets.nickname)
        Assertions.assertEquals(1,globalTRStatsInfo2.count(),"ticket purchased between t1 and t2 not found")
        Assertions.assertTrue(
            globalTRStatsInfo2.map { it.iat }.stream()
                .allMatch { it.before(Date.from(timeBetweent1Andt2.toInstant(ZoneOffset.ofHours(0))))
                        && it.after(Date.from(timeBeforet1.toInstant(ZoneOffset.ofHours(0)))) },
            "some dates are out of range"
        )
    }


    @Test
    /**
     * For the moment the logic doesn't distinguish the absence of one single temporal constraint
     * i.e. no repo methods findBefore/findAfter are used
     */
    @Ignore
    @WithMockUser(username = "JOHN", authorities = ["ADMIN"] )
    fun `admin get after or before date stats`() {
        val dateFilterBeforeT1 = FilterDto(
            timeStart = timeBeforet1,
            timeEnd = null,
            nickname = null
        )
        val globalTRStatsInfo1 = travelerService.getStats(dateFilterBeforeT1.timeStart, dateFilterBeforeT1.timeEnd, dateFilterBeforeT1.nickname)
        Assertions.assertEquals(2,globalTRStatsInfo1.count(),"ticket purchased after t1 and unlimited end not found")

        val dateFilterAfter2Tickets = FilterDto(
            timeStart = null,
            timeEnd = timeAftert2,
            nickname = null
        )
        val globalTRStatsInfo3 = travelerService.getStats(dateFilterAfter2Tickets.timeStart, dateFilterAfter2Tickets.timeEnd, dateFilterAfter2Tickets.nickname)
        Assertions.assertEquals(2,globalTRStatsInfo3.count(),"ticket purchased before t2 unlimited begin not found")
    }


    @Test
    @WithMockUser(username = "JOHN", authorities = ["ADMIN"] )
    fun `admin try to get stats for not existing user`() {
        val notExistingUserFilter = FilterDto(
            timeStart = null,
            timeEnd = null,
            nickname = "darthMaul"
        )
        Assertions.assertThrows(TravelerException::class.java,
            { travelerService.getStats(notExistingUserFilter.timeStart,notExistingUserFilter.timeEnd,notExistingUserFilter.nickname) },
            "user not found exception not thrown")
    }

    @Test
    @WithMockUser(username = "R2D2", authorities = ["CUSTOMER"] )
    fun `customer try to get stats`() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException::class.java){
            travelerService.getStats(null, null, null)
        }
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException::class.java){
            travelerService.getStats(null, null, null)
        }

    }
}