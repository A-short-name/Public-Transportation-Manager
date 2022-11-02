package it.polito.wa2.g15.validatorservice

import it.polito.wa2.g15.validatorservice.dtos.FilterDto
import it.polito.wa2.g15.validatorservice.dtos.StatisticDto
import it.polito.wa2.g15.validatorservice.dtos.TicketDTO
import it.polito.wa2.g15.validatorservice.services.ValidationService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
class Controller {

    @Autowired
    lateinit var validationService: ValidationService

    private val logger = KotlinLogging.logger {}


    @GetMapping("/get/stats")
    fun getValidatorStats(@Valid @RequestBody filters: FilterDto): ResponseEntity<StatisticDto> {
        val res = validationService.getStats(filters)
        return ResponseEntity<StatisticDto>(res, HttpStatus.ACCEPTED)
    }

    /**
     * It validates the ticket
     * @param userID the user associated to that ticket (for the statistics purposes)
     * @param ticket
     */
    @PutMapping("/{userID}/validate")
    fun validateTicket(
        @PathVariable("userID") userID: Long,
        @Valid @RequestBody ticket: TicketDTO
    ): ResponseEntity<Boolean> {
        return try {
            validationService.validate(userID, ticket)
            ResponseEntity(HttpStatus.ACCEPTED)
        } catch (ex: Exception) {
            ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        }
    }


}