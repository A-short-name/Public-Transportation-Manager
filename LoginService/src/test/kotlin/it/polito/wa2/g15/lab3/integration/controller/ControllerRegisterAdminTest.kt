package it.polito.wa2.g15.lab3.integration.controller

import io.jsonwebtoken.Jwts
import it.polito.wa2.g15.lab3.SecurityConfiguration
import it.polito.wa2.g15.lab3.dtos.AdminRequestDTO
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
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
class ControllerRegisterAdminTest {
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
    lateinit var csrfTokenRepository: CsrfTokenRepository
    @Autowired
    lateinit var securityConfig: SecurityConfiguration
    @Autowired
    lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    lateinit var jwtUtils: JwtUtils

    val userC3POPassword = "BipBop42!"
    lateinit var userC3PO :User

    lateinit var normalAdmin: User

    lateinit var users : MutableList<User>

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
            val superAdminRole = Role().apply {
                this.name = ERole.SUPERADMIN
            }
            roleRepository.save(superAdminRole)
            val embeddedRole = Role().apply {
                this.name = ERole.EMBEDDED
            }
            roleRepository.save(embeddedRole)
        }
        if(userRepository.count() == 0L) {

            userC3PO= User().apply {
                this.nickname = "C3PO"
                this.password = passwordEncoder.encode(userC3POPassword)
                this.email = "drone.galaxy@mail.com"
                this.active = true
            }
            userC3PO.addCustomerRole(roleRepository.findByName(ERole.SUPERADMIN).get())
            userRepository.save(userC3PO)

            normalAdmin = User().apply {
                this.nickname = "normaladmin"
                this.password = passwordEncoder.encode("Strong!1Password")
                this.email = "normaladmin@mail.com"
                this.active = true
            }
            userC3PO.addCustomerRole(roleRepository.findByName(ERole.ADMIN).get())
            userRepository.save(normalAdmin)


            users = userRepository.findAll().toMutableList()
        }

    }

    @AfterEach
    fun teardownDb(){
        if(userRepository.count() > 0) {
            userRepository.deleteAll()
            users.clear()
        }
    }

    //https://github.com/spring-projects/spring-security-oauth/issues/1906
    //Spring DaoAuthenticationProvider throws BadCredentialsException, which is converted to InvalidGrantException.
    //Then the InvalidGrantException is translated to 400 by the DefaultWebResponseExceptionTranslator

    @Test
    fun `successfully create a superadmin`() {
        val request = HttpEntity(
                UserLoginRequestDTO(nickname = userC3PO.nickname, password =  userC3POPassword),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request
        )

        println(response.body)
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "user login failed")

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add("Authorization", "Bearer ${response.body!!.token}")

        val createRequest = HttpEntity(
                AdminRequestDTO("newsuperadmin","test@example.com","NewSuper!Admin22",true),
                requestHeader
        )
        val createResponse : ResponseEntity<Unit> = restTemplate.exchange(
                "http://localhost:$port/admin/create",
                HttpMethod.POST,
                createRequest
        )

        Assertions.assertEquals(HttpStatus.CREATED, createResponse.statusCode, "user creation failed")
    }

    @Test
    fun `successfully create a admin`() {
        val request = HttpEntity(
                UserLoginRequestDTO(nickname = userC3PO.nickname, password =  userC3POPassword),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request
        )

        println(response.body)
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "user login failed")

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add("Authorization", "Bearer ${response.body!!.token}")

        val createRequest = HttpEntity(
                AdminRequestDTO("newsuperadmin","test@example.com","NewSuper!Admin22",false),
                requestHeader
        )
        val createResponse : ResponseEntity<Unit> = restTemplate.exchange(
                "http://localhost:$port/admin/create",
                HttpMethod.POST,
                createRequest
        )

        Assertions.assertEquals(HttpStatus.CREATED, createResponse.statusCode, "user creation failed")
    }

    @Test
    fun `try to access superadmin api without being logged in`() {
        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)

        val createRequest = HttpEntity(
                AdminRequestDTO("newsuperadmin","test@example.com","NewSuper!Admin22",true),
                requestHeader
        )
        val createResponse : ResponseEntity<Unit> = restTemplate.exchange(
                "http://localhost:$port/admin/create",
                HttpMethod.POST,
                createRequest
        )

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, createResponse.statusCode, "user creation successfull. Should not happen")
    }

    @Test
    fun `trying to access superadmin API as admin`() {
        val request = HttpEntity(
                UserLoginRequestDTO(nickname = normalAdmin.nickname, password =  "Strong!1Password"),
                securityConfig.generateCsrfHeader(csrfTokenRepository)
        )
        val response = restTemplate.postForEntity<UserLoginResponseDTO>(
                "http://localhost:$port/user/login", request
        )

        println(response.body)
        Assertions.assertEquals(HttpStatus.OK, response.statusCode, "user login failed")

        val requestHeader = securityConfig.generateCsrfHeader(csrfTokenRepository)
        requestHeader.add("Authorization", "Bearer ${response.body!!.token}")

        val createRequest = HttpEntity(
                AdminRequestDTO("newsuperadmin","test@example.com","NewSuper!Admin22",false),
                requestHeader
        )
        val createResponse : ResponseEntity<Unit> = restTemplate.exchange(
                "http://localhost:$port/admin/create",
                HttpMethod.POST,
                createRequest
        )

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, createResponse.statusCode, "user creation successfull. Should not happen")
    }
}