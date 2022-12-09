package it.polito.wa2.g15.validatorservice

import it.polito.wa2.g15.validatorservice.services.ValidationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.*
import org.springframework.stereotype.Component

@Component
class SystemSetup {


    @Autowired
    lateinit var validatorService: ValidationService


    @EventListener(ApplicationReadyEvent::class)
    fun systemConfiguration() {
        validatorService.systemConfig()
    }

}