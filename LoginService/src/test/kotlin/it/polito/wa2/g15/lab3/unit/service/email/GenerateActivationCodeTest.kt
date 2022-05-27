package it.polito.wa2.g15.lab3.unit.service.email

import it.polito.wa2.g15.lab3.services.EmailServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GenerateActivationCodeTest {
    val emailService = EmailServiceImpl()

    @Test
    fun checkLengthOfActivationCode() {
        Assertions.assertTrue(emailService.generateActivationCode().length == 6 )
    }

    @Test
    fun checkFormatOfActivationCode() {
        Assertions.assertTrue(emailService.generateActivationCode().all { it.isDigit() } )
    }
}