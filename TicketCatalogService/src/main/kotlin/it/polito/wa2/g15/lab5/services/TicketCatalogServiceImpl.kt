package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.entities.TicketType
import it.polito.wa2.g15.lab5.repositories.TicketTypeRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TicketCatalogServiceImpl : TicketCatalogService {
    @Autowired
    lateinit var ticketTypeRepository: TicketTypeRepository

    override suspend fun getAllTicketTypes() : Flow<TicketType> {
        return ticketTypeRepository.findAll()
    }
}