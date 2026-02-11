package org.example.task

import jakarta.persistence.*
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
    var createdBy: String? = null,

    @LastModifiedBy
    var lastModifiedBy: String? = null,

    @Column(nullable = false)
    @ColumnDefault("false")
    var deleted: Boolean = false
)


@Entity
class Project(

    @Column(nullable = false, unique = true, length = 124)
    var name: String,

    @Column(nullable = false)
    var organizationId: Long,

    @Column(length = 250)
    var description: String? = null

) : BaseEntity()


@Entity
class Task(

    @Column(nullable = false)
    var ownerAccountId: Long,

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    var dueDate: Date,

    @Column(nullable = false)
    var priority: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var board: Board,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var taskState: TaskState

) : BaseEntity()


@Entity
class Board(

    @Column(nullable = false, length = 72)
    var name: String,

    @Column(nullable = false, length = 124)
    var title: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var project: Project,

    @Column(nullable = false)
    var active: Boolean,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "board_states",
        joinColumns = [JoinColumn(name = "board_id")],
        inverseJoinColumns = [JoinColumn(name = "task_state_id")]
    )
    var taskStates: MutableList<TaskState> = mutableListOf()

) : BaseEntity()


@Entity
class TaskState(

    @Column(nullable = false, unique = true, length = 72)
    var name: String,

    @Column(nullable = false, unique = true, length = 60)
    var code: String

) : BaseEntity()


@Entity
class TaskFile(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var task: Task,

    @Column(nullable = false, length = 128)
    var keyName: String

) : BaseEntity()


@Entity
class AccountTask(

    @Column(nullable = false)
    var accountId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var task: Task

) : BaseEntity()
