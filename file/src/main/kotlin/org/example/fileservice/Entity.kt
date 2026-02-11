package uz.zero.fileservice

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    var id: Long? = null

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null

    @Column(nullable = false)
    var status: Boolean = true

    @Column(nullable = false)
    var deleted: Boolean = false
}


@Entity
@Table(name = "files")
class File(

    @Column(name = "owner_id", nullable = false)
    val taskId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: MediaType,

    @Column(name = "org_name", nullable = false)
    val orgName: String,

    @Column(name = "key_name", nullable = false, unique = true)
    val keyName: String,

    @Column(nullable = false)
    val path: String,

    @Column(nullable = false)
    val size: Long,

    val width: Int? = null,
    val height: Int? = null,
) : BaseEntity()