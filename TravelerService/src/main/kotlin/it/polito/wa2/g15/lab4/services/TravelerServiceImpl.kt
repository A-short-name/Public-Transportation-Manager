package it.polito.wa2.g15.lab4.services

import it.polito.wa2.g15.lab4.dtos.*
import it.polito.wa2.g15.lab4.entities.TicketPurchased
import it.polito.wa2.g15.lab4.entities.UserDetails
import it.polito.wa2.g15.lab4.exceptions.TravelerException
import it.polito.wa2.g15.lab4.repositories.TicketPurchasedRepository
import it.polito.wa2.g15.lab4.repositories.UserDetailsRepository
import it.polito.wa2.g15.lab4.security.JwtUtils
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class TravelerServiceImpl(val ticketPurchasedRepository : TicketPurchasedRepository,
                          val userDetailsRepository : UserDetailsRepository,
                          val jwtUtils : JwtUtils) : TravelerService {

    private val logger = KotlinLogging.logger {}

    @Value("\${security.jwtExpirationMs}")
    private lateinit var jwtExpirationMs: String

    override fun getUserDetails(username: String): UserProfileDTO {
        val userDetails = userDetailsRepository.findByUsername(username)

        if(userDetails.isEmpty) throw TravelerException("User not found")

        return userDetails.get().toDTO()
    }



    override fun getPurchasedTicketsByUsername(username: String): Set<TicketDTO> {
        val user = userDetailsRepository.findByUsername(username)

        if (user.isEmpty) throw TravelerException("No user found.")

        val ticketPurchased = user.get().ticketPurchased

        return ticketPurchased.map{ it.toDTO() }.toSet()
    }

    override fun updateUserProfile(userProfileDTO: UserProfileDTO, username: String) {
        val userProfile = userDetailsRepository.findByUsername(username)

        userProfile.ifPresent {
            logger.info("Got user from database: ${userProfile.get()}")
        }
        val newUserProfile = UserDetails(
            userProfileDTO.name,
            username,
            userProfileDTO.address,
            userProfileDTO.dateOfBirth,
            userProfileDTO.telephoneNumber
        )

        if(userProfile.isPresent) newUserProfile.setId(userProfile.get().getId())

        try {
            userDetailsRepository.save(newUserProfile)
        }catch(ex: Exception) {
            throw TravelerException("Error while updating user details")
        }
        
    }


    override fun buyTickets(executeCommandOnTicketsDTO: ExecuteCommandOnTicketsDTO, username: String) : Set<TicketDTO> {
        val result = mutableSetOf<TicketDTO>()

        val userDetails = userDetailsRepository.findByUsername(username)
        val userBuyer: UserDetails = if(userDetails.isEmpty) {
            val newUserProfile = UserDetails(
                    username= username
            )
            try {
                userDetailsRepository.save(newUserProfile)
            }catch(ex: Exception) {
                throw TravelerException("Error while creating user details")
            }
        } else{
            userDetails.get()
        }

        for(i in 0 until executeCommandOnTicketsDTO.quantity) {
            val iat = Date()
            val exp = Date(Date().time+jwtExpirationMs.toLong())
            val zones = executeCommandOnTicketsDTO.zones

            // Prepare ticket without jws
            val ticket = TicketPurchased(iat,exp,zones,"",userBuyer)

            val ticketdb : TicketPurchased

            try {
                ticketdb = ticketPurchasedRepository.save(ticket)
            }catch(ex: Exception) {
                throw TravelerException("Failed purchasing ticket")
            }

            // Generate jws and save again with jws
            val sub = ticketdb.getId()!!
            ticket.jws = jwtUtils.generateTicketJwt(sub, iat, exp, zones)

            try {
                ticketPurchasedRepository.save(ticket)
            }catch(ex: Exception) {
                throw TravelerException("Failed purchasing ticket")
            }

            userBuyer.addTicketPurchased(ticket)

            val ticketDTO = ticket.toDTO()
            result.add(ticketDTO)
        }

        return result
    }

    override fun getListOfUsername():List<String>{
        return try {
            userDetailsRepository.selectAllUsername()
        }catch(ex: Exception) {
            throw TravelerException("Failed retrieving username: ${ex.message}")
        }
    }

    override fun getUserById(userID: Long): UserProfileAdminViewDTO {
        val userDetails = userDetailsRepository.findById(userID)

        if(userDetails.isEmpty) throw TravelerException("User not found")

        return userDetails.get().toUserProfileAdminViewDTO()
    }

    override fun getPurchasedTicketsByUserId(userID: Long): Set<TicketDTO> {
        val user = userDetailsRepository.findById(userID)

        if (user.isEmpty) throw TravelerException("No user found.")

        val ticketPurchased = user.get().ticketPurchased

        return ticketPurchased.map{ it.toDTO() }.toSet()
    }
}