package it.polito.wa2.g15.lab5.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JWTWebFilter : WebFilter {
    @Autowired
    private lateinit var jwtUtils: JwtUtils

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val jwt = parseHeader(exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: "")

        if (jwt != null && jwtUtils.validateJwt(jwt)) {
            val userDetails = jwtUtils.getDetailsJwt(jwt)
            val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.roles)
            //authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
        }
//        else{
//            if(jwt == null)
//                throw Exception("Invalid token: no authorization header")
//            if(!jwtUtils.validateJwt(jwt))
//                throw Exception("Invalid token: problem parsing jwt")
//        }

        return chain.filter(exchange)
    }

    private fun parseHeader(headerAuth: String): String? {
        return if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            headerAuth.substring(7, headerAuth.length)
        } else null
    }
}