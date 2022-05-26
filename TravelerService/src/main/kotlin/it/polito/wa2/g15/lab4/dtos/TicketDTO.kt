package it.polito.wa2.g15.lab4.dtos

import it.polito.wa2.g15.lab4.entities.TicketPurchased
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

data class TicketDTO(
    // The unique ticketID
    val sub: Int,

    // issuedAt, a timestamp
    val iat: Date,

    // Expiry timestamp
    val exp: Date,

    // zoneID, the set of transport zones it gives access to
    val zid: String,

    // The encoding of the previous information as a signed JWT
    val jws: String,

    val type: String,

    val validFrom: ZonedDateTime,

    val duration: Long

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TicketDTO

        if (sub != other.sub) return false
        if (iat != other.iat) return false
        if (exp != other.exp) return false
        if (zid != other.zid) return false
        if (jws != other.jws) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sub
        result = 31 * result + iat.hashCode()
        result = 31 * result + exp.hashCode()
        result = 31 * result + zid.hashCode()
        result = 31 * result + jws.hashCode()
        return result
    }
}

fun TicketPurchased.toDTO() : TicketDTO {
    return TicketDTO(getId()!!,iat,exp,zid,jws,type,validFrom,duration)
}

data class ExecuteCommandOnTicketsDTO(
    @field:NotBlank(message = "Command can't be empty or null")
    val cmd: String,

    @field:NotNull
    @field:Positive
    val quantity: Int,

    @field:NotBlank(message = "Zones can't be empty or null")
    val zones: String
)

data class TicketFromCatalogDTO (
        val duration: Long,
        @field:NotBlank(message = "Type can't be empty or null")
        val type: String,
        @field:NotNull
        val validFrom: ZonedDateTime,
        @field:NotBlank
        val zid: String,
        val quantity: Int
)