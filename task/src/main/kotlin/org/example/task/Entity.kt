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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @CreatedBy var createdBy: String? = null,
    @LastModifiedBy var lastModifiedBy: String? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false,
)

@Entity
class Project(
    @Column(unique = true, nullable = false, length = 124)
    var name: String,
    @Column(nullable = false)
    var organizationId: Long,
    var description: String?
) : BaseEntity()



@Entity
class Task(
    var ownerAccountId: Long,
    var name: String,
    var description: String,
    var dueDate: Date,
    var priority: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    var board: Board,
    @ManyToOne(fetch = FetchType.LAZY)
    var taskState: TaskState
) : BaseEntity()



@Entity
class Board(
    var name: String,
    var title: String,
    @ManyToOne(fetch = FetchType.LAZY)
    var project: Project,
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
    var name: String, // New
    @Column(nullable = false, unique = true, length = 60)
    var code: String, // NEW
) : BaseEntity()




@Entity
class TaskFile(
    @ManyToOne(fetch = FetchType.LAZY)
    var task: Task,
    @Column(nullable = false, length = 20)
    var keyName: String
) : BaseEntity()


@Entity
class AccountTask(
    var accountId: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    var task: Task
) : BaseEntity()