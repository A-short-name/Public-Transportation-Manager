package it.polito.wa2.g15.lab3.security.services

import it.polito.wa2.g15.lab3.entities.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserDetailsImpl(
    val id: Long,
    private val username: String,
    val email: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>,
    private val isEnabled: Boolean
) : UserDetails {
    
    companion object {
        private const val serialVersionUID = 1L
        fun build(user: User): UserDetailsImpl {
            val authorities: List<GrantedAuthority> = user.roles.map { SimpleGrantedAuthority(it.name?.name) }
            //user.role.stream().map { role -> SimpleGrantedAuthority(role.getName().name()) }.collect(Collectors.toList())
            return UserDetailsImpl(
                user.id,
                user.nickname,
                user.email,
                user.password,
                authorities,
                user.active
            )
        }
    }
    
    override fun isAccountNonExpired(): Boolean {
        return true
    }
    
    override fun isAccountNonLocked(): Boolean {
        return true
    }
    
    override fun isCredentialsNonExpired(): Boolean {
        return true
    }
    
    override fun isEnabled(): Boolean {
        return isEnabled
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as UserDetailsImpl
        
        if (id != other.id) return false
        if (username != other.username) return false
        if (email != other.email) return false
        if (password != other.password) return false
        
        return true
    }
    
    override fun getAuthorities(): Collection<out GrantedAuthority> {
        return authorities
    }
    
    override fun getPassword(): String {
        return password
    }
    
    override fun getUsername(): String {
        return username
    }
}