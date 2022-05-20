package it.polito.wa2.g15.lab3.integration.controller

import io.jsonwebtoken.Jwts
import it.polito.wa2.g15.lab3.SecurityConfiguration
import it.polito.wa2.g15.lab3.dtos.UserLoginRequestDTO
import it.polito.wa2.g15.lab3.dtos.UserLoginResponseDTO
import it.polito.wa2.g15.lab3.entities.Activation
import it.polito.wa2.g15.lab3.entities.ERole
import it.polito.wa2.g15.lab3.entities.Role
import it.polito.wa2.g15.lab3.entities.User
import it.polito.wa2.g15.lab3.repositories.ActivationRepository
import it.polito.wa2.g15.lab3.repositories.RoleRepository
import it.polito.wa2.g15.lab3.repositories.UserRepository
import it.polito.wa2.g15.lab3.security.jwt.JwtUtils
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
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@Testcontainers
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
class ControllerLoginUserTest {
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
    @Autowired
    lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    lateinit var jwtUtils: JwtUtils


    val activationCodeC3PO = "123456"
    lateinit var okDeadlineC3PO : Calendar

    val userC3POPassword = "BipBop42"
    lateinit var userC3PO :User

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

            userC3PO= User().apply {
                this.nickname = "C3PO"
                this.password = passwordEncoder.encode(userC3POPassword)
                this.email = "drone.galaxy@mail.com"
                this.active = true
            }
            userC3PO.addCustomerRole(roleRepository.findByName(ERole.CUSTOMER).get())
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

    //https://github.com/spring-projects/spring-security-oauth/issues/1906
    //Spring DaoAuthenticationProvider throws BadCredentialsException, which is converted to InvalidGrantException.
    //Then the InvalidGrantException is translated to 400 by the DefaultWebResponseExceptionTranslator

