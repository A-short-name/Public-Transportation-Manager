package it.polito.wa2.g15.lab5.paymentservice

import io.r2dbc.spi.ConnectionFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


//https://stackoverflow.com/questions/59007414/testcontainers-postgresqlcontainer-with-kotlin-unit-test-not-enough-informatio
class MyPostgresSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgresSQLContainer>(imageName)

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureTestDatabase()
class PaymentServiceApplicationTests {

//    @TestConfiguration
//    internal class TestConfig {
//        @Bean
//        fun initializer(connectionFactory: ConnectionFactory?): ConnectionFactoryInitializer {
//            val initializer = ConnectionFactoryInitializer()
//            initializer.setConnectionFactory(connectionFactory)
//            val populator = CompositeDatabasePopulator()
//            populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("schema.sql")))
//            populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("data.sql")))
//            initializer.setDatabasePopulator(populator)
//            return initializer
//        }
//    }

    companion object {
        @Container
        val postgres = MyPostgresSQLContainer("postgres:latest").apply {
            withDatabaseName("payments")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                ("r2dbc:postgresql://" + postgres.host + ":" + postgres.firstMappedPort + "/" + postgres.databaseName)
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
        }
    }
    @Test
    fun contextLoads() {
    }

    @Test
    fun myFirstTest(){
        Assertions.assertEquals(1,1)
    }
}
