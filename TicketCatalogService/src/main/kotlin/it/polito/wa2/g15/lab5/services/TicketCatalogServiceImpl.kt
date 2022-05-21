package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.BuyTicketDTO
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TicketCatalogServiceImpl : TicketCatalogService {
    @Autowired
    lateinit var ticketItemRepository: TicketItemRepository

    private val logger = KotlinLogging.logger {}

    override suspend fun getAllTicketItems() : Flow<TicketItem> {
        return ticketItemRepository.findAll()
    }

    override suspend fun addNewTicketType(newTicketItemDTO: NewTicketItemDTO) {
        val ticketItem = TicketItem(ticketType = newTicketItemDTO.type,price = newTicketItemDTO.price)

        try {
            ticketItemRepository.save(ticketItem)
        } catch (e: Exception) {
            throw Exception("Failed saving ticketItem: ${e.message}")
        }
    }

    override suspend fun buyTicket(buyTicketDTO: BuyTicketDTO, ticketId: Long, userName: Mono<String>) : Mono<Long> {
        logger.info("order request received from user ${userName.awaitSingle()} for ${buyTicketDTO.numOfTickets } ticket $ticketId" +
                "\n the user want to pay with ${buyTicketDTO.paymentInfo}")

        TODO("Not yet implemented")
    }

}