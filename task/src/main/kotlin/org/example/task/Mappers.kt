package org.example.task

import org.springframework.stereotype.Component

@Component
class ProjectMapper{
    fun toDto(project: Project): ProjectResponse{
        project.run {
            return ProjectResponse(
                id = id,
                name = name,
                description = description,
                organizationId = organizationId
            )
        }
    }
}

@Component
class BoardMapper {

    fun toDto(board: Board): BoardResponse {
        board.run {
            return BoardResponse(
                id = id,
                name = name,
                code = code,
                title = title,
                active = active,
                projectId = project.id!!
            )
        }
    }
}

@Component
class TaskStateMapper {

    fun toDto(taskState: TaskState): TaskStateResponse {
        taskState.run {
            return TaskStateResponse(
                id = id,
                name = name,
                code = code,
                boardId = board.id!!
            )
        }
    }
}


@Component
class TaskMapper {

    fun toDto(task: Task): TaskResponse {
        task.run {
            return TaskResponse(
                id = id,
                ownerAccountId = ownerAccountId,
                workerId = workerId,
                name = name,
                description = description,
                dueDate = dueDate,
                priority = priority,
                boardId = board.id!!,
                taskStateId = taskState.id!!
            )
        }
    }
}
