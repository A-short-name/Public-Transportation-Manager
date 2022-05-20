package it.polito.wa2.g15.lab3.entities

import javax.persistence.*

@Entity
@Table(name = "users")
class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: Long = 0L
    
    @Column(unique = true)
    var nickname: String = ""
    
    @Column(unique = true)
    var email: String = ""
    
    var password: String = ""
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    // By default the Role() that we create is a customer, this can be c
    // When a user is deleted, also its roles from the user_roles table are automatically deleted
    var roles: MutableSet<Role> = hashSetOf()
    
    var active: Boolean = false
    
    //https://www.baeldung.com/jpa-one-to-one#spk-model
    @OneToOne(mappedBy = "user")
    @PrimaryKeyJoinColumn
    private val activation: Activation? = null
    
    // equals and hashCode are needed for mockk tests
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as User
        
        if (id != other.id) return false
        if (nickname != other.nickname) return false
        if (email != other.email) return false
        if (password != other.password) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + nickname.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + password.hashCode()
        return result
    }
    
    fun addCustomerRole(role: Role) {
        roles.add(role)
        // If we need to add a list of all the users with a certain role we just need to uncomment the next line and
        // add the corresponding list of users to the Role Entity.
        //Role.users().add(this);
    }
    
}