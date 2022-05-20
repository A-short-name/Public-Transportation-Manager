package it.polito.wa2.g15.lab3.integration.controller

import it.polito.wa2.g15.lab3.SecurityConfiguration
import it.polito.wa2.g15.lab3.dtos.ActivationRequestDTO
import it.polito.wa2.g15.lab3.entities.Activation
import it.polito.wa2.g15.lab3.entities.ERole
import it.polito.wa2.g15.lab3.entities.Role
import it.polito.wa2.g15.lab3.entities.User
import it.polito.wa2.g15.lab3.repositories.ActivationRepository
import it.polito.wa2.g15.lab3.repositories.RoleRepository
import it.polito.wa2.g15.lab3.repositories.UserRepository
import org.junit.Before
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@Testcontainers
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
class ControllerActivationUserTest {
    companion object {
        @Container
        val postgres = MyPostgreSQLContainer("postgres:latest")
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") {"create-drop"}
        }
    }

    @LocalServerPort
    protected var port: Int = 0
    @Autowired
    lateinit var restTemplate: TestRestTemplate
    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var roleRepository: RoleRepository
    @Autowired
    lateinit var activationRepository: ActivationRepository
    @Autowired
    lateinit var csrfTokenRepository: CsrfTokenRepository
    @Autowired
    lateinit var securityConfig: SecurityConfiguration
    
    val activationCodeC3PO = "123456"
    lateinit var okDeadlineC3PO : Calendar
    val userC3PO = User().apply {
        this.nickname = "C3PO"
        this.password = "BipBop42"
        this.email = "drone.galaxy@mail.com"
        this.active = false
    }
    lateinit var provisionalIdC3PO : UUID
    lateinit var users : MutableList<User>
    lateinit var activations: MutableList<Activation>

    @BeforeEach
    fun populateDb(){
        if (roleRepository.count() == 0L) {
            val adminRole = Role().apply {
                this.name = ERole.ADMIN
            }
            roleRepository.save(adminRole)
            val customerRole = Role().apply {
                this.name = ERole.CUSTOMER
            }
            roleRepository.save(customerRole)
        }
        if(userRepository.count() == 0L) {

            okDeadlineC3PO  = Calendar.getInstance()
            okDeadlineC3PO.add(Calendar.HOUR_OF_DAY, 12)
            userRepository.save(userC3PO)

            val savedActivation = activationRepository.save(Activation().apply{
                this.user = userC3PO
                this.attempts = 5
                this.activationCode = activationCodeC3PO
                this.activationDeadline = okDeadlineC3PO.time
            })
            provisionalIdC3PO = savedActivation.provisionalId!!
            users = userRepository.findAll().toMutableList()
            activations = activationRepository.findAll().toMutableList()

        }
    }

    @AfterEach
    fun teardownDb(){
        if(userRepository.count() > 0) {
            activationRepository.deleteAll()
            userRepository.deleteAll()
            users.clear()
            activations.clear()
        }
    }
    
    @Test
    fun validateUserTest() {
        val request = HttpEntity(ActivationRequestDTO(provisional_id = provisionalIdC3PO,activation_code = activationCodeC3PO),securityConfig.generateCsrfHeader(csrfTokenRepository))
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate",request
        )
        Assertions.assertEquals(HttpStatus.CREATED,response.statusCode,"userC3PO validation failed")

    }

    @Test
    fun multipleValidationUserTest() {
        val request = HttpEntity(ActivationRequestDTO(provisional_id = provisionalIdC3PO,activation_code = activationCodeC3PO),securityConfig.generateCsrfHeader(csrfTokenRepository))
        Assertions.assertTrue(activationRepository.findActivationByProvisionalId(provisionalIdC3PO).isPresent,
        "the expected activation entry in the db is not present")

        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate",request
        )
        Assertions.assertTrue(activationRepository.findActivationByProvisionalId(provisionalIdC3PO).isEmpty,
        "the activation entry in the db has not been deleted")
        Assertions.assertEquals(HttpStatus.CREATED,response.statusCode,"userC3PO validation failed")
        val response2 = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate",request
        )
        Assertions.assertTrue(activationRepository.findActivationByProvisionalId(provisionalIdC3PO).isEmpty)

        Assertions.assertEquals(HttpStatus.NOT_FOUND,response2.statusCode,"userC3PO validation failed")


    }
    @Test
    fun `multiple validation and one last ok validation`() {
        `trying max attempts - 1 validations`()

        val request = HttpEntity(ActivationRequestDTO(provisional_id = provisionalIdC3PO,activation_code = activationCodeC3PO),securityConfig.generateCsrfHeader(csrfTokenRepository))
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate",request
        )
        Assertions.assertEquals(HttpStatus.CREATED,response.statusCode,"userC3PO validation failed")

    }
    
    @Test
    fun `too many wrong validations`() {
        `trying max attempts - 1 validations`()

        Assertions.assertFalse(activationRepository.findActivationByProvisionalId(provisionalIdC3PO).isEmpty,
        "the activation entry is not present on the db before")
        
        Assertions.assertFalse(userRepository.findByNickname(userC3PO.nickname).isEmpty,
                "the user entry is not present on the db before")
        
        val request = HttpEntity(ActivationRequestDTO(provisional_id = provisionalIdC3PO,activation_code = "999999"),securityConfig.generateCsrfHeader(csrfTokenRepository))
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate",request
        )
        Assertions.assertEquals(HttpStatus.NOT_FOUND,response.statusCode,"userC3PO validation failed")
        
        Assertions.assertTrue(activationRepository.findActivationByProvisionalId(provisionalIdC3PO).isEmpty,
        "the activation entry in the db has not been deleted after receiving 5 wrong validation request")
        
        Assertions.assertTrue(userRepository.findByNickname(userC3PO.nickname).isEmpty,
                "user was not deleted after receiving 5 wrong validation request")

    }

    private fun `trying max attempts - 1 validations`() {
        var attemptN = 0
        while (attemptN < Activation.MAX_ATTEMPTS-1) {
            val tmpActivationCode: String = if (String.format("%06d", attemptN) == activationCodeC3PO)
                String.format("%06d", attemptN - 1)
            else
                String.format("%06d", attemptN)

            Assertions.assertEquals(
                Activation.MAX_ATTEMPTS - attemptN,
                activationRepository.findActivationByProvisionalId(provisionalIdC3PO).get().attempts,
                "num of attempts has not been decreased"
            )
            val request = HttpEntity(
                ActivationRequestDTO(
                    provisional_id = provisionalIdC3PO,
                    activation_code = tmpActivationCode
                )
                ,securityConfig.generateCsrfHeader(csrfTokenRepository))
            val response = restTemplate.postForEntity<Unit>(
                "http://localhost:$port/user/validate", request
            )
            Assertions.assertEquals(HttpStatus.NOT_FOUND, response.statusCode, "userC3PO validation failed")
            attemptN++
        }
    }

    @Test
    fun shortActivationCodeUserTest() {
        val shortActivationCode = "12"
        val request = HttpEntity(ActivationRequestDTO(provisional_id = provisionalIdC3PO,activation_code = shortActivationCode),securityConfig.generateCsrfHeader(csrfTokenRepository))
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate",request
        )
        Assertions.assertEquals(HttpStatus.NOT_FOUND,response.statusCode,"userC3PO validation failed")
        Assertions.assertEquals(
            5,
            activationRepository.findActivationByProvisionalId(provisionalIdC3PO).get().attempts,
            "num of attempts has been decreased even if the format of the activation is wrong"
        )
    }
    @Test
    fun wrongActivationCodeUserTest() {
        val shortActivationCode = "999999"
        val request = HttpEntity(ActivationRequestDTO(provisional_id = provisionalIdC3PO,activation_code = shortActivationCode),securityConfig.generateCsrfHeader(csrfTokenRepository))
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate",request
        )
        Assertions.assertEquals(HttpStatus.NOT_FOUND,response.statusCode,"userC3PO validation failed")
        Assertions.assertEquals(
            4,
            activationRepository.findActivationByProvisionalId(provisionalIdC3PO).get().attempts,
            "num of attempts has not been decreased"
        )
    }
    @Test
    fun wrongProvisionalIdUserTest() {
        val wrongProvisionalId = UUID.randomUUID()
        val request = HttpEntity(ActivationRequestDTO(provisional_id = wrongProvisionalId,activation_code = activationCodeC3PO),securityConfig.generateCsrfHeader(csrfTokenRepository))
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate",request
        )
        Assertions.assertEquals(HttpStatus.NOT_FOUND,response.statusCode,"userC3PO validation failed")
        Assertions.assertEquals(
            5,
            activationRepository.findActivationByProvisionalId(provisionalIdC3PO).get().attempts,
            "num of attempts has not been decreased"
        )
    }
    
    @Test
    fun expiredDeadlineTest(){
        
        Assertions.assertFalse(activationRepository.findActivationByProvisionalId(provisionalIdC3PO).isEmpty,
        "the activation entry is not present on the db before")
        
        Assertions.assertFalse(userRepository.findByNickname(userC3PO.nickname).isEmpty,
                "the user entry is not present on the db before")
        
        val expiredDeadlineC3PO  = Calendar.getInstance()
        expiredDeadlineC3PO.add(Calendar.HOUR_OF_DAY, -12)
        
        val updatedActivation = activationRepository.findActivationByProvisionalId(provisionalIdC3PO).get()
        updatedActivation.activationDeadline=expiredDeadlineC3PO.time
        activationRepository.save(updatedActivation)
        
        val request = HttpEntity(ActivationRequestDTO(provisional_id = provisionalIdC3PO,activation_code = activationCodeC3PO),securityConfig.generateCsrfHeader(csrfTokenRepository))
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate",request
        )
        Assertions.assertEquals(HttpStatus.NOT_FOUND,response.statusCode,"userC3PO validation failed")
        
        Assertions.assertTrue(activationRepository.findActivationByProvisionalId(provisionalIdC3PO).isEmpty,
        "the expired activation entry in the db has not been deleted")
        
        Assertions.assertTrue(userRepository.findByNickname(userC3PO.nickname).isEmpty,
                "user was not deleted after receiving expired validation request")
    }
    
}