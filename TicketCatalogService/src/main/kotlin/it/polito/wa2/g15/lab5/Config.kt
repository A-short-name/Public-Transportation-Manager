package it.polito.wa2.g15.lab5

import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import it.polito.wa2.g15.lab5.security.JwtUtils
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Configuration
@EnableR2dbcRepositories
class Config {
    @Autowired
    lateinit var jwtUtils: JwtUtils

    @Value("\${ticket.catalog.cache}")
    lateinit var ticketCatalogCacheStatus : String

    @Autowired
    lateinit var ticketItemRepository: TicketItemRepository
    private val logger = KotlinLogging.logger {}
    @Bean
    @Order(1)
    fun generateClient(): WebClient {
        return WebClient.builder()
                .baseUrl("http://localhost:8081")
                //.defaultCookie("Cookie", "cookieValue")
                .defaultHeaders { headers ->
                        headers.contentType = MediaType.APPLICATION_JSON
                        headers.setBearerAuth(jwtUtils.generateJwtToken())
                        headers.set(HttpHeaders.ACCEPT_ENCODING, MediaType.APPLICATION_JSON_VALUE)
                        headers.set("Cookie", "XSRF-TOKEN=224159f4-d4ed-41ff-b726-c6d7a2ad71d6")
                        headers.set("X-XSRF-TOKEN", "224159f4-d4ed-41ff-b726-c6d7a2ad71d6")
                }
                .defaultUriVariables(Collections.singletonMap("url", "http://localhost:8081"))
                .build()
    }
    @Bean
    @Order(2)
    fun initializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {
        val initializer = ConnectionFactoryInitializer()
        initializer.setConnectionFactory(connectionFactory)
        initializer.setDatabasePopulator(
            ResourceDatabasePopulator(
                ClassPathResource("schema.sql")
            )
        )
        return initializer
    }
    @Bean
    @Order(3)
    fun initTicketItemCache(): MutableList<TicketItem>{

        var res : MutableList<TicketItem>
        runBlocking {
            if(ticketCatalogCacheStatus == "enabled") {
                logger.info { "start initialization ticketItem cache ..." }
                res = ticketItemRepository.findAll().toList().toMutableList()
                logger.info { "... initialization ticketItem cache finished" }
                logger.info { "these are the ticket found in the catalog during the startup:\n $res" }
            }
            else res = mutableListOf()
        }
        return res
    }



/*private fun httpHeaders(): Consumer<HttpHeaders> {
    return Consumer<HttpHeaders> { headers ->
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBearerAuth(jwtUtils.generateJwtToken())
        headers.set(HttpHeaders.ACCEPT_ENCODING, MediaType.APPLICATION_JSON_VALUE)
    }
}*/


}