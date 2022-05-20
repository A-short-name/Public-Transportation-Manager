package it.polito.wa2.g15.lab4.entities

import org.springframework.data.util.ProxyUtils
import java.io.Serializable
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class EntityBase<T: Serializable> {
    companion object{
        private const val serialVersionUID = -43869754L
    }
    @Id
    @GeneratedValue
    private var id:T? = null

    fun getId(): T? = id
    override fun toString(): String{
        return "@Entry ${this.javaClass.name}(id=$id)"
    }

    fun setId(id: T?) {
        this.id = id
    }

    override fun equals(other: Any?): Boolean {
        if(other == null) return false
        if (this === other) return true
        if (javaClass != ProxyUtils.getUserClass(other)) return false
        other as EntityBase<*>
        return id != null && this.id==other.id
    }

    override fun hashCode(): Int {
        return 42
    }

}