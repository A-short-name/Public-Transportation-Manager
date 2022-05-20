package it.polito.wa2.g15.lab5

import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.stereotype.Component
import java.util.logging.Logger

@SpringBootApplication
class Lab5TicketCatalogService

fun main(args: Array<String>) {
    runApplication<Lab5TicketCatalogService>(*args)
}

@Bean
fun initializer(connectionFactory: ConnectionFactory?): ConnectionFactoryInitializer {
    val initializer = ConnectionFactoryInitializer()
    if (connectionFactory != null) {
        initializer.setConnectionFactory(connectionFactory)
    }
    val populator = CompositeDatabasePopulator()
    populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("schema.sql")))
    initializer.setDatabasePopulator(populator)
    return initializer

    //initializer.setDatabasePopulator(ResourceDatabasePopulator(ClassPathResource("schema.sql")))
    //return initializer
}

/*
@Configuration
class InitializerConfiguration {
    private val LOGGER: Logger = LoggerFactory.getLogger(InitializerConfiguration::class.java)
    @Bean
    fun initializer(connectionFactory: ConnectionFactory?): ConnectionFactoryInitializer {
        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory!!)

    }
}
*/

