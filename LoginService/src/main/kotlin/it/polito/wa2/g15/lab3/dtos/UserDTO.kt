package it.polito.wa2.g15.lab3.dtos

import it.polito.wa2.g15.lab3.entities.User
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern

data class UserRequestDTO(
    @field:NotBlank(message = "Nickname can't be empty or null")
    val nickname: String,
    
    @field:NotBlank(message = "Email can't be empty or null")
    @field:Email(regexp = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}") // without regex it allows emails like
    // a@b without final part
    val email: String,
    
    // More validations missing: password must be reasonably strong (it must not contain any whitespace, it must be at
    // least 8 characters long, it must contain at least one digit, one uppercase letter, one lowercase letter, one
    // non alpha-numeric character);
    @field:NotBlank(message = "Password can't be empty or null")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must not contain any whitespace, it must be at least 8 characters long, it must contain at " +
                "least one digit, one uppercase letter, one lowercase letter, one non alphanumeric character"
    )
    val password: String
)

fun User.toUserRequestDTO(): UserRequestDTO {
    return UserRequestDTO(nickname, email, password)
}

data class UserResponseDTO(
    val userId: Long,
    val nickname: String,
    val email: String
)

fun User.toUserResponseDTO(): UserResponseDTO {
    return UserResponseDTO(id, nickname, email)
}

data class UserLoginRequestDTO(
    @field:NotBlank(message = "Nickname can't be empty or null")
    val nickname: String,
    // More validations missing: password must be reasonably strong (it must not contain any whitespace, it must be at
    // least 8 characters long, it must contain at least one digit, one uppercase letter, one lowercase letter, one
    // non alpha-numeric character);
    @field:NotBlank(message = "Password can't be empty or null")
    @field:Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")
    val password: String
)

fun User.toUserLoginRequestDTO(): UserLoginRequestDTO {
    return UserLoginRequestDTO(nickname, password)
}

data class UserLoginResponseDTO(
        val token: String,
        //https://security.stackexchange.com/questions/108662/why-is-bearer-required-before-the-token-in-authorization-header-in-a-http-re
        //val type: String = "Bearer"
)