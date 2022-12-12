package it.polito.wa2.g15.lab4.dtos

import java.time.LocalDateTime

data class FilterDto(
    val timeStart: LocalDateTime?,
    val timeEnd: LocalDateTime?,
    val nickname: String?,
)
