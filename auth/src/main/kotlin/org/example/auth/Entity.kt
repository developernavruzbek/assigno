package org.example.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.example.auth.enums.Role
import org.example.auth.enums.UserStatus
import java.io.Serializable
import java.util.Date

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @CreatedBy var createdBy: Long? = null,
    @LastModifiedBy var lastModifiedBy: Long? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
) : Serializable


@Entity(name = "users")
class User(
    var fullName: String,
    @Column(nullable = false, unique = true)
    var phoneNumber: String,
    @Column(length = 32, unique = true, nullable = false)
    var username: String,
    @Column(nullable = false)
    var age :Long,
    @Column(length = 255)
    var password: String,
    var role: Role,
    var currentOrgId: Long,
    @Enumerated(STRING)
    @Column(length = 32)
    var status: UserStatus = UserStatus.ACTIVE
) : BaseEntity()