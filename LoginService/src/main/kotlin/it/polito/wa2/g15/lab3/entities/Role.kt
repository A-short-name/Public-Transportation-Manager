package it.polito.wa2.g15.lab3.entities

import javax.persistence.*

enum class ERole {
    CUSTOMER, ADMIN
}

@Entity
@Table(name = "roles")
class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0
    
    @Enumerated(EnumType.STRING)
    @Column(unique = true, length = 20)
    var name: ERole? = null
    
}