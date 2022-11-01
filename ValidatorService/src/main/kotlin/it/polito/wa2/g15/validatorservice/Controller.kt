package it.polito.wa2.g15.validatorservice

import it.polito.wa2.g15.validatorservice.dtos.FilterDto
import it.polito.wa2.g15.validatorservice.dtos.StatisticDto
import it.polito.wa2.g15.validatorservice.services.ValidationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class Controller {

    @Autowired
    lateinit var validationService: ValidationService



    @GetMapping("/get/stats")
    fun getValidatorStats( @Valid @RequestBody filters: FilterDto) : ResponseEntity<StatisticDto>{
        val res = validationService.getStats(filters)
        return ResponseEntity<StatisticDto>(res, HttpStatus.ACCEPTED)
    }


}