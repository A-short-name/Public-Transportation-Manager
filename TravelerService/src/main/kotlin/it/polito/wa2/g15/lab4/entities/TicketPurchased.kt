package it.polito.wa2.g15.lab4.entities

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "ticket_purchased")
class TicketPurchased(
    
    // issuedAt, a timestamp
    @Temporal(TemporalType.TIMESTAMP)
    val iat: Date,
    
    // Expiry timestamp
    @Temporal(TemporalType.TIMESTAMP)
    val exp: Date,
    
    // zoneID, the set of transport zones it gives access to
    val zid: String,
    
    // The encoding of the previous information as a signed JWT
    var jws: String,
    
    @ManyToOne
    var user: UserDetails
) : EntityBase<Int>()
