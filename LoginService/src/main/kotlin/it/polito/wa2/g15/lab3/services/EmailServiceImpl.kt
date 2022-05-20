package it.polito.wa2.g15.lab3.services

import it.polito.wa2.g15.lab3.entities.Activation
import it.polito.wa2.g15.lab3.exceptions.RegistrationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.security.SecureRandom


@Service
class EmailServiceImpl : EmailService {
    @Autowired
    private val emailSender: JavaMailSender? = null
    
    @Value("\${spring.mail.username}")
    var emailSenderAddress: String = ""
    
    override fun generateActivationCode(): String {
        //Instantiate secureRandomGenerator
        val secureRandomGenerator: SecureRandom =
            SecureRandom.getInstance("SHA1PRNG", "SUN")

        //Get random integer in range
        val randInRange: Int = secureRandomGenerator.nextInt(999999)

        //Add leading zeros
        return String.format("%06d", randInRange)
    }

    override fun sendEmail(savedActivation : Activation) {
        val message = SimpleMailMessage()
        message.setFrom(emailSenderAddress)
        message.setTo(savedActivation.user.email)
        message.setSubject("Your registration at WA2-LAB3 (G15)")
        message.setText("Hello there! This is your activation code: " + savedActivation.activationCode)

        try {
            emailSender?.send(message)
        }catch(ex:MailException) {
            throw RegistrationException(ex.message ?: "Could not send email")
        }
    }

}