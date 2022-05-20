package it.polito.wa2.g15.lab3.services

import it.polito.wa2.g15.lab3.dtos.ActivationResponseDTO
import it.polito.wa2.g15.lab3.dtos.UserResponseDTO
import it.polito.wa2.g15.lab3.dtos.toActivationResponseDTO
import it.polito.wa2.g15.lab3.dtos.toUserResponseDTO
import it.polito.wa2.g15.lab3.entities.Activation
import it.polito.wa2.g15.lab3.entities.ERole
import it.polito.wa2.g15.lab3.entities.User
import it.polito.wa2.g15.lab3.exceptions.RegistrationException
import it.polito.wa2.g15.lab3.repositories.ActivationRepository
import it.polito.wa2.g15.lab3.repositories.RoleRepository
import it.polito.wa2.g15.lab3.repositories.UserRepository
import mu.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.PersistenceException

@Service
@Transactional //with transactional the method won't catch the data integrity violation, look register(...) comments to know more
class UserServiceImpl(
    val userRepo: UserRepository, val roleRepo: RoleRepository, val activationRepo: ActivationRepository,
    val emailService: EmailService
) : UserService {
    
    private val logger = KotlinLogging.logger {}
    
    /*
    *   In certi casi potrebbe servire fare la flush esplicita di una transazione per committarla e poter fare andare a buon fine il catch dell'eccezione che ci si aspetta
    *   Questo succede se il service è annotato come transactional
    *   https://stackoverflow.com/questions/43707774/spring-repository-not-always-throwing-dataintegrityviolationexception
    @PersistenceContext
    lateinit var entityManager: EntityManager;
    *   Dato che ci va il transactional fuori dalla register si dovrà catturare la DataIntegrityViolationException
    * */
    
    override fun register(nickname: String, password: String, email: String): ActivationResponseDTO {
        /*
            entityManager.persist(User().apply{
                this.nickname = nickname
                this.password = password
                this.email = email})
            entityManager.flush()
        */
        
        // Create user DTO (password is already encoded)
        val userDTO = User().apply {
            this.nickname = nickname
            this.password = password
            this.email = email
        }
        try {
            userDTO.addCustomerRole(roleRepo.findByName(ERole.CUSTOMER).get())
        } catch (ex: Exception) {
            throw RegistrationException("No Customer Role found")
        }
        
        lateinit var savedUser: User
        
        //1. Store user on db
        try {
            savedUser = userRepo.save(userDTO)
        } catch (ex: PersistenceException) {
            throw RegistrationException("Db Persistence exception while saving a user")
        } catch (ex: DataIntegrityViolationException) {
            throw RegistrationException("Nickname/Email already present on the db")
        }
        
        //2. generate activation
        val activation = Activation().apply {
            activationCode = emailService.generateActivationCode()
            activationDeadline = generateActivationDeadline()
            user = savedUser
        }
        
        //3. insert activation in db
        lateinit var savedActivation: Activation
        try {
            savedActivation = activationRepo.save(activation)
            
        } catch (ex: PersistenceException) {
            throw RegistrationException("Db Persistence exception while saving a user")
        } catch (ex: DataIntegrityViolationException) {
            throw RegistrationException("Activation already present on the db")
        }
        
        //4. call to emailService function to send email
        try {
            emailService.sendEmail(savedActivation)
        } catch (ex: Exception) {
            throw RegistrationException(ex.message ?: "Could not send email")
        }
        
        // Return the provisional UUID of the saved activation as String
        //return savedActivation.toActivationValidateRequestDTO().provisionalUUID.toString()
        
        // Return the Object containing provisional UUID and email that must be the body of the Controller register
        // method response
        //ActivationResponseDTO(savedActivation.provisionalId!!, savedUser.email)
        return savedActivation.toActivationResponseDTO()
        
    }
    
    override fun verify(uuid: UUID, activationCode: String): UserResponseDTO {
        val activationReq = activationRepo.findActivationByProvisionalId(uuid)
        
        // 1. If the provisional random ID does not exist, status code 404 is returned.
        if (activationReq.isEmpty) throw RegistrationException("No provisional ID found.")
        
        // 2. If the request will be received after the expiration of the deadline, the activation
        // record will be removed from the activation table, and status code 404 will be returned.
        if (activationReq.get().activationDeadline < Calendar.getInstance().time) {
            activationRepo.deleteActivationByProvisionalId(uuid)
            //the user will be also deleted so the users have to register once again
            userRepo.deleteById(activationReq.get().user.id)
            throw RegistrationException("Time expired.")
        }
        
        // 3. If the provisional random ID exists but the activation code does not match the expected one,
        // status code 404 will be returned and the attempt counter will be decremented: if it reaches 0,
        // the activation record will be removed together with the User entry.
        if (activationReq.get().activationCode != activationCode) {
            // It updates database automatically at the end of transaction
            activationReq.get().attempts--
            
            if (activationReq.get().attempts == 0) {
                activationRepo.deleteActivationByProvisionalId(uuid)
                userRepo.deleteById(activationReq.get().user.id)
                throw RegistrationException("Attempt failed. No more attempts, register again.")
            }
            
            throw RegistrationException("Attempt failed. Try again")
        }
        
        // 4. If, within the expected time, a registration completion request will be posted,
        // the user record will be transitioned to the active state, the corresponding
        // record in the activation table will be removed, and status code 201, Created, will be returned,
        // together with the definitive UserRequestDTO.
        activationReq.get().user.active = true
        activationRepo.deleteActivationByProvisionalId(uuid)
        
        return activationReq.get().user.toUserResponseDTO()
    }
    
    private fun generateActivationDeadline(): Date {
        val tmp: Calendar = Calendar.getInstance()
        tmp.add(Calendar.HOUR_OF_DAY, 12)
        return tmp.time
    }
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds call this function
    @Async // Call this function in another thread
    fun checkDeadlines() {
        /*        val time = Calendar.getInstance().time
                logger.info { "Cleaning expired entries in activation table" }
                activationRepo.deleteActivationByActivationDeadlineBefore(time)*/
        val time = Calendar.getInstance().time
        logger.info { "Cleaning expired entries in activation table" }
        val oldActivations: List<Activation> = activationRepo.findAllActivationsByActivationDeadlineBefore(time)
        logger.info { oldActivations }
        for (activation in oldActivations) {
            userRepo.deleteById(activation.user.id)
        }
        activationRepo.deleteActivationByActivationDeadlineBefore(time)
    }
    
}