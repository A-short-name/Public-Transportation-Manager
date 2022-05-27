package it.polito.wa2.g15.lab5.integration

import java.time.LocalDate
import javax.validation.constraints.NotBlank

data class UserProfileDTO(
    @field:NotBlank(message = "Name can't be empty or null")
    val name: String,

    @field:NotBlank(message = "Address can't be empty or null")
    val address: String,

    val dateOfBirth: LocalDate,

    @field:NotBlank(message = "Telephone number can't be empty or null")
    val telephoneNumber: String
)