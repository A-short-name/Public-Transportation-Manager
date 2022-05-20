package it.polito.wa2.g15.lab3.security.jwt

import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.lab3.security.services.UserDetailsImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtils {
    @Value("\${security.shared.key.login}")
    lateinit var loginStringKey: String

    @Value("\${login.jwt.expiration.ms}")
    lateinit var jwtExpirationMs: String     //1 hour in ms

    val jwtSecret: SecretKey by lazy {
        // To generate private key:
        //val key = Keys.secretKeyFor(SignatureAlgorithm.HS256)
        // val secretString = Encoders.BASE64.encode(key.encoded)
        // File(keyPath).writeText(secretString)
        // To read private key:
        //val secretString = File(keyPath).bufferedReader().use { it.readLine()}
        val decodedKey = Decoders.BASE64.decode(loginStringKey)
        Keys.hmacShaKeyFor(decodedKey)
    }
    //val expCalendar = Calendar.getInstance().add(Calendar.HOUR_OF_DAY, 1)
    //"sub", "iat", "exp", "roles"
    fun generateJwtToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserDetailsImpl
        val authorities : List<String> = userPrincipal.authorities.map { it.authority }
        return Jwts.builder()
                .setSubject(userPrincipal.username)
                .setIssuedAt(Date())
                .setExpiration(Date(Date().time + jwtExpirationMs.toInt()))
                .claim("roles", authorities)
                //check this algorithm and also what you want to return (if before you want to save a role or return directly the authoirities
                .signWith(jwtSecret)
                .compact()
    }


    //Used by the test
    fun getRolesFromJwt(authToken: String?): Set<String> {
        val jws = Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(authToken)

        @Suppress("UNCHECKED_CAST")
        val rolesSet = (jws.body["roles"] as ArrayList<String>)
        return rolesSet.toSet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JwtUtils::class.java)
    }
}