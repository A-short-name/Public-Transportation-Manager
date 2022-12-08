package it.polito.wa2.g15.validatorservice.dtos

import it.polito.wa2.g15.validatorservice.entities.TicketValidation

data class StatisticDto(
    val validations: List<TicketValidation> = emptyList()
)