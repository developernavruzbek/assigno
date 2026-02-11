package org.example.task

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("projects")
class ProjectController(
    private val projectService: ProjectService
) {

    @PostMapping
    fun create(@RequestBody request: ProjectCreateRequest): ProjectResponse =
        projectService.create(request)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ProjectResponse =
        projectService.getById(id)

    @GetMapping
    fun getAll(): List<ProjectResponse> =
        projectService.getAll()

    @GetMapping("/organization/{orgId}")
    fun getByOrg(@PathVariable orgId: Long): List<ProjectResponse> =
        projectService.getByOrgId(orgId)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: ProjectUpdateRequest
    ): ProjectResponse =
        projectService.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): Boolean =
        projectService.delete(id)
}


@RestController
@RequestMapping("boards")
class BoardController(
    private val boardService: BoardService
) {

    @PostMapping
    fun create(@RequestBody request: BoardCreateRequest): BoardResponse =
        boardService.create(request)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): BoardResponse =
        boardService.getById(id)

    @GetMapping
    fun getAll(): List<BoardResponse> =
        boardService.getAll()

    @GetMapping("/project/{projectId}")
    fun getByProject(@PathVariable projectId: Long): List<BoardResponse> =
        boardService.getByProjectId(projectId)

    @GetMapping("/{id}/states")
    fun getStates(@PathVariable id: Long): List<TaskStateResponse> =
        boardService.getStatesById(id)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: BoardUpdateRequest
    ): BoardResponse =
        boardService.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): Boolean =
        boardService.delete(id)
}

@RestController
@RequestMapping("task-states")
class TaskStateController(
    private val service: TaskStateService
) {

    @PostMapping("/{boardId}")
    fun create(
        @PathVariable boardId: Long,
        @RequestBody request: TaskStateCreateRequest
    ): TaskStateResponse =
        service.create(boardId, request)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): TaskStateResponse =
        service.getById(id)

    @GetMapping
    fun getAll(): List<TaskStateResponse> =
        service.getAll()

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: TaskStateUpdateRequest
    ) = service.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) =
        service.delete(id)
}

@RestController
@RequestMapping("tasks")
class TaskController(
    private val taskService: TaskService
) {

    @PostMapping
    fun create(@RequestBody request: TaskCreateRequest): TaskResponse =
        taskService.create(request)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): TaskResponse =
        taskService.getById(id)

    @GetMapping("/board/{boardId}")
    fun getAllByBoard(@PathVariable boardId: Long): List<TaskResponse> =
        taskService.getAllByBoardId(boardId)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: TaskUpdateRequest
    ): TaskResponse =
        taskService.update(id, request)

    @PutMapping("/{id}/state")
    fun changeState(
        @PathVariable id: Long,
        @RequestBody request: TaskStateChangeRequest
    ): TaskResponse =
        taskService.updateState(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): Boolean =
        taskService.delete(id)
}


@RestController
@RequestMapping("account-tasks")
class AccountTaskController(
    private val service: AccountTaskService
) {

    @PostMapping
    fun assign(@RequestBody request: AssignAccountToTaskRequest): AccountTaskResponse =
        service.assignAccountToTask(request)

    @GetMapping("/{taskId}")
    fun getAll(@PathVariable taskId: Long): List<AccountTaskResponse> =
        service.getAllByTaskId(taskId)

    @DeleteMapping("/{taskId}/{accountId}")
    fun disallow(
        @PathVariable taskId: Long,
        @PathVariable accountId: Long
    ) = service.disallow(taskId, accountId)
}

