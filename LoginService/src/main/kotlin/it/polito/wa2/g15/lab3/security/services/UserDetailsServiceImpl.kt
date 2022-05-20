package it.polito.wa2.g15.lab3.security.services

import it.polito.wa2.g15.lab3.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl: UserDetailsService {
    
    @Autowired
    lateinit var userRepository: UserRepository
    
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails{
        val user = userRepository.findByNickname(username).orElseThrow { UsernameNotFoundException("User Not Found " +
                "with username: " + username) }
        return UserDetailsImpl.build(user)
    }
    
}