package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.BuyTicketDTO
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import kotlinx.coroutines.flow.Flow
import reactor.core.publisher.Mono

interface TicketCatalogService {
    suspend fun getAllTicketItems() : Flow<TicketItem>

    suspend fun addNewTicketType(newTicketItemDTO: NewTicketItemDTO)

    /**
     * - Check if the ticket has some restrictions on user age,
     * if yes it asks the TravellerService the user profile in
     * order to check if the operation is permitted.
     * - In case it is, it saves the order in the database with the
     * PENDING status
     * - then it transmits the billing information and the total cost of the
     * order to the payment service through a kafka topic and returns the orderId
     *
     *
     *
     * - When the Kafka listener receives the outcome of the transaction, the status of order is
     * updated according to what the payment service has stated and, if the operation was
     * successful, the purchased products are added to the list of acquired tickets in the
     * TravellerService.
     * The client to check the order result, must do polling to check the outcome.
     *
     */
    suspend fun buyTicket(buyTicketDTO: BuyTicketDTO, ticketId: Long, userName: Mono<String>) : Mono<Long>
}