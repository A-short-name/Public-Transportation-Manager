package it.polito.wa2.g15.lab4.services

import it.polito.wa2.g15.lab4.dtos.ExecuteCommandOnTicketsDTO
import it.polito.wa2.g15.lab4.dtos.UserProfileDTO

import it.polito.wa2.g15.lab4.dtos.TicketDTO
import it.polito.wa2.g15.lab4.dtos.UserProfileAdminViewDTO
import org.springframework.security.access.prepost.PreAuthorize

interface TravelerService {
    fun getUserDetails(username: String) : UserProfileDTO
    fun getPurchasedTicketsByUsername(username: String): Set<TicketDTO>
    fun updateUserProfile(userProfileDTO: UserProfileDTO, username: String)
    fun buyTickets(executeCommandOnTicketsDTO: ExecuteCommandOnTicketsDTO, username: String) : Set<TicketDTO>
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getListOfUsername():List<String>
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getUserById(userID: Long) : UserProfileAdminViewDTO
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getPurchasedTicketsByUserId(userID: Long) : Set<TicketDTO>
}