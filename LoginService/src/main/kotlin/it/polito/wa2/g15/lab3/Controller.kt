package it.polito.wa2.g15.lab3

import it.polito.wa2.g15.lab3.dtos.*
import it.polito.wa2.g15.lab3.security.jwt.JwtUtils
import it.polito.wa2.g15.lab3.services.UserService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.function.RequestPredicates.headers
import java.util.stream.Collectors
import javax.validation.Valid


@RestController
class Controller {
    @Autowired
    lateinit var authenticationManager: AuthenticationManager

    @Autowired
    lateinit var jwtUtils: JwtUtils

    @Autowired
    lateinit var userService: UserService
    
    @Autowired
    lateinit var passwordEncoder: PasswordEncoder
    
    private val logger = KotlinLogging.logger {}
    
    @PostMapping("/user/register")
    fun registerUser(
        @Valid @RequestBody userRequestDTO: UserRequestDTO,
        bindingResult: BindingResult
    ): ResponseEntity<ActivationResponseDTO> {
        // bindingResult is automatically populated by Spring and Hibernate-validate, trying to parse a userRequestDTO which
        // respects the validation annotations in UserRequestDTO. These errors can be detected before trying to insert into
        // the db
        if (bindingResult.hasErrors()) {
            // If the json contained in the post body does not satisfy our validation annotations, return 400
            //Can be used for debugging, to extract the exact errors
            logBindingResultErrors(bindingResult)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        try {
            // N.B. in this way the password is sent encoded from the Controller to the service, which will then
            // store it encrypted and salted
            val result = userService.register(
                userRequestDTO.nickname,
                passwordEncoder.encode(userRequestDTO.password),
                userRequestDTO.email
            )
            return ResponseEntity<ActivationResponseDTO>(result, HttpStatus.ACCEPTED)
        } catch (ex: Exception) { // Uniqueness exceptions (known only after trying to insert into database)
            //the exceptions should be DataIntegrityException and RegistrationException
            logger.error { "\tRegistration not valid: ${ex.message}" }
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }
    
    @PostMapping("/user/validate")
    fun validateUser(@Valid @RequestBody activationDTO: ActivationRequestDTO, bindingResult: BindingResult):
            ResponseEntity<UserResponseDTO> {
        if (bindingResult.hasErrors()) {
            // If the json contained in the post body does not satisfy our validation annotations, return 400
            logBindingResultErrors(bindingResult)
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
        try {
            val result = userService.verify(activationDTO.provisional_id, activationDTO.activation_code)
            return ResponseEntity<UserResponseDTO>(result, HttpStatus.CREATED)
        } catch (ex: Exception) { // Uniqueness exceptions (known only after trying to insert into database)
            logger.error { "\tRegistration not valid: ${ex.message}" }
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
    
/*    @PostMapping("/user/login")
    fun loginUser(
            @Valid @RequestBody userLoginDTO: UserLoginRequestDTO,
            bindingResult: BindingResult
    ): ResponseEntity<String> {
        if (bindingResult.hasErrors()) {
            logBindingResultErrors(bindingResult)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        try {
            // N.B. in this way the password is sent encoded from the Controller to the service, which will then
            // store it encrypted and salted
            val result = userService.login(
                userLoginDTO.nickname,
                passwordEncoder.encode(userLoginDTO.password)
            )
            return ResponseEntity<String>(result, HttpStatus.ACCEPTED)
        } catch (ex: Exception) { // Uniqueness exceptions (known only after trying to insert into database)
            //the exceptions should be DataIntegrityException and RegistrationException
            logger.error { "\tRegistration not valid: ${ex.message}" }
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }*/
    //endpoint di login di prova
    @PostMapping("/user/login")
    fun authenticateUser(
             @RequestBody userLoginDTO: UserLoginRequestDTO,
            bindingResult: BindingResult
    ): ResponseEntity<UserLoginResponseDTO> {

        //Non necessario a meno che non vogliamo proteggere l'endpoint da username o password con formato scorretto
/*        if (bindingResult.hasErrors()) {
            logBindingResultErrors(bindingResult)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }*/

        val authentication: Authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(userLoginDTO.nickname, userLoginDTO.password))
        SecurityContextHolder.getContext().authentication = authentication

        val jwt: String = jwtUtils.generateJwtToken(authentication)

        //val responseHeaders = HttpHeaders().apply { setBearerAuth(jwt) }
       // return ResponseEntity<UserLoginResponseDTO>(UserLoginResponseDTO(jwt), responseHeaders, HttpStatus.OK)
        return ResponseEntity<UserLoginResponseDTO>(UserLoginResponseDTO(jwt), HttpStatus.OK)

        //Valutare se attenersi al lab o a alla buona norma: ritornare solo il token o un json con il token e i campi
/*
            val userDetails = authentication.principal
            val roles = userDetails.authorities.stream()
                .map { item: GrantedAuthority -> item.authority }
                .collect(Collectors.toList())
        return ResponseEntity.ok<Any>(JwtResponse(jwt,
                userDetails.id,
                userDetails.username,
                userDetails.email,
                roles))*/
    }
    
    fun logBindingResultErrors(bindingResult: BindingResult) {
        val errors: MutableMap<String, String?> = HashMap()
        bindingResult.allErrors.forEach { error: ObjectError ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.getDefaultMessage()
            errors[fieldName] = errorMessage
        }
        logger.debug { errors }
    }
}