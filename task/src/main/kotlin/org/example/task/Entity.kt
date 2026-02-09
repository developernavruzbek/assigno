package org.example.task


import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.OneToMany
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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @CreatedBy var createdBy: String? = null,
    @LastModifiedBy var lastModifiedBy: String? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false,
)


@Entity
class Task(
    var ownerAccountId: Long,
    var workerId:Long,
    var name: String,
    var description: String,
    var dueDate: Date,
    var priority: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    var board: Board,
    @ManyToOne(fetch = FetchType.LAZY)
    var taskState: TaskState
):BaseEntity()

@Entity
class TaskState(
    var name: String, // New
    var code: String, // NEW
    @ManyToOne(fetch = FetchType.LAZY)
    var board: Board
):BaseEntity()

@Entity
class Project(
    var name: String,
    var description: String,
    var organizationId: Long
):BaseEntity()

@Entity
class Board(
    var name: String,
    var code: String,
    var title: String,
    @ManyToOne(fetch = FetchType.LAZY)
    var project: Project,
    var active: Boolean,
):BaseEntity()

@Entity
class TaskFile(
    @ManyToOne(fetch = FetchType.LAZY)
    var taskId: Task,
    var keyName: String
):BaseEntity()

@Entity
class AccountTask(
    var accountId: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    var task: Task
):BaseEntity()