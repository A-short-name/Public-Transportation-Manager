package it.polito.wa2.g15.lab4.entities

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
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
    var user: UserDetails,

        var type: String,

        var validFrom: ZonedDateTime,

        var duration: Long
) : EntityBase<Int>()
