package it.polito.wa2.g15.validatorservice.entities

import java.time.ZonedDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "ticket_validated")
class TicketValidation(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val validationId: Long? = null,
    @Column(nullable = false)
    var username: String = "",
    @NotNull
    var validationTime: ZonedDateTime
)