package it.polito.wa2.g15.lab3.services

import it.polito.wa2.g15.lab3.dtos.ActivationResponseDTO
import it.polito.wa2.g15.lab3.dtos.UserResponseDTO
import java.util.*

//Information taken from the slide
//Case study: managing user access
//This is the first version of a UserService able to manage everything concerning the user access to the system
interface UserService {
    /**
     * Checks that no other user with the same name exists, generates a random verification token and sends it as part
     * of a verification URL via email to the supplied address and persists a new User object in the repository
     */
    fun register(nickname: String, password: String, email: String): ActivationResponseDTO
    
    /**
     * Searches the User repository for the activation code and updates the corresponding User object, setting it as verified
     */
    fun verify(uuid: UUID, activationCode: String) : UserResponseDTO
    
}