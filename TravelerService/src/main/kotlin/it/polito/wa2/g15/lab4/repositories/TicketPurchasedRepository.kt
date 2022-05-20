package it.polito.wa2.g15.lab4.repositories

import it.polito.wa2.g15.lab4.entities.TicketPurchased
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TicketPurchasedRepository : CrudRepository<TicketPurchased, Int>