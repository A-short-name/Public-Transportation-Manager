package it.polito.wa2.g15.lab3.unit.service.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import it.polito.wa2.g15.lab3.dtos.ActivationRequestDTO
import it.polito.wa2.g15.lab3.dtos.toUserResponseDTO
import it.polito.wa2.g15.lab3.entities.Activation
import it.polito.wa2.g15.lab3.entities.ERole
import it.polito.wa2.g15.lab3.entities.Role
import it.polito.wa2.g15.lab3.entities.User
import it.polito.wa2.g15.lab3.exceptions.RegistrationException
import it.polito.wa2.g15.lab3.repositories.ActivationRepository
import it.polito.wa2.g15.lab3.repositories.RoleRepository
import it.polito.wa2.g15.lab3.repositories.UserRepository
import it.polito.wa2.g15.lab3.services.EmailService
import it.polito.wa2.g15.lab3.services.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class UserServiceVerifyTest {
    private var userRepo: UserRepository = mockk()
    private var roleRepo: RoleRepository = mockk()
    private var activationRepo: ActivationRepository = mockk()
    private var emailService: EmailService = mockk()

    private var userService: UserServiceImpl = UserServiceImpl(userRepo,roleRepo,activationRepo,emailService)

    @Test
    fun invalidProvisionalIdTest() {
        // Define ValidationDTO
        val validation = ActivationRequestDTO(UUID.randomUUID(),"123")
        val uuid = validation.provisional_id

        every { activationRepo.findActivationByProvisionalId(uuid) } returns Optional.empty()
        every { activationRepo.deleteActivationByProvisionalId(uuid) } returns Unit

        Assertions.assertThrows(RegistrationException::class.java, {
            userService.verify(uuid,validation.activation_code)
        },"Wrong provisional id (not found)")

        verify(exactly = 1){activationRepo.findActivationByProvisionalId(uuid)}
        verify(exactly = 0){activationRepo.deleteActivationByProvisionalId(uuid)}
    }

    @Test
    fun expiredDeadlineTest() {
        val activationCode = "123456"

        // Define ValidationDTO with activationCode
        val validation = ActivationRequestDTO(UUID.randomUUID(), activationCode)

        // Define User
        val userWithId = User().apply {
            this.nickname = "userS1"
            this.email = "userS1@mail.com"
            this.password = "Str0ngP@ssS1"
            this.id = 1
        }

        // Generate expired deadline
        val expiredUserDeadline : Calendar = Calendar.getInstance()
        expiredUserDeadline.add(Calendar.HOUR_OF_DAY, -12)

        // Define Activation object
        val uuid = validation.provisional_id
        val userActivationWithUUID = Activation().apply {
            this.activationCode = activationCode
            this.activationDeadline = expiredUserDeadline.time
            this.user = userWithId
            this.provisionalId = uuid
            this.attempts = 5
        }

        every { activationRepo.findActivationByProvisionalId(uuid) } returns Optional.of(userActivationWithUUID)
        every { activationRepo.deleteActivationByProvisionalId(uuid) } returns Unit
        every { userRepo.deleteById(userWithId.id) } returns Unit
        every { roleRepo.findByName(ERole.CUSTOMER).get() } returns Role().apply{ERole.CUSTOMER}
        
        Assertions.assertThrows(RegistrationException::class.java, {
            userService.verify(uuid,validation.activation_code)
        },"Expired deadline")

        verify(exactly = 1){activationRepo.findActivationByProvisionalId(uuid)}
        verify(exactly = 1){activationRepo.deleteActivationByProvisionalId(uuid)}
        verify(exactly = 1){userRepo.deleteById(userWithId.id)}
    }

    @Test
    fun wrongActivationCodeWith5AttemptsTest() {
        val correctActivationCode = "123456"
        val wrongActivationCode = "123321"

        // Define ValidationDTO with wrong activationCode
        val validation = ActivationRequestDTO(UUID.randomUUID(), wrongActivationCode)

        // Define User
        val userWithId = User().apply {
            this.nickname = "userS1"
            this.email = "userS1@mail.com"
            this.password = "Str0ngP@ssS1"
            this.id = 2
        }

        // Generate deadline
        val userDeadline : Calendar = Calendar.getInstance()
        userDeadline.add(Calendar.HOUR_OF_DAY, 12)

        // Define Activation object
        val uuid = validation.provisional_id
        val userActivationWithUUID = Activation().apply {
            this.activationCode = correctActivationCode
            this.activationDeadline = userDeadline.time
            this.user = userWithId
            this.provisionalId = uuid
            this.attempts = 5
        }

        every { activationRepo.findActivationByProvisionalId(uuid) } returns Optional.of(userActivationWithUUID)
        every { activationRepo.deleteActivationByProvisionalId(uuid) } returns Unit
        every { userRepo.deleteById(userWithId.id) } returns Unit

        Assertions.assertThrows(RegistrationException::class.java, {
            userService.verify(uuid,validation.activation_code)
        },"Wrong activation code (does not match)")

        verify(exactly = 1){activationRepo.findActivationByProvisionalId(uuid)}
        verify(exactly = 0){activationRepo.deleteActivationByProvisionalId(uuid)}
        verify(exactly = 0){userRepo.deleteById(userWithId.id)}

    }

    @Test
    fun wrongActivationCodeWith1AttemptsTest() {
        val correctActivationCode = "123456"
        val wrongActivationCode = "123321"

        // Define ValidationDTO with wrong activationCode
        val validation = ActivationRequestDTO(UUID.randomUUID(), wrongActivationCode)

        // Define User
        val userWithId = User().apply {
            this.nickname = "userS1"
            this.email = "userS1@mail.com"
            this.password = "Str0ngP@ssS1"
            this.id = 3
        }

        // Generate deadline
        val userDeadline : Calendar = Calendar.getInstance()
        userDeadline.add(Calendar.HOUR_OF_DAY, 12)

        // Define Activation object
        val uuid = validation.provisional_id
        val userActivationWithUUID = Activation().apply {
            this.activationCode = correctActivationCode
            this.activationDeadline = userDeadline.time
            this.user = userWithId
            this.provisionalId = uuid
            this.attempts = 1
        }

        every { activationRepo.findActivationByProvisionalId(uuid) } returns Optional.of(userActivationWithUUID)
        every { activationRepo.deleteActivationByProvisionalId(uuid) } returns Unit
        every { userRepo.deleteById(userWithId.id) } returns Unit
        every { roleRepo.findByName(ERole.CUSTOMER).get() } returns Role().apply{ ERole.CUSTOMER}
        
        Assertions.assertThrows(RegistrationException::class.java, {
            userService.verify(uuid,validation.activation_code)
        },"Wrong activation code (does not match)")

        verify(exactly = 1){activationRepo.findActivationByProvisionalId(uuid)}
        verify(exactly = 1){activationRepo.deleteActivationByProvisionalId(uuid)}
        verify(exactly = 1){userRepo.deleteById(userWithId.id)}
    }

    @Test
    fun validVerifyTest() {
        val activationCode = "123456"

        // Define ValidationDTO with activationCode
        val validation = ActivationRequestDTO(UUID.randomUUID(), activationCode)

        // Define User
        val userWithId = User().apply {
            this.nickname = "userS1"
            this.email = "userS1@mail.com"
            this.password = "Str0ngP@ssS1"
            this.id = 4
        }

        // Result to be compared
        val result = userWithId.toUserResponseDTO()

        // Generate deadline
        val userDeadline : Calendar = Calendar.getInstance()
        userDeadline.add(Calendar.HOUR_OF_DAY, 12)

        // Define Activation object
        val uuid = validation.provisional_id
        val userActivationWithUUID = Activation().apply {
            this.activationCode = activationCode
            this.activationDeadline = userDeadline.time
            this.user = userWithId
            this.provisionalId = uuid
            this.attempts = 5
        }

        every { activationRepo.findActivationByProvisionalId(uuid) } returns Optional.of(userActivationWithUUID)
        every { activationRepo.deleteActivationByProvisionalId(uuid) } returns Unit
        every { userRepo.deleteById(userWithId.id) } returns Unit

        Assertions.assertEquals(result,
            userService.verify(uuid,validation.activation_code)
        ,"User validated")

        verify(exactly = 1){activationRepo.findActivationByProvisionalId(uuid)}
        verify(exactly = 1){activationRepo.deleteActivationByProvisionalId(uuid)}
        verify(exactly = 0){userRepo.deleteById(userWithId.id)}
    }
}