package it.polito.wa2.g15.lab3.unit.service.user

import io.mockk.every
import io.mockk.mockk
import it.polito.wa2.g15.lab3.entities.Activation
import it.polito.wa2.g15.lab3.entities.ERole
import it.polito.wa2.g15.lab3.entities.Role
import it.polito.wa2.g15.lab3.entities.User
import it.polito.wa2.g15.lab3.repositories.ActivationRepository
import it.polito.wa2.g15.lab3.repositories.RoleRepository
import it.polito.wa2.g15.lab3.repositories.UserRepository
import it.polito.wa2.g15.lab3.services.EmailService
import it.polito.wa2.g15.lab3.services.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class UserServiceCheckDeadlinesTest {
    private var userRepo: UserRepository = mockk()
    private var activationRepo: ActivationRepository = mockk()
    private var roleRepo: RoleRepository = mockk()
    private var emailService: EmailService = mockk()
    private var oldActivations: List<Activation> = mockk()
    val activation: Activation = mockk()
    
    private var userService: UserServiceImpl = UserServiceImpl(userRepo, roleRepo, activationRepo, emailService)
    
    private fun generateExpiredDeadline(): Date {
        val tmp: Calendar = Calendar.getInstance()
        tmp.add(Calendar.HOUR_OF_DAY, -12)
        return tmp.time
    }
    
    @Test
    fun `remove activation and user`() {
        val userST1WithId = User().apply {
            this.nickname = "userS1"
            this.email = "userS1@mail.com"
            this.password = "Str0ngP@ssS1"
            this.id = 1
        }
        
        //val time = Calendar.getInstance().time
        val oldTime = generateExpiredDeadline()
        every { activation.activationDeadline } returns oldTime
        val oldActivations = mutableListOf(activation)
        every { activationRepo.findAllActivationsByActivationDeadlineBefore(any()) } returns oldActivations
        every { activation.user.id } returns userST1WithId.id
        every { userRepo.deleteById(userST1WithId.id) } returns Unit
        
        every { activationRepo.deleteActivationByActivationDeadlineBefore(any()) } returns Unit
        every { roleRepo.findByName(ERole.CUSTOMER).get() } returns Role().apply { ERole.CUSTOMER }
        
        Assertions.assertDoesNotThrow({
            userService.checkDeadlines()
        }, "Activation and user not removed")
        
    }
}