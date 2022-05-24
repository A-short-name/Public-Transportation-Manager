package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.BuyTicketDTO
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketOrderException
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketRestrictionException
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.kotlin.core.publisher.toMono
import kotlin.coroutines.CoroutineContext

@Service
class TicketCatalogServiceImpl : TicketCatalogService {
    @Autowired
    lateinit var ticketItemRepository: TicketItemRepository

    @Autowired
    lateinit var ticketOrderService: TicketOrderService

    private val logger = KotlinLogging.logger {}

    override fun getAllTicketItems() : Flow<TicketItem> {
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

    private fun checkRestriction(userAge: Int, ticketRequested: TicketItem): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun buyTicket(buyTicketDTO: BuyTicketDTO, ticketId: Long, userName: String) : Long
     = coroutineScope()
    {
        val ctx : CoroutineContext = Dispatchers.IO

        val ticketRequested : TicketItem
        logger.info("ctx: ${this.coroutineContext.job} \t start buying info ")
        val ticketFuture =
            withContext(Dispatchers.IO + CoroutineName("find ticket")) {
                logger.info("ctx:  ${this.coroutineContext.job} \t searching ticket info")
                ticketItemRepository.findById(ticketId) ?: throw InvalidTicketOrderException("Ticket Not Found")
        }

        ticketRequested = ticketFuture
        if(ticketHasRestriction(ticketRequested)){
            val travelerAge =
                //async(Dispatcher.IO + CoroutineName("find user age")
                withContext(Dispatchers.IO + CoroutineName("find user age")) {
                    logger.info("ctx:  ${this.coroutineContext.job} \t searching user age")
                    getTravelerAge(userName)
            }
            if ( ! checkRestriction(travelerAge,ticketRequested))
                throw InvalidTicketRestrictionException("User $userName is $travelerAge years old and can not buy" +
                        " ticket $ticketId")

        }

        val ticketPrice = ticketRequested.price
        val totalPrice = buyTicketDTO.numOfTickets * ticketPrice

        logger.info("ctx: ${this.coroutineContext.job}\t order request received from user $userName for ${buyTicketDTO.numOfTickets } ticket $ticketId" +
                "\n the user want to pay with ${buyTicketDTO.paymentInfo}" +
                "\n\t totalPrice = $totalPrice")

        val order = withContext(Dispatchers.IO + CoroutineName("save pending order")){

            ticketOrderService.savePendingOrder(
                totalPrice = totalPrice,
                username = userName,
                ticketId = ticketId,
                quantity = buyTicketDTO.numOfTickets
            )
        }
        logger.info("order $order set pending")

        publishOrderOnKafka(order)


        order.orderId ?: throw InvalidTicketOrderException("order id not saved correctly in the db")


    }

    private fun getTravelerAge(userName: String) : Int {
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
    private fun ticketHasRestriction(ticket : TicketItem): Boolean {

        if(ticket.minAge == null && ticket.maxAge == null)
            return false
        else
            if(ticket.minAge != null && ticket.maxAge != null)
                if(ticket.minAge > ticket.maxAge)
                    throw InvalidTicketRestrictionException("ticket restriction is not valid, min age = ${ticket.minAge} > max age = ${ticket.maxAge}")

        return true
    }

}