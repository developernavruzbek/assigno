package org.example.notification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
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
import java.time.Instant
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
    var createdBy: String? = null,

    @LastModifiedBy
    var lastModifiedBy: String? = null,

    @Column(nullable = false)
    @ColumnDefault("false")
    var deleted: Boolean = false
)

@Entity
class TelegramConnection(
    val employeeId: Long,

    // binding data
    var chatId: Long? = null,
    var telegramUserId: Long? = null,
    var linkedAt: Instant? = null,

    // link token data
    var linkToken: String? = null,
    var tokenExpiresAt: Instant? = null,
    var tokenUsed: Boolean = false
): BaseEntity()

@Entity
class NotificationMessage(

    val employeeId: Long,

    @Column(length = 2000)
    val message: String,

    @Enumerated(EnumType.STRING)
    var status: NotificationStatus,

    val createdAt: Instant = Instant.now(),

    var sentAt: Instant? = null
) : BaseEntity()