package it.polito.wa2.g15.lab4.repositories

import it.polito.wa2.g15.lab4.entities.UserDetails
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserDetailsRepository : CrudRepository<UserDetails, Long> {
    fun findByUsername(username: String) : Optional<UserDetails>
    @Query("SELECT u.username FROM UserDetails u")
    fun selectAllUsername() :List<String>


}
