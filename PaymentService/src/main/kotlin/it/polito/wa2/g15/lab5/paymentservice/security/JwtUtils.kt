package it.polito.wa2.g15.lab5.paymentservice.security


import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import it.polito.wa2.g15.lab5.paymentservice.dtos.UserDetailsDTO
import it.polito.wa2.g15.lab5.paymentservice.exceptions.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey
import kotlin.collections.ArrayList

@Component
class JwtUtils {
    @Value("\${security.privateKey.common}")
    lateinit var validateJwtStringKey : String

    @Value("\${security.privateKey.traveler}")
    lateinit var generateJwtStringKey : String


    val generateJwtKey: SecretKey by lazy {
        val decodedKey = Decoders.BASE64.decode(generateJwtStringKey)
        Keys.hmacShaKeyFor(decodedKey)
    }

    val validateJwtKey: SecretKey by lazy {
        // To generate private key:
        //val key = Keys.secretKeyFor(SignatureAlgorithm.HS256)
        // val secretString = Encoders.BASE64.encode(key.encoded)
        // File(keyPath).writeText(secretString)

        // To read private key:
        val decodedKey = Decoders.BASE64.decode(validateJwtStringKey)
        Keys.hmacShaKeyFor(decodedKey)
    }
    
    fun generateTicketJwt(sub: Int, iat: Date, exp: Date, zid: String): String {
        return Jwts.builder()
                .setSubject(sub.toString())
                .setIssuedAt(iat)
                .setExpiration(exp)
                .claim("zid", zid)
                .signWith(generateJwtKey)
                .compact()
    }

    fun getDetailsJwt(authToken: String): UserDetailsDTO {
        val claims = Jwts.parserBuilder().setSigningKey(validateJwtKey).build().parseClaimsJws(authToken)

        // We are sure that there is a set of granted authority because this method is called after validateJwt()
        @Suppress("UNCHECKED_CAST")
        val rolesSet: Set<GrantedAuthority> = (claims.body["roles"] as ArrayList<String>).map { SimpleGrantedAuthority(it)}.toSet()

        return UserDetailsDTO(sub = claims.body.subject, roles = rolesSet)
    }

    fun validateJwt(authToken: String?): Boolean {
        lateinit var jws: Jws<Claims>
        try {
            jws = Jwts.parserBuilder().setSigningKey(validateJwtKey).build().parseClaimsJws(authToken)

            val fromJSONtoSet = jws.body["roles"] as? ArrayList<*> ?: throw ValidationException("No roles collection found")


            val checkItems = fromJSONtoSet.filterIsInstance<String>()
            if (checkItems.size != fromJSONtoSet.size) throw ValidationException("No granted authority found")

            checkItems.map { SimpleGrantedAuthority(it) }.toSet()

            return true
        } catch (e: SecurityException) {
            logger.error("Invalid JWT signature: {}", e.message)
        } catch (e: MalformedJwtException) {
            logger.error("Invalid JWT token: {}", e.message)
        } catch (e: ExpiredJwtException) {
            logger.error("JWT token is expired: {}", e.message)
        } catch (e: UnsupportedJwtException) {
            logger.error("JWT token is unsupported: {}", e.message)
        } catch (e: IllegalArgumentException) {
            logger.error("JWT claims string is empty: {}", e.message)
        } catch (e: ValidationException){
            logger.error("JWT claims collection are invalid: {}", e.message)
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JwtUtils::class.java)
    }
}