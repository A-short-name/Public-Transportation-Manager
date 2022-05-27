package it.polito.wa2.g15.lab5


import it.polito.wa2.g15.lab5.services.TicketCatalogService
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Lab5TicketCatalogService

@Autowired
lateinit var ticketCatalogService: TicketCatalogService

fun main(args: Array<String>) {
    runApplication<Lab5TicketCatalogService>(*args)

    runBlocking {
        ticketCatalogService.initTicketCatalogCache()
    }

}