    @Test
    fun `successful login`() {
        val request = HttpEntity(
                UserLoginRequestDTO(nickname = userC3PO.nickname, password =  userC3POPassword),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request
        )

        println(response.body)
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "user login failed")
    }

    @Test
    fun `body of the response login`() {
        val request = HttpEntity(
                UserLoginRequestDTO(nickname = userC3PO.nickname, password =  userC3POPassword),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request
        )

        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "user login failed")
        Assertions.assertNotNull(response.body!!.token, "Token is null")
        println(response.headers)
        println(response.body!!)
    }

    @Test
    fun `wrong nickname login`() {
        val request = HttpEntity(
                UserLoginRequestDTO(nickname = "wrongNickname", password =  userC3POPassword),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<Unit>(
                "http://localhost:$port/user/login", request
        )

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode, "user login successful")
    }

    @Test
    fun `wrong password login`() {
        val request = HttpEntity(
                UserLoginRequestDTO(nickname = userC3PO.nickname, password =  "SuperWrongPassword123"),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<Unit>(
                "http://localhost:$port/user/login", request
        )

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode, "user login successful")
    }

    @Test
    fun `not validated login`() {
        val nicknameUC2 = "userC2"
        val emailUC2 = "emailC2@examplemail.com"
        val passwordUC2 = "Str0ng!S3cr3t"

        userRepository.save(User().apply {
                                        nickname = nicknameUC2
                                        email= emailUC2
                                        password = passwordEncoder.encode(passwordUC2)
                                        active = false})
        val request = HttpEntity(
                UserLoginRequestDTO(nickname = nicknameUC2, password =  passwordUC2),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<Unit>(
                "http://localhost:$port/user/login", request
        )

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.statusCode, "user login successful")
    }

    @Test
    fun `successful login after validation`() {
        val nicknameUC2 = "userC2"
        val emailUC2 = "emailC2@examplemail.com"
        val passwordUC2 = "Str0ng!S3cr3t"
        val activationCodeUC2 = "123456"

        val UC2 = User().apply {
            nickname = nicknameUC2
            email= emailUC2
            password = passwordEncoder.encode(passwordUC2)
            active = false}
        val savedUser = userRepository.save(UC2)

        val request = HttpEntity(
                UserLoginRequestDTO(nickname = nicknameUC2, password =  passwordUC2),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        //Login before activation
        val response1 = restTemplate.postForEntity<Unit>(
                "http://localhost:$port/user/login", request
        )
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response1.statusCode, "user login failed")

        userRepository.deleteById(savedUser.id)
        UC2.active=true
        userRepository.save(UC2)

        //Login after activation
        val response2 = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request
        )

        Assertions.assertEquals(HttpStatus.OK, response2.statusCode, "user login failed")

    }

    @Test
    fun `successful admin login`() {
        val bigBossPass = "MySuperP4ss"
        val bigBoss = User().apply {
            this.nickname = "BigBoss"
            this.password = passwordEncoder.encode(bigBossPass)
            this.email = "big.boss@mail.com"
            this.active = true
        }
        bigBoss.addCustomerRole(roleRepository.findByName(ERole.ADMIN).get())
        userRepository.save(bigBoss)

        val request = HttpEntity(
                UserLoginRequestDTO(nickname = bigBoss.nickname, password =  bigBossPass),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request
        )

        println(response.body)
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "user login failed")

        val rolesSet = jwtUtils.getRolesFromJwt(response.body!!.token)
        Assertions.assertTrue(rolesSet.contains("ADMIN"))
    }

    //Test to generate a set of fresh token
    @Test
    fun `generate Token`() {
        //Token 1 role CUSTOMER
        val request1 = HttpEntity(
                UserLoginRequestDTO(nickname = userC3PO.nickname, password =  userC3POPassword),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response1 = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request1
        )

        Assertions.assertEquals(HttpStatus.OK, response1.statusCode, "user login failed")
        println("C3PO, role CUSTOMER, token : ${response1.body!!.token}")
        //Token 2 role CUSTOMER
        val secondUser = User().apply {
            this.nickname = "SecondUser"
            this.password = passwordEncoder.encode("myBeautifulP4ss")
            this.email = "second.user@mail.com"
            this.active = true
        }
        secondUser.addCustomerRole(roleRepository.findByName(ERole.CUSTOMER).get())
        userRepository.save(secondUser)

        val request2 = HttpEntity(
                UserLoginRequestDTO(nickname = secondUser.nickname, password =  "myBeautifulP4ss"),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response2 = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request2
        )

        Assertions.assertEquals(HttpStatus.OK, response2.statusCode, "user login failed")
        println("SecondCustomer, role CUSTOMER, token : ${response2.body!!.token}")
        //Token 3 role ADMIN
        val admin = User().apply {
            this.nickname = "BigBoss"
            this.password = passwordEncoder.encode("securityF1rst")
            this.email = "admin.wow@mail.com"
            this.active = true
        }
        admin.addCustomerRole(roleRepository.findByName(ERole.ADMIN).get())
        userRepository.save(admin)

        val request3 = HttpEntity(
                UserLoginRequestDTO(nickname = admin.nickname, password =  "securityF1rst"),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response3 = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request3
        )

        Assertions.assertEquals(HttpStatus.OK, response3.statusCode, "user login failed")
        println("BigBoss, role ADMIN, token : ${response3.body!!.token}")

        //Token 4 role ADMIN AND CUSTOMER
        val adminAndCustomer = User().apply {
            this.nickname = "Boss&Customer"
            this.password = passwordEncoder.encode("securityS2cond")
            this.email = "admin&custom.wow@mail.com"
            this.active = true
        }
        adminAndCustomer.addCustomerRole(roleRepository.findByName(ERole.CUSTOMER).get())
        adminAndCustomer.addCustomerRole(roleRepository.findByName(ERole.ADMIN).get())
        userRepository.save(adminAndCustomer)

        val request4 = HttpEntity(
                UserLoginRequestDTO(nickname = adminAndCustomer.nickname, password =  "securityS2cond"),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response4 = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request4
        )

        Assertions.assertEquals(HttpStatus.OK, response4.statusCode, "user login failed")
        println("Boss&Customer, roles ADMIN & CUSTOMER, token : ${response4.body!!.token}")

    }

}