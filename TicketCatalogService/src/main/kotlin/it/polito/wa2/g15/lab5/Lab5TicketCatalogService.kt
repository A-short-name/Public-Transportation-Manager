package it.polito.wa2.g15.lab5


import it.polito.wa2.g15.lab5.services.TicketCatalogService
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Lab5TicketCatalogService

fun main(args: Array<String>) {
    val app = runApplication<Lab5TicketCatalogService>(*args)

    val ticketCatalogService = app.getBean(TicketCatalogService::class.java)
    runBlocking {
        ticketCatalogService.initTicketCatalogCache()
    }

}