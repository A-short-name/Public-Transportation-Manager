package it.polito.wa2.g15.lab3.entities

import org.hibernate.annotations.GenericGenerator
import org.hibernate.validator.constraints.Range
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "activations")
class Activation {
    companion object{
        const val MAX_ATTEMPTS=5
    }
    @Id //@Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    var provisionalId: UUID? = null

    var activationCode: String = ""

    @field:Range(min = 0, max = 5)
    var attempts: Int = MAX_ATTEMPTS

    // https://vladmihalcea.com/date-timestamp-jpa-hibernate/
    // https://www.baeldung.com/spring-data-jpa-query-by-date#test-1
    @Temporal(TemporalType.TIMESTAMP)
    var activationDeadline: Date = Date()

    @OneToOne
    @JoinColumn(name = "user_id")
    lateinit var user: User

    // equals and hashCode are needed for mockk tests
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Activation

        if (provisionalId != other.provisionalId) return false
        if (activationCode != other.activationCode) return false
        if (user != other.user) return false
        if (attempts != other.attempts) return false

        return true
    }

    override fun hashCode(): Int {
        var result = provisionalId.hashCode()
        result = 31 * result + activationCode.hashCode()
        result = 31 * result + user.hashCode()
        result = 31 * result + attempts.hashCode()
        return result
    }
}