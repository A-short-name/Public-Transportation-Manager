package it.polito.wa2.g15.validatorservice.entities

import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "ticket_validated")
class TicketValidation(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val validationId: Long? = null,

    // username used during ticket validation
    @Column(nullable = false)
    var username: String = "",

    // when the ticket has been validated
    @NotNull
    var validationTime: LocalDateTime,

    @NotNull
    var ticketId: Int
)