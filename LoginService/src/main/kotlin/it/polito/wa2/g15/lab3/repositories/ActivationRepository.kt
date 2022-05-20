package it.polito.wa2.g15.lab3.repositories

import it.polito.wa2.g15.lab3.entities.Activation
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ActivationRepository: CrudRepository<Activation, UUID> {
    fun findActivationByProvisionalId(provisional_id: UUID) : Optional<Activation>

    fun deleteActivationByProvisionalId(provisional_id: UUID)

    fun deleteActivationByActivationDeadlineBefore(activationDeadline: Date)

    @Query("select a from Activation a where a.activationDeadline < ?1")
    fun findAllActivationsByActivationDeadlineBefore(activationDeadline: Date) :List<Activation>
}