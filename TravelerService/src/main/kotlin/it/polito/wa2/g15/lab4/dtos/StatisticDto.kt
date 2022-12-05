package it.polito.wa2.g15.lab4.dtos

import it.polito.wa2.g15.lab4.entities.TicketPurchased

data class StatisticDto(
    val purchases: List<TicketDTO>
)