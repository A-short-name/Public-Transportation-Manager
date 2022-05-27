package it.polito.wa2.g15.lab4.integration.service

import it.polito.wa2.g15.lab4.entities.TicketPurchased
import it.polito.wa2.g15.lab4.entities.UserDetails
import it.polito.wa2.g15.lab4.integration.MyPostgresSQLContainer
import it.polito.wa2.g15.lab4.repositories.TicketPurchasedRepository
import it.polito.wa2.g15.lab4.repositories.UserDetailsRepository
import it.polito.wa2.g15.lab4.services.TravelerService
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
import java.time.*
import java.util.*


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceTest {

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

    final val t1iatLocalDateTime: LocalDateTime = LocalDateTime.of(2000, Month.DECEMBER, 25, 0, 0)
    final val t1expLocalDateTime: LocalDateTime = LocalDateTime.of(2000, Month.DECEMBER, 31, 0, 0)
    final val t1iat = Date(t1iatLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))
    final val t1exp = Date(t1expLocalDateTime.toEpochSecond(ZoneOffset.ofHours(0)))
    final val fakeJws = "fakeJws"
    final val t1Zid = "ABC"
    final val t1Type = "ORDINAL"
    final val t1ValidFrom = ZonedDateTime.now(ZoneId.of("UTC"))
    final val t1Duration = 300*60*1000L

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
            userRepo.save(r2d2User)
            userRepo.save(c3poUser)
            ticketPurchasedRepository.save(t1Expired)
            Assertions.assertEquals(1, ticketPurchasedRepository.count(), "ticket not saved in db")
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
    fun `admin visit get list of username`() {
        val userNames = travelerService.getListOfUsername()

        Assertions.assertEquals(2,userNames.count(),"users not found by admin")
        Assertions.assertTrue(userNames.contains(r2d2User.username),"not found r2d2")
        Assertions.assertTrue(userNames.contains(c3poUser.username),"not found c3po")

    }

    @Test
    @WithMockUser(username = "R2D2", authorities = ["CUSTOMER"] )
    fun `customer try to get all usernames`() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException::class.java){
            travelerService.getListOfUsername()
        }
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException::class.java){
            travelerService.getUserById(1L)
        }

    }
}