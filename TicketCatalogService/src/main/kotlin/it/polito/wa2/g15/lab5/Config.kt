package it.polito.wa2.g15.lab5

import it.polito.wa2.g15.lab5.security.JwtUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

@Configuration
@EnableR2dbcRepositories
class Config {
    @Autowired
    lateinit var jwtUtils: JwtUtils

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

}