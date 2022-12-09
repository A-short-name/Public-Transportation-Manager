package it.polito.wa2.g15.lab4.services

import it.polito.wa2.g15.lab4.dtos.TicketDTO
import it.polito.wa2.g15.lab4.dtos.TicketFromCatalogDTO
import it.polito.wa2.g15.lab4.dtos.UserProfileAdminViewDTO
import it.polito.wa2.g15.lab4.dtos.UserProfileDTO
import org.springframework.security.access.prepost.PreAuthorize

interface TravelerService {
    fun getUserDetails(username: String) : UserProfileDTO
    fun getPurchasedTicketsByUsername(username: String): Set<TicketDTO>
    fun getJwtPurchasedTicketByUsernameAndId(username: String, sub: Int): String
    fun updateUserProfile(userProfileDTO: UserProfileDTO, username: String)
    fun generateTickets(ticketFromCatalog: TicketFromCatalogDTO, username: String)
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getListOfUsername():List<String>
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getUserById(userID: Long) : UserProfileAdminViewDTO
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getPurchasedTicketsByUserId(userID: Long) : Set<TicketDTO>
    @PreAuthorize("hasAuthority('EMBEDDED')")
    fun getJwtTravelerPrivateKey(): String

}