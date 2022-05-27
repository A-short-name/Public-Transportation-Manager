package it.polito.wa2.g15.lab5

import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import it.polito.wa2.g15.lab5.security.JwtUtils
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Configuration
class Config {
    @Autowired
    lateinit var jwtUtils: JwtUtils

    @Autowired
    lateinit var ticketItemRepository: TicketItemRepository
    private val logger = KotlinLogging.logger {}
    @Bean
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
    fun initTicketItemCache(): List<TicketItem>{

        var res : List<TicketItem>
        runBlocking {
            logger.info { "start initialization ticketItem cache ..." }
            res = ticketItemRepository.findAll().toList()
            logger.info { "... initialization ticketItem cache finished" }
            logger.info { "these are the ticket found in the catalog during the startup:\n $res" }
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