package it.polito.wa2.g15.lab3.services

import it.polito.wa2.g15.lab3.entities.Activation

interface EmailService {
    /**
     * Send an e-mail using information contained in application.properties
     **/
    fun sendEmail( savedActivation: Activation )
    /**
     * Generates a random activation code to be sent via e-mail.
     * @return type String - Random activation code
     **/
    fun generateActivationCode():String
}