package it.polito.wa2.g15.lab3.unit.service.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import it.polito.wa2.g15.lab3.dtos.ActivationResponseDTO
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
import org.springframework.dao.DataIntegrityViolationException
import java.util.*

class UserServiceRegisterTest {

    private val userRepo: UserRepository = mockk()
    private var roleRepo: RoleRepository = mockk()
    private var activationRepo: ActivationRepository = mockk()
    private var emailService: EmailService = mockk()

    private var userService: UserServiceImpl = UserServiceImpl(userRepo,roleRepo,activationRepo,emailService)

    @Test
    fun singleRegistrationTest(){
        // Generate userDTO and userDTO with ID
        val userST1 = User().apply {
            this.nickname = "userS1"
            this.email = "userS1@mail.com"
            this.password = "Str0ngP@ssS1"
        }
        val userST1WithId = User().apply {
            this.nickname = "userS1"
            this.email = "userS1@mail.com"
            this.password = "Str0ngP@ssS1"
            this.id = 1
        }
        // Generate deadline
        val userST1Deadline : Calendar = Calendar.getInstance()
        userST1Deadline.add(Calendar.HOUR_OF_DAY, 12)

        // Generate activation and activation with UUID
        val userST1ActivationCode = "12"
        val userST1Activation = Activation().apply {
            this.activationCode = userST1ActivationCode
            this.activationDeadline = userST1Deadline.time
            this.user = userST1WithId
        }
        val uuid = UUID.randomUUID()
        val userST1ActivationWithUUID = Activation().apply {
            this.activationCode = userST1ActivationCode
            this.activationDeadline = userST1Deadline.time
            this.user = userST1WithId
            this.provisionalId = uuid
        }

        every { userRepo.save(userST1) } returns userST1WithId
        every { emailService.generateActivationCode() } returns userST1ActivationCode
        every { activationRepo.save(userST1Activation) } returns userST1ActivationWithUUID
        every { emailService.sendEmail(userST1ActivationWithUUID)} returns Unit
        every { roleRepo.findByName(ERole.CUSTOMER).get() } returns Role().apply{ ERole.CUSTOMER}
        Assertions.assertEquals(
            ActivationResponseDTO(uuid,userST1.email),
            userService.register(userST1.nickname, userST1.password, userST1.email),
            "unexpected activation code"
        )

        verify(exactly = 1){ userRepo.save(userST1) }
        verify(exactly = 1){ emailService.generateActivationCode() }
        verify(exactly = 1){ activationRepo.save(userST1Activation) }
        verify(exactly = 1){ emailService.sendEmail(userST1ActivationWithUUID) }
    }

