package it.polito.wa2.g15.lab5.dtos

import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

data class BuyTicketDTO (

    @field:NotNull
    @field:Positive
    val numOfTickets: Int,

    @field:NotNull
    @field:Positive
    val paymentInfo: PaymentInfo,

    /*
    *   L'utente dovrà inserire l'informazione valid from nel caso abbia selezionato un ticket
    *   con tipologia abbonamento (qualunque). Altrimenti verrà cancellato l'acquisto o verrà
    *   scelta la data corrente come startingDate
    * */
    val validFrom: LocalDate? = null,
    /* Possibili scelte implementative:
    *  -L'utente seleziona un certo item da catalogo (es. biglietto ordinario)
    *   Nel body inserisce le informazioni sulle zone per cui deve essere valevole il biglietto.
    *   Nel service calcoliamo il prezzo finale come price dell'item * numero di zone
    *  -Zone diverse rappresentano item diversi nel catalogo. Il price sarà diverso,
    *   l'utente dovrà specificare solo l'id del ticket
    * */
    val zid: String
    )



data class PaymentInfo(
    @field:NotBlank
    val creditCardNumber: String,

    val exp: LocalDate,

    @field:NotBlank
    val csv: String,

    )

data class TicketForTravelerDTO (
        @field:NotNull
        @field:Positive
        val ticketItemId: Long,

        @field:NotBlank(message = "Type can't be empty or null")
        val type: String? = null,

        //Present only for subscriptions
        val validFrom: LocalDate? = null,

        //Present only for standard ticket
        val zid: String? = null
)