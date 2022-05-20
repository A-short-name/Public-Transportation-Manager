package it.polito.wa2.g15.lab3.repositories

import it.polito.wa2.g15.lab3.entities.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository: CrudRepository<User, Long> {

    fun findByNickname(nickname: String) : Optional<User>
}