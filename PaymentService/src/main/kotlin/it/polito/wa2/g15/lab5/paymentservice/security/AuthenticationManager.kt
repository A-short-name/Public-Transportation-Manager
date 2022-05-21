package it.polito.wa2.g15.lab5.paymentservice.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono


/**
 * This class implements ReactiveAuthenticationManager to validate token and role.
 * It also returns a Mono of the Authentication object containing userDetails and Roles.
 */
@Component
class AuthenticationManager : ReactiveAuthenticationManager {
    @Autowired
    private lateinit var jwtUtils: JwtUtils

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val authToken: String = authentication.credentials.toString()

        return Mono.just(jwtUtils.validateJwt(authToken)) // Validate token
            .filter{it == true} // Filter it only if it is true
            .switchIfEmpty(Mono.empty()) // If not, the mono will be empty
            .map{ // I'm here only if true and will map it to an authentication object with userDetails
                val userDetails = jwtUtils.getDetailsJwt(authToken)
                println("User is: ${userDetails.sub}")
                UsernamePasswordAuthenticationToken(userDetails, authToken, userDetails.roles)
            }
    }
}