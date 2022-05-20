package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.entities.TicketType
import kotlinx.coroutines.flow.Flow

interface TicketCatalogService {
    suspend fun getAllTicketTypes() : Flow<TicketType>
}