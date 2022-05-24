package it.polito.wa2.g15.lab5

import it.polito.wa2.g15.lab5.security.JwtUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.util.*
import java.util.function.Consumer

@SpringBootApplication
class Lab5TicketCatalogService

fun main(args: Array<String>) {
    runApplication<Lab5TicketCatalogService>(*args)
}