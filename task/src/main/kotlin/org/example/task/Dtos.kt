package org.example.task

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.*
import java.util.Date

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserInfoResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val role: String,
)


data class ProjectCreateRequest(
    @field:NotBlank
    @field:Size(max = 124)
    val name: String,

    @field:Size(max = 250)
    val description: String
)

data class ProjectUpdateRequest(
    @field:Size(max = 124)
    val name: String?,

    @field:Size(max = 250)
    val description: String?
)

data class ProjectResponse(
    val id: Long,
    val name: String,
    val description: String,
    val organizationId: Long
)

data class BoardCreateRequest(
    @field:NotBlank
    @field:Size(max = 72)
    val name: String,

    @field:NotBlank
    @field:Size(max = 124)
    val title: String,

    @field:Positive
    val projectId: Long,
)

data class BoardUpdateRequest(
    @field:Size(max = 72)
    val name: String?,

    @field:Size(max = 124)
    val title: String?,

    val active: Boolean?
)

data class BoardResponse(
    val id: Long?,
    val name: String,
    val title: String,
    val active: Boolean,
    val projectId: Long,
    val taskStates: List<TaskStateResponse>
)


data class TaskStateCreateRequest(
    @field:NotBlank
    @field:Size(max = 72)
    val name: String,

    @field:NotBlank
    @field:Size(max = 60)
    val code: String,

    val prevStateId: Long? // <-- yangi qo'shildi
)

data class TaskStateUpdateRequest(
    @field:Size(max = 72)
    val name: String?,

    @field:Size(max = 60)
    val code: String?
)

data class TaskStateResponse(
    val id: Long,
    val name: String,
    val code: String,
    val createdAt: Date
)

data class TaskStateChangeRequest(
    @field:NotBlank
    @field:Size(max = 60)
    val code: String,
)

data class TaskStateMoveRequest(
    val direction: String // UP yoki DOWN
)

data class TaskMoveRequest(
    val direction: MoveDirection // FORWARD or BACKWARD
)



data class TaskCreateRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,

    @field:NotBlank
    val description: String,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @field:FutureOrPresent
    val dueDate: Date,

    @field:Min(1) @field:Max(5)
    val priority: Int,

    @field:Positive
    val boardId: Long,
)

data class TaskUpdateRequest(
    @field:Size(max = 150)
    val name: String?,

    @field:Size(max = 2000)
    val description: String?,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @field:FutureOrPresent
    val dueDate: Date?,

    @field:Min(1) @field:Max(5)
    val priority: Int?,
)

data class TaskResponse(
    val id: Long?,
    val ownerAccountId: Long,
    val name: String,
    val description: String,
    val dueDate: Date,
    val priority: Int,
    val boardId: Long,
    val taskState: TaskStateResponse,
    val taskAssigns: MutableList<Long>
)


data class AssignAccountToTaskRequest(
    @field:Positive
    val accountId: Long,

    @field:Positive
    val taskId: Long
)

data class AccountTaskResponse(
    val accountId: Long
)


data class EmpRequest(
    val userId:Long,
    val orgId:Long
)

data class EmpResponse(
    val id:Long,
    val userId:Long,
    val orgId:Long,
    val position: String

)

data class ActionRequest(
    val taskId: Long,
    val content: String,
    val employees: List<Long>
)

data class OrganizationResponse(
    val id: Long,
    val name: String
)

data class UserResponse(
    val id: Long,
    val fullName: String,
)