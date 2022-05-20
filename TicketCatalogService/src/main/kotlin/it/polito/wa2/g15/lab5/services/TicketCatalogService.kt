package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import kotlinx.coroutines.flow.Flow

interface TicketCatalogService {
    suspend fun getAllTicketItems() : Flow<TicketItem>

    suspend fun addNewTicketType(newTicketItemDTO: NewTicketItemDTO)
}