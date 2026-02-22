package org.example.organization

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.Date

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @CreatedDate @Temporal(TemporalType.TIMESTAMP)
    var createdDate: Date? = null,

    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP)
    var modifiedDate: Date? = null,

    @CreatedBy
    var createdBy: Long? = null,

    @LastModifiedBy
    var lastModifiedBy: Long? = null,

    @Column(nullable = false) @ColumnDefault(value = "false")
    var deleted: Boolean = false
)

@Entity
class Organization(
    @Column(nullable = false, unique = true, length = 150)
    var name: String,

    @Column(length = 250)
    var tagline: String,

    @Column(nullable = false, length = 36)
    var code: String,

    @Column(length = 172)
    var address: String,

    @Column(nullable = false, unique = true, length = 20)
    var phoneNumber: String,

    @Column(nullable = false)
    var active: Boolean
) : BaseEntity()



@Entity
class Employee(
    @Column(nullable = false)
    var accountId: Long,

    @ManyToOne
    var organization: Organization,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var position: Position,

    @Column(nullable = false)
    var localNumber: Long
) : BaseEntity()
