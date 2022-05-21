package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.BuyTicketDTO
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketOrderException
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketRestrictionException
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

    @Autowired
    lateinit var ticketOrderService: TicketOrderService

    private val logger = KotlinLogging.logger {}

    override suspend fun getAllTicketItems() : Flow<TicketItem> {
        return ticketItemRepository.findAll()
    }

    override suspend fun addNewTicketType(newTicketItemDTO: NewTicketItemDTO) {
        val ticketItem = TicketItem(
            ticketType = newTicketItemDTO.type,
            price = newTicketItemDTO.price,
            minAge = newTicketItemDTO.minAge,
            maxAge = newTicketItemDTO.maxAge
        )

        try {
            ticketItemRepository.save(ticketItem)
        } catch (e: Exception) {
            throw Exception("Failed saving ticketItem: ${e.message}")
        }
    }

    override suspend fun buyTicket(mBuyTicketDTO: Mono<BuyTicketDTO>, ticketId: Long, mUserName: Mono<String>) : Long {


        val ticket = ticketItemRepository.findById(ticketId) ?: throw InvalidTicketOrderException("Ticket Not Found")

        val buyTicketDTO = mBuyTicketDTO.awaitSingle()
        val ticketPrice = ticket.price
        val totalPrice = buyTicketDTO.numOfTickets * ticketPrice
        if(ticketHasNoRestriction(ticket)){

            getTravelerInfo(mUserName)

            TODO("implement the cancellation of the other coroutine because the user can not perform the order")
        }
        val username = mUserName.awaitSingle()
        logger.info("order request received from user $username for ${buyTicketDTO.numOfTickets } ticket $ticketId" +
                "\n the user want to pay with ${buyTicketDTO.paymentInfo}" +
                "\n\t totalPrice = $totalPrice")

        val order = ticketOrderService.savePendingOrder(
            totalPrice = totalPrice,
            username = username,
            ticketId = ticketId,
            quantity = buyTicketDTO.numOfTickets

        )
        logger.info("order $order set pending")

        publishOrderOnKafka(order)

        return order.orderId ?: throw InvalidTicketOrderException("order id not saved correctly in the db")


    }

    private fun getTravelerInfo(mUserName: Mono<String>) {
        // Use this to contact the travelerService:
        // Client is a webClient (val client = WebClient.create() ??) It should be in the consturctor of the controller
//        client
//            .get()
//            .uri("/suspend")
//            .accept(MediaType.APPLICATION_JSON)
//            .awaitExchange()
//            .awaitBody<Banner>()
        TODO("Not yet implemented")
    }

    /**
     * publish on kafka the event of the pending order
     */
    private fun publishOrderOnKafka(ticketOrder: TicketOrder) {
        TODO("Not yet implemented, should push on kafka that this ticketOrder: $ticketOrder is pending")
    }

    /**
     * if the ticket has some restriction about the age or stuff like that return true, false otherwise
     */
    private fun ticketHasNoRestriction(ticket : TicketItem): Boolean {

        if(ticket.minAge == null && ticket.maxAge == null)
            return true
        else
            if(ticket.minAge != null && ticket.maxAge != null)
                if(ticket.minAge > ticket.maxAge)
                    throw InvalidTicketRestrictionException("ticket restriction is not valid, min age = ${ticket.minAge} > max age = ${ticket.maxAge}")

        return false
    }

}