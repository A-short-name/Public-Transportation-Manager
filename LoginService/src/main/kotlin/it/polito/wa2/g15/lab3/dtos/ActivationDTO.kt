package it.polito.wa2.g15.lab3.dtos

import it.polito.wa2.g15.lab3.entities.Activation
import org.hibernate.validator.constraints.Length
import java.util.UUID
import javax.validation.constraints.NotBlank

data class ActivationRequestDTO(
    val provisional_id: UUID,
    
    @field:NotBlank(message = "Activation code can't be empty or null")
    @field:Length(min = 6, max = 6)
    val activation_code: String
)

fun Activation.toActivationRequestDTO(): ActivationRequestDTO {
    return ActivationRequestDTO(provisionalId!!, activationCode)
}

data class ActivationResponseDTO(
    val provisional_id: UUID,
    val email: String
)

fun Activation.toActivationResponseDTO(): ActivationResponseDTO {
    return ActivationResponseDTO(provisionalId!!, user.email)
}