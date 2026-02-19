package org.example.task

import org.springframework.stereotype.Component

@Component
class ProjectMapper{
    fun toDto(project: Project): ProjectResponse{
        project.run {
            return ProjectResponse(
                id = project.id!!,
                name = project.name,
                description = project.description!!,
                organizationId = project.organizationId
            )
        }
    }
}

@Component
class BoardMapper(
    private val taskStateMapper: TaskStateMapper
) {

    fun toDto(board: Board): BoardResponse {
        board.run {
            return BoardResponse(
                id = id,
                name = name,
                title = title,
                active = active,
                projectId = project.id!!,
                taskStates = taskStates.map { taskStateMapper.toDto(it) }
            )
        }
    }
}

@Component
class TaskStateMapper {

    fun toDto(taskState: TaskState): TaskStateResponse {
        taskState.run {
            return TaskStateResponse(
                id = id!!,
                name = name,
                code = code,
                createdAt = createdDate!!
            )
        }
    }
}

@Component
class TaskMapper(
    private val taskStateMapper: TaskStateMapper,
) {

    fun toDto(task: Task, accountTasks: MutableList<Long>): TaskResponse {
        task.run {
            return TaskResponse(
                id = id,
                ownerAccountId = ownerAccountId,
                name = name,
                description = description,
                dueDate = dueDate,
                priority = priority,
                boardId = board.id!!,
                taskState = taskStateMapper.toDto(taskState),
                taskAssigns = accountTasks
            )
        }
    }
}



@Component
class AccountTaskMapper {
    fun toDto(accountTask: AccountTask): AccountTaskResponse {
        return AccountTaskResponse(
            accountId = accountTask.accountId
        )
    }

}