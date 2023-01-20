package it.polito.wa2.g15.lab4.services

import it.polito.wa2.g15.lab4.dtos.TicketDTO
import it.polito.wa2.g15.lab4.dtos.TicketFromCatalogDTO
import it.polito.wa2.g15.lab4.dtos.UserProfileAdminViewDTO
import it.polito.wa2.g15.lab4.dtos.UserProfileDTO
import org.springframework.security.access.prepost.PreAuthorize
import java.time.LocalDateTime

interface TravelerService {
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER','SERVICE')")
    fun getUserDetails(username: String): UserProfileDTO
    
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER')")
    fun getPurchasedTicketsByUsername(username: String): Set<TicketDTO>
    
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER')")
    fun getJwtPurchasedTicketByUsernameAndId(username: String, sub: Int): String
    
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER')")
    fun updateUserProfile(userProfileDTO: UserProfileDTO, username: String)
    
    @PreAuthorize("hasAuthority('SERVICE')")
    fun generateTickets(ticketFromCatalog: TicketFromCatalogDTO, username: String)
    
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    fun getListOfUsername(): List<String>
    
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    fun getUserById(userID: Long): UserProfileAdminViewDTO
    
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    fun getPurchasedTicketsByUserId(userID: Long): Set<TicketDTO>
    
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    fun getStats(timeStart: LocalDateTime?, timeEnd: LocalDateTime?, nickname: String?): List<TicketDTO>
    @PreAuthorize("hasAuthority('EMBEDDED')")
    fun getJwtTravelerPrivateKey(): String
}