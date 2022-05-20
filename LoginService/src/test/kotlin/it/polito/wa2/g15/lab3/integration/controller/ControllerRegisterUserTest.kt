package it.polito.wa2.g15.lab3.integration.controller

import it.polito.wa2.g15.lab3.SecurityConfiguration
import it.polito.wa2.g15.lab3.dtos.UserRequestDTO
import it.polito.wa2.g15.lab3.dtos.toUserRequestDTO
import it.polito.wa2.g15.lab3.entities.ERole
import it.polito.wa2.g15.lab3.entities.Role
import it.polito.wa2.g15.lab3.entities.User
import it.polito.wa2.g15.lab3.repositories.ActivationRepository
import it.polito.wa2.g15.lab3.repositories.RoleRepository
import it.polito.wa2.g15.lab3.repositories.UserRepository
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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

//https://stackoverflow.com/questions/59007414/testcontainers-postgresqlcontainer-with-kotlin-unit-test-not-enough-informatio
class MyPostgreSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgreSQLContainer>(imageName)

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ControllerRegisterUserTest {
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
    
    lateinit var users: MutableList<User>
    
    @BeforeEach
    fun populateDb() {
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
        if (userRepository.count() == 0L) {
            userRepository.save(User().apply {
                this.nickname = "C3PO"
                this.password = "BipBop42"
                this.email = "drone.galaxy@mail.com"
                this.active = true
            })
            
            users = userRepository.findAll().toMutableList()
        }
    }
    
    @AfterEach
    fun teardownDb() {
        if (userRepository.count() > 0) {
            activationRepository.deleteAll()
            userRepository.deleteAll()
            users.clear()
        }
    }
    
    val nickname = "nickname"
    val email = "emailTest@thisKotlin.com"
    val password = "Str0ng!S3cr3t"
    
    @Test
    fun validRegisterUserTest() {
        
        val request = HttpEntity(UserRequestDTO(nickname, email, password),securityConfig.generateCsrfHeader(csrfTokenRepository))

        val response = restTemplate.postForEntity<MutableMap<String, String>>(
            "http://localhost:$port/user/register", request
        )
        Assertions.assertEquals(HttpStatus.ACCEPTED, response.statusCode, "registration failed")
        Assertions.assertNotNull(response.body, "no body in response")
        
        Assertions.assertEquals(email, response.body!!["email"], "email doesn't match")
        Assertions.assertNotNull(response.body!!["provisional_id"], "no provisionalUUID in response body")
    }
    
    @Test
    fun emptyNicknameRegisterTest() {
        //Passing wrong DTO
        val request = HttpEntity(UserRequestDTO("", email,password),
            securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
    @Test
    fun emptyEmailRegisterTest() {
        //Passing wrong DTO
        val request = HttpEntity(UserRequestDTO(nickname, "",password),
            securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
    @Test
    fun emptyPasswordRegisterTest() {
        //Passing wrong DTO
        val request = HttpEntity(UserRequestDTO(nickname, email,""),
            securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
    @Test
    fun shortPasswordRegisterTest() {
        //Passing wrong DTO
        val request = HttpEntity(UserRequestDTO(nickname, email,"aA1!"),
            securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
    
    @Test
    fun weakPasswordRegisterTest() {
        //Passing wrong DTO
        val request = HttpEntity(UserRequestDTO(nickname, email,"aA111111111"),
            securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
    
    @Test
    fun passwordContainingSpacesRegisterTest() {
        //Passing wrong DTO
        val request = HttpEntity(UserRequestDTO(nickname, email,"aA1! 111111"),
            securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
    
    @Test
    fun invalidEmailRegisterTest() {
        //Passing wrong DTO
        val request = HttpEntity(UserRequestDTO(nickname, "aaa@wrongmail",password),
            securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
    
    @Test
    fun alreadyPresentUserTest() {
        val user = users[0].toUserRequestDTO()
        
        val request = HttpEntity(user,securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
    
    @Test
    fun alreadyPresentEmailTest() {
        val user = UserRequestDTO("newUser", users[0].email, "secret")
        
        val request = HttpEntity(user,securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
    
    @Test
    fun alreadyPresentNicknameTest() {
        val user = UserRequestDTO(users[0].nickname, "new.email@mail.com", "secret")
        
        val request = HttpEntity(user,securityConfig.generateCsrfHeader(csrfTokenRepository))
        
        val response = restTemplate.postForEntity<Unit>(
            "http://localhost:$port/user/register", request
        )
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }
}