    @Test
    fun multipleRegistrationTest(){
        // Generate userDTO and userDTO with ID
        val userMT1 = User().apply {
            this.nickname = "userM1"
            this.email = "userM1@mail.com"
            this.password = "Str0ngP@ssS1"
        }
        val userMT1WithId = User().apply {
            this.nickname = "userM1"
            this.email = "userM1@mail.com"
            this.password = "Str0ngP@ssS1"
            this.id = 2
        }
        // Generate deadline
        val userMT1Deadline : Calendar = Calendar.getInstance()
        userMT1Deadline.add(Calendar.HOUR_OF_DAY, 12)

        // Generate activation and activation with UUID
        val userMT1ActivationCode = "12"
        val userMT1Activation = Activation().apply {
            this.activationCode = userMT1ActivationCode
            this.activationDeadline = userMT1Deadline.time
            this.user = userMT1WithId
        }
        val uuid = UUID.randomUUID()
        val userMT1ActivationWithUUID = Activation().apply {
            this.activationCode = userMT1ActivationCode
            this.activationDeadline = userMT1Deadline.time
            this.user = userMT1WithId
            this.provisionalId = uuid
        }

        every { userRepo.save(userMT1) } returns userMT1WithId
        every { emailService.generateActivationCode() } returns userMT1ActivationCode
        every { activationRepo.save(userMT1Activation) } returns userMT1ActivationWithUUID
        every { emailService.sendEmail(userMT1ActivationWithUUID)} returns Unit
        every { roleRepo.findByName(ERole.CUSTOMER).get() } returns Role().apply{ERole.CUSTOMER}
        Assertions.assertEquals(
            ActivationResponseDTO(uuid,userMT1.email),
            userService.register(userMT1.nickname, userMT1.password, userMT1.email),
            "unexpected activation code"
        )

        verify(exactly = 1){ userRepo.save(userMT1) }
        verify(exactly = 1){ emailService.generateActivationCode() }
        verify(exactly = 1){ activationRepo.save(userMT1Activation) }
        verify(exactly = 1){ emailService.sendEmail(userMT1ActivationWithUUID) }

        // Generate userDTO and userDTO with ID
        val userMT2 = User().apply {
            this.nickname = "userM2"
            this.email = "userM2@mail.com"
            this.password = "Str0ngP@ssS1"
        }
        val userMT2WithId = User().apply {
            this.nickname = "userM2"
            this.email = "userM2@mail.com"
            this.password = "Str0ngP@ssS1"
            this.id = 3
        }
        // Generate deadline
        val userMT2Deadline : Calendar = Calendar.getInstance()
        userMT2Deadline.add(Calendar.HOUR_OF_DAY, 12)

        // Generate activation and activation with UUID
        val userMT2ActivationCode = "12"
        val userMT2Activation = Activation().apply {
            this.activationCode = userMT2ActivationCode
            this.activationDeadline = userMT2Deadline.time
            this.user = userMT2WithId
        }
        val uuid2 = UUID.randomUUID()
        val userMT2ActivationWithUUID = Activation().apply {
            this.activationCode = userMT2ActivationCode
            this.activationDeadline = userMT2Deadline.time
            this.user = userMT2WithId
            this.provisionalId = uuid2
        }

        every { userRepo.save(userMT2) } returns userMT2WithId
        every { emailService.generateActivationCode() } returns userMT2ActivationCode
        every { activationRepo.save(userMT2Activation) } returns userMT2ActivationWithUUID
        every { emailService.sendEmail(userMT2ActivationWithUUID)} returns Unit

        Assertions.assertEquals(
            ActivationResponseDTO(uuid2,userMT2.email),
            userService.register(userMT2.nickname, userMT2.password, userMT2.email),
            "unexpected activation code"
        )

        verify(exactly = 1){ userRepo.save(userMT2) }
        verify(exactly = 2){ emailService.generateActivationCode() }
        verify(exactly = 1){ activationRepo.save(userMT2Activation) }
        verify(exactly = 1){ emailService.sendEmail(userMT2ActivationWithUUID) }
    }

    @Test
    fun alreadyUsedNicknameTest(){
        val nicknameValue = "userAUN1"
        val emailValue1 = "userAUN1@email.com"
        val emailValue2 = "userAUN2@emailAUN2.com"
        val passwordValue = "secret"

        // Generate userDTO and userDTO with ID
        val user1 = User().apply {
            this.nickname = nicknameValue
            this.email = emailValue1
            this.password = passwordValue
        }
        val user1WithId = User().apply {
            this.nickname = nicknameValue
            this.email = emailValue1
            this.password = passwordValue
            this.id = 4
        }
        val user2 = User().apply {
            this.nickname = nicknameValue
            this.email = emailValue2
            this.password = passwordValue
        }
        // Generate deadline
        val user1Deadline : Calendar = Calendar.getInstance()
        user1Deadline.add(Calendar.HOUR_OF_DAY, 12)

        // Generate activation and activation with UUID
        val user1ActivationCode = "12"
        val user1Activation = Activation().apply {
            this.activationCode = user1ActivationCode
            this.activationDeadline = user1Deadline.time
            this.user = user1WithId
        }
        val uuid = UUID.randomUUID()
        val user1ActivationWithUUID = Activation().apply {
            this.activationCode = user1ActivationCode
            this.activationDeadline = user1Deadline.time
            this.user = user1WithId
            this.provisionalId = uuid
        }

        every { userRepo.save(user1) } returns user1WithId
        every { emailService.generateActivationCode() } returns user1ActivationCode
        every { activationRepo.save(user1Activation) } returns user1ActivationWithUUID
        every { emailService.sendEmail(user1ActivationWithUUID)} returns Unit
        every { roleRepo.findByName(ERole.CUSTOMER).get() } returns Role().apply{ERole.CUSTOMER}
        Assertions.assertEquals(
            ActivationResponseDTO(uuid,user1.email),
            userService.register(user1.nickname, user1.password, user1.email),
            "unexpected activation code"
        )

        verify(exactly = 1){ userRepo.save(user1) }
        verify(exactly = 1){ emailService.generateActivationCode() }
        verify(exactly = 1){ activationRepo.save(user1Activation) }
        verify(exactly = 1){ emailService.sendEmail(user1ActivationWithUUID) }

        every { userRepo.save(user1) } throws DataIntegrityViolationException("duplicate user")
        Assertions.assertThrows(RegistrationException::class.java, {
            userService.register(nicknameValue, passwordValue, emailValue1)
        },"duplicate user (nickname AND password AND email")
        verify(exactly = 2){userRepo.save(user1)}

        every { userRepo.save(user2) } throws DataIntegrityViolationException("duplicate nickname")
        Assertions.assertThrows(RegistrationException::class.java, {
            userService.register(nicknameValue, passwordValue, emailValue2)
        },"duplicate nickname and password but different email")
        verify(exactly = 1){userRepo.save(user2)}
    }

