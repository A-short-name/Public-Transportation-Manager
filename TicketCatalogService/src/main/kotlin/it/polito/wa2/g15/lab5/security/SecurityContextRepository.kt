package it.polito.wa2.g15.lab5.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


/**
 * This class implements ServerSecurityContextRepository in order to get the token
 * and forward it to the AuthenticationManager.
 */
@Component
class SecurityContextRepository : ServerSecurityContextRepository {
    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    // Not implemented but it is needed anyway
    override fun save(swe: ServerWebExchange, sc: SecurityContext): Mono<Void> {
        throw UnsupportedOperationException("Not supported yet.")
    }

    override fun load(swe: ServerWebExchange): Mono<SecurityContext> {
        // Get authorization header
        return Mono.justOrEmpty(swe.request.headers.getFirst(HttpHeaders.AUTHORIZATION))
                // Check if it starts with "Bearer "
            .filter { authHeader: String -> authHeader.startsWith("Bearer ") }
                // Map it to an authentication object that contains only the token to be verified
                // This is done to forward the token to the AuthenticationManager
            .flatMap { authHeader: String ->
                val authToken = authHeader.substring(7)
                val auth: Authentication = UsernamePasswordAuthenticationToken(authToken, authToken)
                authenticationManager.authenticate(auth)
                    .map { authentication: Authentication? ->
                        SecurityContextImpl(
                            authentication
                        )
                    }
            }
    }
}