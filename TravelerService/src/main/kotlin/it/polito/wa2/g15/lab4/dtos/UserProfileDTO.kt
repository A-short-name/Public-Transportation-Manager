package it.polito.wa2.g15.lab4.dtos

import it.polito.wa2.g15.lab4.entities.UserDetails
import org.springframework.security.core.GrantedAuthority
import java.time.LocalDate
import javax.validation.constraints.NotBlank

data class UserProfileDTO(
    @field:NotBlank(message = "Name can't be empty or null")
    val name: String,
    
    @field:NotBlank(message = "Address can't be empty or null")
    val address: String,
    
    val dateOfBirth: LocalDate,
    
    @field:NotBlank(message = "Telephone number can't be empty or null")
    val telephoneNumber: String
)

fun UserDetails.toDTO(): UserProfileDTO {
    return UserProfileDTO(
        name = name,
        address = address,
        dateOfBirth = dateOfBirth,
        telephoneNumber = telephoneNumber
    )
}

data class UserProfileAdminViewDTO(
    val name: String,
    val username: String,
    val address: String,
    val dateOfBirth: LocalDate,
    val telephoneNumber: String,
){
    //Used in test
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as UserProfileAdminViewDTO
        return  name==other.name                     &&
                username==other.username            &&
                address == other.address            &&
                dateOfBirth == other.dateOfBirth    &&
                telephoneNumber == other.telephoneNumber
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + dateOfBirth.hashCode()
        result = 31 * result + telephoneNumber.hashCode()
        return result
    }

}

fun UserDetails.toUserProfileAdminViewDTO(): UserProfileAdminViewDTO {
    return UserProfileAdminViewDTO(
        name = name,
            username = username,
            address = address,
            dateOfBirth = dateOfBirth,
            telephoneNumber = telephoneNumber,
    )
}

data class UserDetailsDTO(
        val sub: String,
        val roles: Set<GrantedAuthority>
)