    @Test
    fun alreadyUsedEmailTest(){
        val nicknameValue1 = "userAUN1"
        val nicknameValue2 = "userAUN2"
        val emailValue = "userAUN1@email.com"
        val passwordValue = "secret"

        // Generate userDTO and userDTO with ID
        val user1 = User().apply {
            this.nickname = nicknameValue1
            this.email = emailValue
            this.password = passwordValue
        }
        val user1WithId = User().apply {
            this.nickname = nicknameValue1
            this.email = emailValue
            this.password = passwordValue
            this.id = 5
        }
        val user2 = User().apply {
            this.nickname = nicknameValue2
            this.email = emailValue
            this.password = passwordValue
        }
        // Generate deadline
        val user1Deadline : Calendar = Calendar.getInstance()
        user1Deadline.add(Calendar.HOUR_OF_DAY, 12)

        // Generate activation and activation with UUID
        val user1ActivationCode = "12"
        val user1Activation = Activation().apply {
            this.activationCode = user1ActivationCode
            this.activationDeadline = user1Deadline.time
            this.user = user1WithId
        }
        val uuid = UUID.randomUUID()
        val user1ActivationWithUUID = Activation().apply {
            this.activationCode = user1ActivationCode
            this.activationDeadline = user1Deadline.time
            this.user = user1WithId
            this.provisionalId = uuid
        }

        every { userRepo.save(user1) } returns user1WithId
        every { emailService.generateActivationCode() } returns user1ActivationCode
        every { activationRepo.save(user1Activation) } returns user1ActivationWithUUID
        every { emailService.sendEmail(user1ActivationWithUUID)} returns Unit
        every { roleRepo.findByName(ERole.CUSTOMER).get() } returns Role().apply{ERole.CUSTOMER}
        Assertions.assertEquals(
            ActivationResponseDTO(uuid,user1.email),
            userService.register(user1.nickname, user1.password, user1.email),
            "unexpected activation code"
        )

        verify(exactly = 1){ userRepo.save(user1) }
        verify(exactly = 1){ emailService.generateActivationCode() }
        verify(exactly = 1){ activationRepo.save(user1Activation) }
        verify(exactly = 1){ emailService.sendEmail(user1ActivationWithUUID) }

        every { userRepo.save(user1) } throws DataIntegrityViolationException("duplicate user")
        Assertions.assertThrows(RegistrationException::class.java, {
            userService.register(nicknameValue1, passwordValue, emailValue)
        },"duplicate user (nickname AND password AND email")
        verify(exactly = 2){userRepo.save(user1)}

        every { userRepo.save(user2) } throws DataIntegrityViolationException("duplicate email")
        Assertions.assertThrows(RegistrationException::class.java, {
            userService.register(nicknameValue2, passwordValue, emailValue)
        },"duplicate nickname and password but different email")
        verify(exactly = 1){userRepo.save(user2)}
    }
}