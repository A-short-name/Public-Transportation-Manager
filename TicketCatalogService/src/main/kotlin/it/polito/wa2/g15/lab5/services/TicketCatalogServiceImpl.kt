package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TicketCatalogServiceImpl : TicketCatalogService {
    @Autowired
    lateinit var ticketItemRepository: TicketItemRepository

    override suspend fun getAllTicketItems() : Flow<TicketItem> {
        return ticketItemRepository.findAll()
    }

    override suspend fun addNewTicketType(newTicketItemDTO: NewTicketItemDTO) {
        val ticketItem = TicketItem(type = newTicketItemDTO.type,price = newTicketItemDTO.price)

        try {
            ticketItemRepository.save(ticketItem)
        } catch (e: Exception) {
            throw Exception("Testing: ${e.message}")
        }
    }
}