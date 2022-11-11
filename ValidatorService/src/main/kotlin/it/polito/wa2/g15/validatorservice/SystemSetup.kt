package it.polito.wa2.g15.validatorservice

import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class SystemSetup {
    private val logger = KotlinLogging.logger {}

    @EventListener(ApplicationReadyEvent::class)
    fun systemConfiguration() {
        logger.info("Contacting Login Service to perform authentication...")

        val bool = false;
        // TODO: Call login service's API
        // Validatrice logga nel LoginService con l'api /user/login e riceve il ruolo di embedded system

        // TODO: Contact Traveler Service's API
        // Contatta il TravelerService con il ruolo di embedded system e chiede il segreto per
        // validare i biglietti ad una nuova api /secret/get

        if(bool)
            logger.info("Successfully authenticated as Embedded user and secret received")
        else
            logger.error("Failed to perform authentication. Not implemented!")
    }

}