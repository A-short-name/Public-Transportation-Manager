package it.polito.wa2.g15.lab3.integration.controller

import it.polito.wa2.g15.lab3.SecurityConfiguration
import it.polito.wa2.g15.lab3.dtos.ActivationRequestDTO
import it.polito.wa2.g15.lab3.dtos.UserLoginRequestDTO
import it.polito.wa2.g15.lab3.dtos.UserRequestDTO
import it.polito.wa2.g15.lab3.entities.ERole
import it.polito.wa2.g15.lab3.entities.Role
import it.polito.wa2.g15.lab3.repositories.ActivationRepository
import it.polito.wa2.g15.lab3.repositories.RoleRepository
import it.polito.wa2.g15.lab3.repositories.UserRepository
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*
import kotlin.system.measureTimeMillis

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ControllerFullTest {
    
    companion object {
        @Container
        val postgres = MyPostgreSQLContainer("postgres:latest")
        
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
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
    
    private val nicknameUC1 = "userC1"
    private val emailUC1 = "emailC1@examplemail.com"
    private val passwordUC1 = "Str0ng!S3cr3t"
    private lateinit var activationCodeUC1: String
    private lateinit var provisionalIdUC1: UUID
    
    private val nicknameUC2 = "userC2"
    private val emailUC2 = "emailC2@examplemail.com"
    private val passwordUC2 = "Str0ng!S3cr3t"
    private lateinit var activationCodeUC2: String
    private lateinit var provisionalIdUC2: UUID
    
    @BeforeEach
    fun initTest() {
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
        
        val request = HttpEntity(
            UserRequestDTO(nicknameUC1, emailUC1, passwordUC1),
            securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        
        val response = restTemplate.postForEntity<MutableMap<String, String>>(
            "http://localhost:$port/user/register", request
        )
        Assertions.assertEquals(HttpStatus.ACCEPTED, response.statusCode, "registration failed")
        provisionalIdUC1 = UUID.fromString(response.body!!["provisional_id"]!!)
        activationCodeUC1 =
            activationRepository.findActivationByProvisionalId(
                provisionalIdUC1
            ).get().activationCode
        
        val request2 =
            HttpEntity(
                UserRequestDTO(nicknameUC2, emailUC2, passwordUC2),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
            )
        
        val response2 = restTemplate.postForEntity<MutableMap<String, String>>(
            "http://localhost:$port/user/register", request2
        )
        Assertions.assertEquals(HttpStatus.ACCEPTED, response2.statusCode, "registration failed")
        provisionalIdUC2 = UUID.fromString(response2.body!!["provisional_id"]!!)
        activationCodeUC2 =
            activationRepository.findActivationByProvisionalId(
                provisionalIdUC2
            ).get().activationCode
    }
    
    @AfterEach
    fun tearDownTest() {
        //if(activationRepository.count() > 0) {
        activationRepository.deleteAll()
        userRepository.deleteAll()
        //}
    }
    
    @Test
    fun `multiple activation`() {
        //UC2 try to validate himself with UC1 activation code
        val request0 = HttpEntity(
            ActivationRequestDTO(provisional_id = provisionalIdUC2, activation_code = activationCodeUC1),
            securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response0 = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate", request0
        )
        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            response0.statusCode,
            "user validation should fail because I use $activationCodeUC1 " +
                    "for user with prov id $provisionalIdUC2 that has activation code $activationCodeUC2"
        )
        Assertions.assertFalse(
            userRepository.findByNickname(nicknameUC2).get().active,
            "the user is not active in the db"
        )
        
        val request = HttpEntity(
            ActivationRequestDTO(provisional_id = provisionalIdUC1, activation_code = activationCodeUC1),
            securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate", request
        )
        Assertions.assertEquals(HttpStatus.CREATED, response.statusCode, "user validation failed")
        Assertions.assertTrue(
            userRepository.findByNickname(nicknameUC1).get().active,
            "the user is not active in the db"
        )
        
        val request2 = HttpEntity(
            ActivationRequestDTO(provisional_id = provisionalIdUC2, activation_code = activationCodeUC2),
            securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response2 = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate", request2
        )
        Assertions.assertEquals(HttpStatus.CREATED, response2.statusCode, "user validation failed")
        Assertions.assertTrue(
            userRepository.findByNickname(nicknameUC2).get().active,
            "the user is not active in the db"
        )
    }
    
    @Value("\${ratelimiter.capacity}")
    var CAPACITY: Long = -1
    
    @Value("\${ratelimiter.refilltime}")
    var REFILL_TIME: Long = -1    //fixed rate at which tokens are added to the bucket
    
    @Test
    fun `too Many Request on register endpoint`() {
        //Capacity and Refill time of Bucket for RateLimiter are set in application.properties
        val csrfTokenHeaders = securityConfig.generateCsrfHeader(csrfTokenRepository)
        val password = "Str0ng!S3cr3t"
        val requests = mutableListOf<HttpEntity<UserRequestDTO>>()
        //Creating an array of CAPACITY +1 Request
        for (i in 0..CAPACITY) {
            val nickname = "nickname$i"
            val email = "emailTest$i@thisKotlin.com"
            requests.add(i.toInt(), HttpEntity(UserRequestDTO(nickname, email, password), csrfTokenHeaders))
        }
        var response: ResponseEntity<MutableMap<String, String>>
        val measurement = measureTimeMillis {
            for (i in 0 until CAPACITY) {
                response = restTemplate.postForEntity(
                    "http://localhost:$port/user/register", requests[i.toInt()]
                )
                Assertions.assertEquals(HttpStatus.ACCEPTED, response.statusCode, "registration failed")
                Assertions.assertNotNull(response.body, "no body in response")
                
                Assertions.assertEquals(
                    requests[i.toInt()].body!!.email,
                    response.body!!["email"],
                    "email doesn't match"
                )
                Assertions.assertNotNull(response.body!!["provisional_id"], "no provisional_id in response body")
            }
            
            //Sent the post that must be discarded (exceeds capacity)
            response = restTemplate.postForEntity(
                "http://localhost:$port/user/register", requests[CAPACITY.toInt()]
            )
        }
        Assumptions.assumeTrue(measurement < REFILL_TIME * 1000, "Time exceeded. Test not valid.")
        
        Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode, "Not enough many request")
    }
    
    @Test
    fun `too Many Request on validate endpoint`() {
        //Capacity and Refill time of Bucket for RateLimiter are set in application.properties
        val csrfTokenHeaders = securityConfig.generateCsrfHeader(csrfTokenRepository)
        val requests = mutableListOf<HttpEntity<ActivationRequestDTO>>()
        //Creating an array of CAPACITY +1 Request
        //They are all wrong... that's not important, just write properly the assert
        for (i in 0..CAPACITY) {
            val provisional_id = UUID.randomUUID()
            val activationCode = "123456"
            requests.add(i.toInt(), HttpEntity(ActivationRequestDTO(provisional_id, activationCode), csrfTokenHeaders))
        }
        var response: ResponseEntity<MutableMap<String, String>>
        val measurement = measureTimeMillis {
            for (i in 0 until CAPACITY) {
                response = restTemplate.postForEntity(
                    "http://localhost:$port/user/validate", requests[i.toInt()]
                )
                Assertions.assertEquals(HttpStatus.NOT_FOUND, response.statusCode, "validation is not failed")
            }
            
            //Sent the post that must be discarded (exceeds capacity)
            response = restTemplate.postForEntity(
                "http://localhost:$port/user/validate", requests[CAPACITY.toInt()]
            )
        }
        Assumptions.assumeTrue(measurement < REFILL_TIME * 1000, "Time exceeded. Test not valid.")
        
        Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode, "Not enough many request")
    }
    
    @Test
    fun `many request from different client are allowed`() {
        //Capacity and Refill time of Bucket for RateLimiter are set in application.properties
        //A different csrf token correspond to another client
        //Is used as an API-key header
        val csrfTokenHeaders1 = securityConfig.generateCsrfHeader(csrfTokenRepository)
        val csrfTokenHeaders2 = securityConfig.generateCsrfHeader(csrfTokenRepository)
        val password = "Str0ng!S3cr3t"
        val requests1 = mutableListOf<HttpEntity<UserRequestDTO>>()
        val requests2 = mutableListOf<HttpEntity<UserRequestDTO>>()
        //Creating an array of CAPACITY +1 Request
        for (i in 0..CAPACITY) {
            val nickname1 = "nickname$i"
            val email1 = "emailTest$i@thisKotlin.com"
            val nickname2 = "nickname${i + CAPACITY * 10}"
            val email2 = "emailTest${i + CAPACITY * 10}@thisKotlin.com"
            requests1.add(i.toInt(), HttpEntity(UserRequestDTO(nickname1, email1, password), csrfTokenHeaders1))
            requests2.add(i.toInt(), HttpEntity(UserRequestDTO(nickname2, email2, password), csrfTokenHeaders2))
        }
        var response1: ResponseEntity<MutableMap<String, String>>
        var response2: ResponseEntity<MutableMap<String, String>>
        val measurement = measureTimeMillis {
            for (i in 0 until CAPACITY) {
                response1 = restTemplate.postForEntity(
                    "http://localhost:$port/user/register", requests1[i.toInt()]
                )
                response2 = restTemplate.postForEntity(
                    "http://localhost:$port/user/register", requests2[i.toInt()]
                )
                Assertions.assertEquals(HttpStatus.ACCEPTED, response1.statusCode, "registration failed")
                Assertions.assertNotNull(response1.body, "no body in response")
                Assertions.assertEquals(
                    requests1[i.toInt()].body!!.email,
                    response1.body!!["email"],
                    "email doesn't match"
                )
                Assertions.assertNotNull(response1.body!!["provisional_id"], "no provisional_id in response body")
                
                Assertions.assertEquals(HttpStatus.ACCEPTED, response2.statusCode, "registration failed")
                Assertions.assertNotNull(response2.body, "no body in response")
                Assertions.assertEquals(
                    requests2[i.toInt()].body!!.email,
                    response2.body!!["email"],
                    "email doesn't match"
                )
                Assertions.assertNotNull(response2.body!!["provisional_id"], "no provisional_id in response body")
            }
            
            //Sent the post that must be discarded (exceeds capacity)
            response1 = restTemplate.postForEntity(
                "http://localhost:$port/user/register", requests1[CAPACITY.toInt()]
            )
            response2 = restTemplate.postForEntity(
                "http://localhost:$port/user/register", requests2[CAPACITY.toInt()]
            )
        }
        Assumptions.assumeTrue(measurement < REFILL_TIME * 1000, "Time exceeded. Test not valid.")
        
        Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, response1.statusCode, "Not enough many request")
        Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, response2.statusCode, "Not enough many request")
    }
    
    @Test
    fun `many request from same client on different endpoint are limited`() {
        //Capacity and Refill time of Bucket for RateLimiter are set in application.properties
        val csrfTokenHeaders = securityConfig.generateCsrfHeader(csrfTokenRepository)
        val password = "Str0ng!S3cr3t"
        val requestsForRegister = mutableListOf<HttpEntity<UserRequestDTO>>()
        val requestsForValidate = mutableListOf<HttpEntity<ActivationRequestDTO>>()
        //Creating an array of CAPACITY +1 Request
        for (i in 0..CAPACITY) {
            val nickname = "nickname$i"
            val email = "emailTest$i@thisKotlin.com"
            requestsForRegister.add(i.toInt(), HttpEntity(UserRequestDTO(nickname, email, password), csrfTokenHeaders))
            val provisional_id = UUID.randomUUID()
            val activationCode = "123456"
            //They are all wrong... that's not important, just write properly the assert
            requestsForValidate.add(
                i.toInt(),
                HttpEntity(ActivationRequestDTO(provisional_id, activationCode), csrfTokenHeaders)
            )
        }
        var responseFromRegister: ResponseEntity<MutableMap<String, String>>
        var responseFromValidate: ResponseEntity<MutableMap<String, String>>
        val measurement = measureTimeMillis {
            for (i in 0 until CAPACITY / 2) {
                responseFromRegister = restTemplate.postForEntity(
                    "http://localhost:$port/user/register", requestsForRegister[i.toInt()]
                )
                Assertions.assertEquals(HttpStatus.ACCEPTED, responseFromRegister.statusCode, "registration failed")
                Assertions.assertNotNull(responseFromRegister.body, "no body in response")
                
                Assertions.assertEquals(
                    requestsForRegister[i.toInt()].body!!.email,
                    responseFromRegister.body!!["email"],
                    "email doesn't match"
                )
                Assertions.assertNotNull(
                    responseFromRegister.body!!["provisional_id"],
                    "no provisional_id in response body"
                )
                
                responseFromValidate = restTemplate.postForEntity(
                    "http://localhost:$port/user/validate", requestsForValidate[i.toInt()]
                )
                Assertions.assertEquals(
                    HttpStatus.NOT_FOUND,
                    responseFromValidate.statusCode,
                    "validation is not failed"
                )
            }
            if (CAPACITY % 2 != 0L) {
                //capacity is odd and so I've to put another request
                responseFromValidate = restTemplate.postForEntity(
                    "http://localhost:$port/user/validate", requestsForValidate[(CAPACITY / 2 - 1).toInt()]
                )
                Assertions.assertEquals(
                    HttpStatus.NOT_FOUND,
                    responseFromValidate.statusCode,
                    "validation is not failed"
                )
            }
            
            //Sent the post that must be discarded (exceeds capacity)
            responseFromRegister = restTemplate.postForEntity(
                "http://localhost:$port/user/register", requestsForRegister[CAPACITY.toInt()]
            )
            responseFromValidate = restTemplate.postForEntity(
                "http://localhost:$port/user/validate", requestsForValidate[CAPACITY.toInt()]
            )
        }
        Assumptions.assumeTrue(measurement < REFILL_TIME * 1000, "Time exceeded. Test not valid.")
        
        Assertions.assertEquals(
            HttpStatus.TOO_MANY_REQUESTS,
            responseFromRegister.statusCode,
            "Not enough many request"
        )
        Assertions.assertEquals(
            HttpStatus.TOO_MANY_REQUESTS,
            responseFromValidate.statusCode,
            "Not enough many request"
        )
    }
    
    @Test
    fun `login after registration and validation`() {
        val request1 = HttpEntity(
            ActivationRequestDTO(provisional_id = provisionalIdUC1, activation_code = activationCodeUC1),
            securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response1 = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/validate", request1
        )
        Assertions.assertEquals(HttpStatus.CREATED, response1.statusCode, "user validation failed")
        Assertions.assertTrue(
            userRepository.findByNickname(nicknameUC1).get().active,
            "the user is not active in the db"
        )
        
        val request2 = HttpEntity(
            UserLoginRequestDTO(nickname = nicknameUC1, password = passwordUC1),
            securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response2 = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/login", request2
        )
        
        Assertions.assertEquals(HttpStatus.OK, response2.statusCode, "user login failed")
    }
}