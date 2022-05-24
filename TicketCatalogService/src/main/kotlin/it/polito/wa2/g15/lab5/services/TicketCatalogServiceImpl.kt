package it.polito.wa2.g15.lab5.services

import it.polito.wa2.g15.lab5.dtos.BuyTicketDTO
import it.polito.wa2.g15.lab5.dtos.NewTicketItemDTO
import it.polito.wa2.g15.lab5.entities.TicketItem
import it.polito.wa2.g15.lab5.entities.TicketOrder
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketOrderException
import it.polito.wa2.g15.lab5.exceptions.InvalidTicketRestrictionException
import it.polito.wa2.g15.lab5.repositories.TicketItemRepository
import it.polito.wa2.g15.lab5.security.JwtUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext


@Service
class TicketCatalogServiceImpl : TicketCatalogService {
    @Autowired
    lateinit var ticketItemRepository: TicketItemRepository

    @Autowired
    lateinit var ticketOrderService: TicketOrderService

    @Autowired
    lateinit var jwtUtils: JwtUtils

    private val logger = KotlinLogging.logger {}

    val client = WebClient.builder()
            .baseUrl("http://localhost:8080")
            //.defaultCookie("cookieKey", "cookieValue")
            .defaultHeaders(httpHeaders())
            .defaultUriVariables(Collections.singletonMap("url", "http://localhost:8080"))
            .build()

    private fun httpHeaders(): Consumer<HttpHeaders> {
        return Consumer<HttpHeaders> { headers ->
            headers.contentType = MediaType.APPLICATION_JSON
            headers.setBearerAuth(jwtUtils.generateJwtToken())
            headers.set(HttpHeaders.ACCEPT_ENCODING, MediaType.APPLICATION_JSON_VALUE)
        }
    }

    override fun getAllTicketItems(): Flow<TicketItem> {
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

    override suspend fun buyTicket(buyTicketDTO: BuyTicketDTO, ticketId: Long, userName: String): Long = coroutineScope()
    {
        val ctx: CoroutineContext = Dispatchers.IO

        logger.info("ctx: ${this.coroutineContext.job} \t start buying info ")
        val ticketRequested =
                withContext(Dispatchers.IO + CoroutineName("find ticket")) {
                    logger.info("ctx:  ${this.coroutineContext.job} \t searching ticket info")
                    ticketItemRepository.findById(ticketId) ?: throw InvalidTicketOrderException("Ticket Not Found")
                }

        if (ticketHasRestriction(ticketRequested)) {
            val travelerAge =
                    //async(Dispatcher.IO + CoroutineName("find user age")
                    withContext(Dispatchers.IO + CoroutineName("find user age")) {
                        logger.info("ctx:  ${this.coroutineContext.job} \t searching user age")
                        getTravelerAge(userName)
                    }
            if (!checkRestriction(travelerAge, ticketRequested))
                throw InvalidTicketRestrictionException("User $userName is $travelerAge years old and can not buy" +
                        " ticket $ticketId")

        }

        val ticketPrice = ticketRequested.price
        val totalPrice = buyTicketDTO.numOfTickets * ticketPrice

        logger.info("ctx: ${this.coroutineContext.job}\t order request received from user $userName for ${buyTicketDTO.numOfTickets} ticket $ticketId" +
                "\n the user want to pay with ${buyTicketDTO.paymentInfo}" +
                "\n\t totalPrice = $totalPrice")

        val order = withContext(Dispatchers.IO + CoroutineName("save pending order")) {

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

    private suspend fun getTravelerAge(userName: String): Int {
        val age = client.get()
                .uri("/services/user/$userName/profile/")
                .awaitExchange {
                    if (it.statusCode() != HttpStatus.OK)
                        throw InvalidTicketRestrictionException("User info not found")
                    LocalDate.now()
                    ChronoUnit.YEARS.between(it.bodyToMono<LocalDate>().awaitSingle(), LocalDate.now())
                }
        logger.info { "User ($userName) age is: $age" }
        return age.toInt()
    }


    /**
     * publish on kafka the event of the pending order
     */
    private fun publishOrderOnKafka(ticketOrder: TicketOrder) {
        TODO("Not yet implemented, should push on kafka that this ticketOrder: $ticketOrder is pending")
    }

    /**
     * consume kafka event that confirm success of a pending order
     */
    private fun consumeOrderOnKafka(ticketOrder: TicketOrder) {
        TODO("Not yet implemented, should retrieve the message from kafka that this ticketOrder: $ticketOrder has been payed" +
                "Then set as completed the order and call the buy ticket of the traveler service")

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