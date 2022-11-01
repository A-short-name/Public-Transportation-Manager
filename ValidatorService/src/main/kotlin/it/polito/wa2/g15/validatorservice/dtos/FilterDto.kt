package it.polito.wa2.g15.validatorservice.dtos

import java.time.LocalDateTime
import javax.validation.constraints.NotBlank

data class FilterDto(
    val timeStart: LocalDateTime,
    val timeEnd: LocalDateTime,
    @field:NotBlank(message = "Nickname can't be empty or null")
    val nickname: String,
)
