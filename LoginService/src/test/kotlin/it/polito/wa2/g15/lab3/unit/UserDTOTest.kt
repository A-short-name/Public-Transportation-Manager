package it.polito.wa2.g15.lab3.unit

import io.mockk.every
import io.mockk.mockk
import it.polito.wa2.g15.lab3.dtos.UserRequestDTO
import it.polito.wa2.g15.lab3.dtos.UserResponseDTO
import it.polito.wa2.g15.lab3.dtos.toUserRequestDTO
import it.polito.wa2.g15.lab3.dtos.toUserResponseDTO
import it.polito.wa2.g15.lab3.entities.User
import it.polito.wa2.g15.lab3.repositories.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UserDTOTest {
    
    private var userRepo: UserRepository = mockk()
    private var nickname1 = "1JohnDoe"
    private var email1 = "1john@doe.com"
    private var password1 = "aA1!validPassword"
    private var id1 = 1L

    @Test
    fun toUserRequestDTOTest() {
        val userST1 = User().apply {
            this.nickname = nickname1
            this.email = email1
            this.password = password1
        }
        val userST1WithId = User().apply {
            this.nickname = nickname1
            this.email = email1
            this.password = password1
            this.id = 1
        }

        every { userRepo.save(userST1) } returns userST1WithId

        val savedUser = userRepo.save(userST1)     //save assign an id to u1

        Assertions.assertEquals(savedUser.toUserRequestDTO(), UserRequestDTO(nickname=nickname1, email=email1,
            password=password1)
        )
    }

    @Test
    fun toUserResponseDTOTest(){
        val userST1 = User().apply {
            this.nickname = nickname1
            this.email = email1
            this.password = password1
        }
        val userST1WithId = User().apply {
            this.nickname = nickname1
            this.email = email1
            this.password = password1
            this.id = id1
        }

        every { userRepo.save(userST1) } returns userST1WithId

        val savedUser = userRepo.save(userST1)     //save assign an id to u1

        Assertions.assertEquals(savedUser.toUserResponseDTO(), UserResponseDTO(nickname=nickname1, email=email1,
                userId=id1)
        )
    }
}