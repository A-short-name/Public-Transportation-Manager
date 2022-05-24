package it.polito.wa2.g15.lab4.security

import io.jsonwebtoken.io.IOException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JWTAuthenticationFilter: OncePerRequestFilter() {
    @Autowired
    private lateinit var jwtUtils: JwtUtils

    @Value("\${security.header}")
    lateinit var requestHeader : String
    @Value("\${security.token.prefix}")
    lateinit var tokenPrefix : String

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        try {
            val jwt = parseHeader(request)
            if (jwt != null && jwtUtils.validateJwt(jwt)) {
                val userDetails = jwtUtils.getDetailsJwt(jwt)
                val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.roles)
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }else{
                if(jwt == null)
                    throw Exception("Invalid token: no authorization header")
                if(!jwtUtils.validateJwt(jwt))
                    throw Exception("Invalid token: problem parsing jwt")
            }
        } catch (e: Exception) {
            logger.error("Cannot set user authentication: {}", e)
        }
        filterChain.doFilter(request, response)
    }

    private fun parseHeader(request: HttpServletRequest): String? {
        val headerAuth = request.getHeader(requestHeader)
        return if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("$tokenPrefix ")) {
            headerAuth.substring(tokenPrefix.length+1, headerAuth.length)
        } else null
    }

}