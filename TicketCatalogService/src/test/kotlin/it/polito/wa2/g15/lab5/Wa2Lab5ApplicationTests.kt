package it.polito.wa2.g15.lab5

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

//https://stackoverflow.com/questions/59007414/testcontainers-postgresqlcontainer-with-kotlin-unit-test-not-enough-informatio
class MyPostgresSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgresSQLContainer>(imageName)

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Wa2Lab5ApplicationTests {

    @Test
    fun contextLoads() {
    }

    @Test
    fun firstSimpleTest(){
        Assertions.assertTrue(true)
    }


}
