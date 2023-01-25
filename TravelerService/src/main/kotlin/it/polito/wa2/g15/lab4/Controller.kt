package it.polito.wa2.g15.lab4

import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import it.polito.wa2.g15.lab4.dtos.*
import it.polito.wa2.g15.lab4.services.TravelerService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.IMAGE_PNG_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import javax.validation.Valid

@RestController
class Controller {
    @Autowired
    private lateinit var travelerService: TravelerService
    
    private val logger = KotlinLogging.logger {}


    @GetMapping("/validation/secret")
    @PreAuthorize("hasAuthority('EMBEDDED')")
    fun getValidationSecret(): ResponseEntity<String>{
        return ResponseEntity<String>(travelerService.getJwtTravelerPrivateKey(),HttpStatus.OK)
    }

    /**
     * Returns a JSON representation of the current user’s profile
     * (name, address, date_of_birth, telephone_number) as stored in the service DB.
     * @return UserProfileDTO as JSON or HttpStatus.BAD_REQUEST if user did not perform any PUT request
     */
    @GetMapping("/my/profile/")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER')")
    fun currentUserProfile(): ResponseEntity<UserProfileDTO> {
        return try {
            val principal = SecurityContextHolder.getContext().authentication.principal as UserDetailsDTO
            val username = principal.sub
    
            val result: UserProfileDTO = travelerService.getUserDetails(username)
    
            ResponseEntity<UserProfileDTO>(result, HttpStatus.OK)
        } catch (ex: Exception) {
            logger.error { "\tProfile not valid: ${ex.message}" }
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
    
    /**
     * Accepts a JSON representation of the current user’s profile and
     * updates the corresponding record in the service DB.
     * @param UserProfileDTO user profile from JSON to object
     * @param BindingResult result of validation
     */
    @PutMapping("/my/profile/")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER')")
    fun updateCurrentUserProfile(
        @Valid @RequestBody userRequestDTO: UserProfileDTO,
        bindingResult: BindingResult
    ): ResponseEntity<Unit> {
        // bindingResult is automatically populated by Spring and Hibernate-validate, trying to parse a userRequestDTO which
        // respects the validation annotations in UserRequestDTO. These errors can be detected before trying to insert into
        // the db
        if (bindingResult.hasErrors()) {
            // If the json contained in the post body does not satisfy our validation annotations, return 400
            //Can be used for debugging, to extract the exact errors
            logBindingResultErrors(bindingResult)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        
        val principal = SecurityContextHolder.getContext().authentication.principal as UserDetailsDTO
        val username = principal.sub
        
        try {
            travelerService.updateUserProfile(userRequestDTO, username)
        } catch (ex: Exception) {
            logger.error { "\tProfile not valid: ${ex.message}" }
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        
        return ResponseEntity(HttpStatus.OK)
    }
    
    /**
     * Returns a JSON list of all the tickets purchased by the current
     * user. A ticket is represented as a JSON object consisting of the fields “sub” (the
     * unique ticketID), “iat” (issuedAt, a timestamp), “exp” (expiry timestamp), “zid”
     * (zoneID, the set of transport zones it gives access to), “jws” (the encoding of the
     * previous information as a signed JWT) [Note that this JWT will be used for providing
     * physical access to the train area and will be signed by a key that has nothing to do
     * with the key used by the LoginService]
     * @return A JSON list of all the tickets purchased by the current user
     */
    @GetMapping("/my/tickets/")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER')")
    fun purchasedTicketsByCurrentUser(): ResponseEntity<Set<TicketDTO>> {
        val principal = SecurityContextHolder.getContext().authentication.principal as UserDetailsDTO
        val username = principal.sub
        
        return try {
            val result = travelerService.getPurchasedTicketsByUsername(username)
            logger.info { "tickets = $result" }
            
            ResponseEntity<Set<TicketDTO>>(result, HttpStatus.OK)
        } catch (ex: Exception) {
            logger.error { "\tError retrieving purchased tickets: ${ex.message}" }
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
    
    /**
     * Returns a png image of a QR code encoding of the JWT of the ticket with a certain sub (ticket-id). [Note that
     * this JWT will be used for providing physical access to the train area and will be signed by a key that has
     * nothing to do with the key used by the LoginService]
     * @return a png image of a QR code encoding of the JWT of the ticket with a certain sub (ticket-id)
     */
    @GetMapping("/my/tickets/{ticket-sub}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN','CUSTOMER')")
    fun purchasedTicketByCurrentUserQR(@PathVariable("ticket-sub") ticketSub: Int): ResponseEntity<ByteArrayResource> {
        val principal = SecurityContextHolder.getContext().authentication.principal as UserDetailsDTO
        val username = principal.sub
        
        val jwt = try {
            travelerService.getJwtPurchasedTicketByUsernameAndId(username, ticketSub)
        } catch (ex: Exception) {
            logger.error { "\tError retrieving purchase ticket: ${ex.message}" }
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
        logger.debug { "found jwt of ticket with id $ticketSub: $jwt" }
        val result = try {
            val qr = encodeQR(jwt, 512, 512)
            ByteArrayResource(qr, IMAGE_PNG_VALUE)
        } catch (ex: Exception) {
            logger.error { "\tError generating the QR code for the purchased ticket: ${ex.message}" }
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        
        logger.debug { "qr of the jws of the ticket with id $ticketSub: $result" }
        
        val responseHeaders = HttpHeaders()
        responseHeaders.set(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"ticket.png\""
        )
        return ResponseEntity<ByteArrayResource>(result, responseHeaders, HttpStatus.OK)
    }
    /* ADMIN ONLY endpoints */
    
    /**
     * Returns a JSON list of usernames for which there exists any
     * information (either in terms of user profile or issued tickets). THIS ENDPOINT is only
     * available for users having the Admin role.
     */
    @GetMapping("/admin/travelers/")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    fun getAllTravelers(): ResponseEntity<List<String>> {
        return try {
            SecurityContextHolder.getContext().authentication.principal as UserDetailsDTO
    
            val result: List<String> = travelerService.getListOfUsername()
    
            ResponseEntity<List<String>>(result, HttpStatus.OK)
        } catch (ex: Exception) {
            logger.error { "\tError finding all users: ${ex.message}" }
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
    
    /**
     * Returns the profile corresponding to userID.
     * THIS ENDPOINT is only available for users having the Admin role.
     * @return UserProfileDTO
     */
    @GetMapping("/admin/traveler/{userID}/profile/")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    fun getProfileFromUserID(@PathVariable("userID") userID: Long): ResponseEntity<UserProfileAdminViewDTO> {
        return try {
            SecurityContextHolder.getContext().authentication.principal as UserDetailsDTO
            
            val result: UserProfileAdminViewDTO = travelerService.getUserById(userID)
            
            ResponseEntity<UserProfileAdminViewDTO>(result, HttpStatus.OK)
        } catch (ex: Exception) {
            logger.error { "\tError finding the user with the given id: ${ex.message}" }
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
    
    /**
     * Returns the tickets owned by userID. THIS
     * ENDPOINT is only available for users having the Admin role.
     * @return List of ticketDTO
     */
    @GetMapping("/admin/traveler/{userID}/tickets/")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    fun getTicketsFromUserID(@PathVariable("userID") userID: Long): ResponseEntity<Set<TicketDTO>> {
        return try {
            SecurityContextHolder.getContext().authentication.principal as UserDetailsDTO
            
            val result: Set<TicketDTO> = travelerService.getPurchasedTicketsByUserId(userID)
            
            ResponseEntity<Set<TicketDTO>>(result, HttpStatus.OK)
        } catch (ex: Exception) {
            logger.error { "\tError finding the user with the given id: ${ex.message}" }
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
    /**
    * Currently it returns the list of ticket, like in the validatorService.
     * Decide if stats (so length of these list, or count on the db)
    * */
    @GetMapping("/stats/")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    fun getPurchaseStats(
        @RequestParam(name = "timeStart", required = false) timeStart: String?,
        @RequestParam(name = "timeEnd", required = false) timeEnd: String?,
        @RequestParam(name = "nickname", required = false) nickname: String?
    ): ResponseEntity<StatisticDto> {
        val timeStartLocalDateTime = timeStart?.let { LocalDateTime.parse(it) }
        val timeEndLocalDateTime = timeEnd?.let { LocalDateTime.parse(it) }

        return try {
            val res = travelerService.getStats(timeStartLocalDateTime, timeEndLocalDateTime, nickname)
            ResponseEntity<StatisticDto>(StatisticDto(purchases = res), HttpStatus.ACCEPTED)
        } catch (ex: Exception) {
            logger.error { "\tError finding the user with the given id: ${ex.message}" }
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
    /* END OF ADMIN ONLY endpoints */
    
    /* SERVICES ONLY endpoints */
    /**
     * Returns the birth date of the selected user’s profile
     * as stored in the service DB.
     * @return LocalDate or HttpStatus.BAD_REQUEST
     */
    @GetMapping("/services/user/{username}/birthdate/")
    @PreAuthorize("hasAuthority('SERVICE')")
    fun selectedUserProfile(@PathVariable("username") username: String): ResponseEntity<LocalDate> {
        return try {
            val result: UserProfileDTO = travelerService.getUserDetails(username)
            logger.info { "RESULT IS: $result" }
            ResponseEntity<LocalDate>(result.dateOfBirth, HttpStatus.OK)
        } catch (ex: Exception) {
            logger.error { "\tProfile not valid: ${ex.message}" }
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
    
    /**
     * Accepts a JSON payload like the following {cmd: “buy_tickets”,
     * quantity: 2, zones: “ABC”} and generates a corresponding number of tickets, issued
     * now and valid for the next hour, for the indicated transport zones. The generated
     * tickets are stored in the service DB and will be returned as a payload of the
     * response. At the moment, a user may require an unlimited number of tickets.
     * @param CommandOnTicketsDTO a JSON formatted as the object
     * @param BindingResult result of validation
     */
    @PostMapping("/services/user/{username}/tickets/add/")
    @PreAuthorize("hasAuthority('SERVICE')")
    fun generateTicketsForSelectedUser(
        @PathVariable("username") username: String,
        @Valid @RequestBody ticketDTO: TicketFromCatalogDTO,
        bindingResult: BindingResult
    ): ResponseEntity<Unit> {
        logger.info { "Mi hanno chiesto di generare per $username :$ticketDTO" }
        
        
        if (bindingResult.hasErrors()) {
            logBindingResultErrors(bindingResult)
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        
        return try {
            travelerService.generateTickets(ticketDTO, username)
            //ResponseEntity(result,HttpStatus.OK)
            ResponseEntity(HttpStatus.ACCEPTED)
        } catch (ex: Exception) {
            logger.error { "\tCannot generate tickets: ${ex.message}" }
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }
    /* END OF SERVICES ONLY endpoints */
    
    fun logBindingResultErrors(bindingResult: BindingResult) {
        val errors: MutableMap<String, String?> = HashMap()
        bindingResult.allErrors.forEach { error: ObjectError ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.getDefaultMessage()
            errors[fieldName] = errorMessage
        }
        logger.debug { errors }
    }
    
    @Throws(WriterException::class, IOException::class)
    fun encodeQR(text: String, width: Int, height: Int): ByteArray {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
        val pngOutputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream)
        return pngOutputStream.toByteArray()
    }
}