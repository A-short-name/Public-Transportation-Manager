package it.polito.wa2.g15.lab3.security.jwt

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthEntryPointJwt : AuthenticationEntryPoint {

    //This method will be triggered anytime
    //unauthenticated User requests a secured HTTP resource and an AuthenticationException is thrown
    @Throws(IOException::class, ServletException::class)
    override fun commence(request: HttpServletRequest, response: HttpServletResponse,
                          authException: AuthenticationException) {
        if (authException is BadCredentialsException) {
            logger.error("Unauthorized error: {}", authException.message)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password")
        }
        else {
            //Finisco qui ad esempio quando l'utente è disabilitato
            logger.error("Unauthorized error: {}", authException.message)
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Error: forbidden")
        }
/*        } else if (authException instanceof JwtExpiredTokenException) {
            mapper.writeValue(
                    response.getWriter(),
                    ErrorResponse.of(
                            "Token has expired", ErrorCode.JWT_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED));
        } else if (e instanceof AuthMethodNotSupportedException) {
            mapper.writeValue(
                    response.getWriter(),
                    ErrorResponse.of(e.getMessage(), ErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (e instanceof TokenEncryptionException) {
            mapper.writeValue(
                    response.getWriter(),
                    ErrorResponse.of(e.getMessage(), ErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        } else if (e instanceof InvalidJwtAuthenticationTokenException) {
            mapper.writeValue(
                    response.getWriter(),
                    ErrorResponse.of(e.getMessage(), ErrorCode.AUTHENTICATION, HttpStatus.UNAUTHORIZED));
        }*/

    }
    companion object {
        private val logger = LoggerFactory.getLogger(AuthEntryPointJwt::class.java)
    }
}