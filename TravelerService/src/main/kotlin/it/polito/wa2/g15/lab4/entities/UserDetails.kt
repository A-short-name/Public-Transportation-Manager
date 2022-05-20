package it.polito.wa2.g15.lab4.entities

import java.time.LocalDate
import javax.persistence.*

@Entity
@Table(name = "user_details")
class UserDetails(
    
    var name: String="",
    
    @Column(unique = true)
    val username: String,
    
    var address: String="",
    
    @Column(name = "date_of_birth")
    var dateOfBirth: LocalDate = LocalDate.now(),
    
    @Column(name = "telephone_number", unique = true)
    var telephoneNumber: String="",
    
    @OneToMany(mappedBy = "user")
    @PrimaryKeyJoinColumn
    @Column(name = "ticket_purchased")
    val ticketPurchased: MutableSet<TicketPurchased> = mutableSetOf(),
    ): EntityBase<Long>(){

    /**
     * this function must be used to add tickets into the ticketPurchased
     * otherwise the entities won't be consistent
     * @t ticket purchased of this user
     */
    fun addTicketPurchased(t: TicketPurchased){
        t.user = this
        ticketPurchased.add(t)
    }
}
