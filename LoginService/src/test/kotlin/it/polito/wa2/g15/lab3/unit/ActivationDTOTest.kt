package it.polito.wa2.g15.lab3.unit

import io.mockk.every
import io.mockk.mockk
import it.polito.wa2.g15.lab3.dtos.ActivationRequestDTO
import it.polito.wa2.g15.lab3.dtos.ActivationResponseDTO
import it.polito.wa2.g15.lab3.dtos.toActivationRequestDTO
import it.polito.wa2.g15.lab3.dtos.toActivationResponseDTO
import it.polito.wa2.g15.lab3.entities.Activation
import it.polito.wa2.g15.lab3.entities.User
import it.polito.wa2.g15.lab3.repositories.ActivationRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*


class ActivationDTOTest {
    
    private var userInstance: User = mockk()
    private var activationRepo: ActivationRepository = mockk()

    private val activationCode1 = "123456"
    private val email1 = "1john@doe.com"

    @Test
    fun toActivationRequestDTOTest() {

        // Generate deadline
        val userDeadline : Calendar = Calendar.getInstance()
        userDeadline.add(Calendar.HOUR_OF_DAY, 12)

        // Activation object to be compared
        val uuid = UUID.randomUUID()
        val a1 = Activation().apply {
            this.activationCode = activationCode1
            this.activationDeadline = userDeadline.time
            this.user = userInstance
            this.attempts = 5
        }
        val activationWithUUID = Activation().apply {
            this.activationCode = activationCode1
            this.activationDeadline = userDeadline.time
            this.user = userInstance
            this.provisionalId = uuid
            this.attempts = 5
        }
        every { activationRepo.save(a1) } returns activationWithUUID

        val savedActivation = activationRepo.save(a1)     //save assign a provisionalUUID to a1

        Assertions.assertEquals(
            savedActivation.toActivationRequestDTO(),
            ActivationRequestDTO(provisional_id = activationWithUUID.provisionalId!!, activation_code =
            activationWithUUID.activationCode)
        )
    }

    @Test
    fun toActivationResponseDTOTest(){
        // Generate deadline
        val userDeadline : Calendar = Calendar.getInstance()
        userDeadline.add(Calendar.HOUR_OF_DAY, 12)

        // Activation object to be compared
        val uuid = UUID.randomUUID()
        val a1 = Activation().apply {
            this.activationCode = activationCode1
            this.activationDeadline = userDeadline.time
            this.user = userInstance
            this.attempts = 5
        }
        val activationWithUUID = Activation().apply {
            this.activationCode = activationCode1
            this.activationDeadline = userDeadline.time
            this.user = userInstance
            this.provisionalId = uuid
            this.attempts = 5
        }
        every { activationRepo.save(a1) } returns activationWithUUID

        val savedActivation = activationRepo.save(a1)     //save assign a provisionalUUID to a1

        every { userInstance.email } returns email1
        Assertions.assertEquals(
                savedActivation.toActivationResponseDTO(),
                ActivationResponseDTO(provisional_id = activationWithUUID.provisionalId!!, email =
                userInstance.email)
        )
    }
}