package org.example.task

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 *      PROJECT
 **/

interface ProjectService {
    fun create(request: ProjectCreateRequest): ProjectResponse
    fun getById(projectId: Long): ProjectResponse
    fun getAll(): List<ProjectResponse>
    fun update(projectId: Long, dto: ProjectUpdateRequest): ProjectResponse
    fun getByOrgId(orgId: Long): List<ProjectResponse>
    fun delete(projectId: Long)
}

@Service
class ProjectServiceImpl(
    private val projectRepository: ProjectRepository,
    private val projectMapper: ProjectMapper,
    private val boardService: BoardService,
    private val organizationClient: OrganizationClient
) : ProjectService {

    @Transactional
    override fun create(request: ProjectCreateRequest): ProjectResponse {
        checkPosition(organizationClient)

        val orgId = currentOrgId() ?: throw OrganizationDidNotFoundException("Organization not found")

        val project = Project(
            name = request.name,
            description = request.description,
            organizationId = orgId
        )

        try {
            val saved = projectRepository.save(project)
            return projectMapper.toDto(saved)
        } catch (ex: DataIntegrityViolationException) {
            throw ProjectAlreadyExistsException("Project already exists with name: ${request.name} ex : ${ex.message}")
        }
    }

    override fun getById(projectId: Long): ProjectResponse {
        return projectMapper.toDto(getByIdOrThrow(projectId))
    }

    private fun getByIdOrThrow(projectId: Long): Project =
        projectRepository.findByIdAndDeletedFalse(projectId)
            ?: throw ProjectNotFoundException("Project not found")

    override fun getByOrgId(orgId: Long): List<ProjectResponse> =
        projectRepository.findAllByOrganizationIdAndDeletedFalse(orgId)
            .map(projectMapper::toDto)

    override fun getAll(): List<ProjectResponse> =
        projectRepository.findAllByDeletedFalse()
            .map(projectMapper::toDto)

    @Transactional
    override fun update(projectId: Long, dto: ProjectUpdateRequest): ProjectResponse {
        checkPosition(organizationClient)

        val project = getByIdOrThrow(projectId)

        if (dto.name != null && dto.name != project.name) {
            if (projectRepository.existsByNameAndOrganizationIdAndDeletedFalse(dto.name, project.organizationId)) {
                throw ProjectAlreadyExistsException("Project already exists with name: ${dto.name}")
            }
            project.name = dto.name
        }

        dto.description?.let { project.description = it }

        return projectMapper.toDto(projectRepository.save(project))
    }

    @Transactional
    override fun delete(projectId: Long) {
        checkPosition(organizationClient)

        getByIdOrThrow(projectId)

        boardService.deleteByProjectId(projectId)

        projectRepository.trash(projectId)
    }
}


/**
 *      BOARD
 **/


interface BoardService {
    fun create(request: BoardCreateRequest): BoardResponse
    fun getById(boardId: Long): BoardResponse
    fun getByProjectId(projectId: Long): List<BoardResponse>
    fun getStatesById(boardId: Long): List<TaskStateResponse>
    fun getAll(): List<BoardResponse>
    fun update(boardId: Long, request: BoardUpdateRequest): BoardResponse
    fun delete(boardId: Long)
    fun deleteByProjectId(projectId: Long)
}

@Service
class BoardServiceImpl(
    private val boardRepository: BoardRepository,
    private val projectRepository: ProjectRepository,
    private val boardMapper: BoardMapper,
    private val taskStateService: TaskStateService,
    private val taskStateMapper: TaskStateMapper,
    private val taskRepository: TaskRepository,
    private val accountTaskRepository: AccountTaskRepository,
    private val organizationClient: OrganizationClient
) : BoardService {

    @Transactional
    override fun create(request: BoardCreateRequest): BoardResponse {
        checkPosition(organizationClient)

        val project = getProjectOrThrow(request.projectId)

        if (boardRepository.existsByNameAndProjectIdAndDeletedFalse(request.name, project.id!!))
            throw BoardAlreadyExistsException(
                "Board already exists with name: ${request.name}"
            )

        val board = boardBuilder(request, project)

        val savedBoard = boardRepository.save(board)

        taskStateService.createDefaultStates(savedBoard)

        return boardMapper.toDto(savedBoard)
    }


    override fun getById(boardId: Long): BoardResponse =
        boardMapper.toDto(getByIdOrThrow(boardId))

    override fun getAll(): List<BoardResponse> =
        boardRepository.findAllNotDeleted()
            .map(boardMapper::toDto)

    override fun getByProjectId(projectId: Long): List<BoardResponse> {
        getProjectOrThrow(projectId)
        return boardRepository.findAllByProjectIdAndDeletedFalse(projectId)
            .map(boardMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun getStatesById(boardId: Long): List<TaskStateResponse> {
        val board = getByIdOrThrow(boardId)
        return board.taskStates.map { taskStateMapper.toDto(it) }
    }

    @Transactional
    override fun update(boardId: Long, request: BoardUpdateRequest): BoardResponse {
        checkPosition(organizationClient)


        val board = getByIdOrThrow(boardId)

        request.name?.let { board.name = it }
        request.title?.let { board.title = it }
        request.active?.let { board.active = it }

        return boardRepository.save(board)
            .let { boardMapper.toDto(it) }
    }

    @Transactional
    override fun delete(boardId: Long) {
        checkPosition(organizationClient)

        val board = getByIdOrThrow(boardId)
        deepDeleteBoard(board)
    }

    @Transactional
    override fun deleteByProjectId(projectId: Long) {
        val boards = boardRepository.findAllByProjectIdAndDeletedFalse(projectId)
        boards.forEach {
            deepDeleteBoard(it)
        }
    }

    private fun deepDeleteBoard(board: Board) {
        val tasks = taskRepository.findAllByBoardIdAndDeletedFalse(board.id!!)
        if (tasks.isNotEmpty()) {
            val taskIds = tasks.map { it.id!! }

            accountTaskRepository.trashAllByTaskIds(taskIds)
            taskRepository.trashAllByIds(taskIds)
        }
        boardRepository.trash(board.id!!)
    }

    private fun getByIdOrThrow(boardId: Long): Board =
        boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw BoardNotFoundException("Board not found")

    private fun getProjectOrThrow(projectId: Long): Project =
        projectRepository.findByIdAndDeletedFalse(projectId)
            ?: throw ProjectNotFoundException("Project not found")

    private fun boardBuilder(dto: BoardCreateRequest, project: Project) = Board(
        name = dto.name,
        title = dto.title,
        active = true,
        project = project
    )
}


/**
 *      TASK STATE
 **/

interface TaskStateService {
    fun create(boardId: Long, request: TaskStateCreateRequest): TaskStateResponse
    fun createDefaultStates(board: Board)
    fun getById(id: Long): TaskStateResponse
    fun getAll(): List<TaskStateResponse>
    fun update(id: Long, request: TaskStateUpdateRequest)
    fun delete(id: Long)
}

@Service
class TaskStateServiceImpl(
    private val taskStateRepository: TaskStateRepository,
    private val taskStateMapper: TaskStateMapper,
    private val boardRepository: BoardRepository
) : TaskStateService {

    @Transactional
    override fun create(boardId: Long, request: TaskStateCreateRequest): TaskStateResponse {
        val board = boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw BoardNotFoundException("Board not found with id: $boardId")

        validateUniqueness(boardId, request.name, request.code)

        val insertPosition = if (request.prevStateId != null) {
            val prevState = getByIdOrThrow(request.prevStateId)
            if (prevState.board.id != boardId) throw BadRequestException("State belongs to another board")
            prevState.position + 1
        } else {
            1
        }

        taskStateRepository.incrementPositions(boardId, insertPosition)

        val state = TaskState(
            name = request.name,
            code = request.code,
            position = insertPosition,
            board = board
        )

        return taskStateMapper.toDto(taskStateRepository.save(state))
    }

    private fun validateUniqueness(boardId: Long, name: String, code: String) {
        if (taskStateRepository.existsByNameAndBoardIdAndDeletedFalse(name, boardId))
            throw TaskStateAlreadyExistsException("Name '$name' already exists in this board")

        if (taskStateRepository.existsByCodeAndBoardIdAndDeletedFalse(code, boardId))
            throw TaskStateAlreadyExistsException("Code '$code' already exists in this board")
    }


    @Transactional
    override fun createDefaultStates(board: Board) {
        val required = listOf("NEW", "IN_PROGRESS", "REVIEW", "DONE")

        val lastPos = taskStateRepository.findMaxPositionByBoardId(board.id!!) ?: 0
        var counter = lastPos + 1

        val states = required.map { code ->
            TaskState(
                name = code.replace("_", " "),
                code = code,
                position = counter++,
                board = board
            )
        }
        taskStateRepository.saveAll(states)
    }

    override fun getById(id: Long): TaskStateResponse =
        taskStateMapper.toDto(getByIdOrThrow(id))

    override fun getAll(): List<TaskStateResponse> =
        taskStateRepository.findAllNotDeleted()
            .map(taskStateMapper::toDto)

    @Transactional
    override fun update(id: Long, request: TaskStateUpdateRequest) {
        val taskState = getByIdOrThrow(id)

        request.name?.let { taskState.name = it }
        request.code?.let { taskState.code = it }

        taskStateRepository.save(taskState)
    }

    @Transactional
    override fun delete(id: Long) {
        val state = getByIdOrThrow(id)
        val boardId = state.board.id!!
        val deletedPosition = state.position

        taskStateRepository.trash(id)
        taskStateRepository.decrementPositions(boardId, deletedPosition)
    }

    @Transactional
    fun moveState(stateId: Long, direction: String): TaskStateResponse {
        val state = getByIdOrThrow(stateId)
        val boardId = state.board.id!!

        val neighbor = when (direction.uppercase()) {
            "UP" -> taskStateRepository.findPrev(boardId, state.position)
            "DOWN" -> taskStateRepository.findNext(boardId, state.position)
            else -> throw IllegalArgumentException("Direction must be UP or DOWN")
        } ?: throw IllegalStateException("Boundary reached: cannot move further in this direction")

        val currentPos = state.position
        state.position = neighbor.position
        neighbor.position = currentPos

        taskStateRepository.save(state)
        taskStateRepository.save(neighbor)

        return taskStateMapper.toDto(state)
    }

    private fun getByIdOrThrow(id: Long): TaskState =
        taskStateRepository.findByIdAndDeletedFalse(id)
            ?: throw TaskStateNotFoundException("TaskState not found with id: $id")
}


/**
 *      TASK
 **/


interface TaskService {
    fun create(request: TaskCreateRequest): TaskResponse
    fun getById(taskId: Long): TaskResponse
    fun getAllByBoardId(boardId: Long): List<TaskResponse>
    fun update(taskId: Long, request: TaskUpdateRequest): TaskResponse
    fun updateState(taskId: Long, request: TaskStateChangeRequest): TaskResponse
    fun delete(taskId: Long)
    fun move(taskId: Long, request: TaskMoveRequest): TaskResponse
}

@Service
class TaskServiceImpl(
    private val taskMapper: TaskMapper,
    private val taskRepository: TaskRepository,
    private val taskStateRepository: TaskStateRepository,
    private val taskActionService: TaskActionService,
    private val boardRepository: BoardRepository,
    private val accountTaskService: AccountTaskService,
    private val accountTaskRepository: AccountTaskRepository
) : TaskService {

    @Transactional
    override fun create(request: TaskCreateRequest): TaskResponse {
        val board = getBoard(request.boardId)
        validateTaskUniqueness(request.name, board.id!!)
        val defaultState = getDefaultState(board.id!!)

        val task = buildTask(request, board, defaultState)
        val saved = taskRepository.save(task)

        taskActionService.log(
            task = saved,
            type = TaskActionType.CREATED,
            comment = "New Task created"
        )

        return taskMapper.toDto(saved, mutableListOf())
    }

    override fun getById(taskId: Long): TaskResponse {
        val task = getByIdOrThrow(taskId)
        val assigns = accountTaskService.getAccountIdsByTaskId(taskId)
        return taskMapper.toDto(task, assigns)
    }

    private fun getByIdOrThrow(taskId: Long): Task =
        taskRepository.findByIdAndDeletedFalse(taskId)
            ?: throw TaskNotFoundException("Task not found with id=$taskId")

    override fun getAllByBoardId(boardId: Long): List<TaskResponse> {

        boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw BoardNotFoundException("Board not found with id=$boardId")

        val tasks = taskRepository.findAllByBoardIdAndDeletedFalse(boardId)

        val taskIds = tasks.map { it.id!! }

        val assignsMap = accountTaskRepository
            .findAllByTaskIdInAndDeletedFalse(taskIds)
            .groupBy { it.task.id!! }
            .mapValues { entry -> entry.value.map { it.accountId } }

        return tasks.map { task ->
            val assigns = assignsMap[task.id] ?: emptyList()
            taskMapper.toDto(task, assigns.toMutableList())
        }
    }

    @Transactional
    override fun update(taskId: Long, request: TaskUpdateRequest): TaskResponse {
        val task = getByIdOrThrow(taskId)

        logIfChanged(task, "name", task.name, request.name)
        logIfChanged(task, "description", task.description, request.description)
        logIfChanged(task, "dueDate", task.dueDate.toString(), request.dueDate?.toString())
        logIfChanged(task, "priority", task.priority.toString(), request.priority?.toString())

        applyUpdates(task, request)
        val saved = taskRepository.save(task)

        val assigns = accountTaskService.getAccountIdsByTaskId(taskId)
        return taskMapper.toDto(saved, assigns)
    }

    private fun logIfChanged(task: Task, field: String, oldValue: String?, newValue: String?) {
        if (newValue != null && oldValue != newValue) {
            taskActionService.log(
                task = task,
                type = TaskActionType.UPDATED,
                oldValue = "$field:$oldValue",
                newValue = "$field:$newValue"
            )
        }
    }

    /**
    POST /tasks/{id}/move/forward

    POST /tasks/{id}/move/backward
     **/
    @Transactional
    override fun move(taskId: Long, request: TaskMoveRequest): TaskResponse {

        val task = getByIdOrThrow(taskId)
        val boardId = task.board.id!!

        val currentState = task.taskState

        val nextState = when (request.direction) {
            MoveDirection.FORWARD -> taskStateRepository.findNext(boardId, currentState.position)
            MoveDirection.BACKWARD -> taskStateRepository.findPrev(boardId, currentState.position)
        } ?: throw BadRequestException("Invalid move direction for this task")

        val actionType = when (request.direction) {
            MoveDirection.FORWARD -> TaskActionType.MOVED_FORWARD
            MoveDirection.BACKWARD -> TaskActionType.MOVED_BACKWARD
        }

        val oldState = currentState

        task.taskState = nextState
        val saved = taskRepository.save(task)

        taskActionService.log(
            task = saved,
            type = actionType,
            oldValue = oldState.code,
            newValue = nextState.code
        )

        val assigns = accountTaskService.getAccountIdsByTaskId(taskId)
        return taskMapper.toDto(saved, assigns)
    }

    @Transactional
    override fun updateState(taskId: Long, request: TaskStateChangeRequest): TaskResponse {
        val task = getByIdOrThrow(taskId)
        val currentUserId = userId()

        val isOwner = task.ownerAccountId == currentUserId
        val isAssigned = accountTaskRepository.existsByTaskIdAndAccountIdAndDeletedFalse(taskId, currentUserId)

        if (!isOwner && !isAssigned) throw ForbiddenException("You can't change state of this task. Forbidden")

        val newState = taskStateRepository.findByCodeAndBoardIdAndDeletedFalse(request.code, task.board.id!!)
            ?: throw TaskStateNotFoundException("State not found in this board")

        task.taskState = newState
        val saved = taskRepository.save(task)

        return taskMapper.toDto(saved, accountTaskService.getAccountIdsByTaskId(taskId))
    }

    @Transactional
    override fun delete(taskId: Long) {
        val task = getByIdOrThrow(taskId)

        val assigns = accountTaskRepository.findAllByTaskIdAndDeletedFalse(task.id!!)
        assigns.forEach { accountTaskRepository.trash(it.id!!) }

        taskActionService.log(
            task = task,
            type = TaskActionType.DELETED,
            comment = "Task deleted"
        )

        taskRepository.trash(task.id!!)

    }

    private fun applyUpdates(task: Task, request: TaskUpdateRequest) {

        request.name?.let { task.name = it }
        request.description?.let { task.description = it }
        request.dueDate?.let { task.dueDate = it }
        request.priority?.let { task.priority = it }
    }

    private fun getBoard(boardId: Long): Board =
        boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw BoardNotFoundException("Board not found with id=$boardId")

    private fun validateTaskUniqueness(name: String, boardId: Long) {
        if (taskRepository.existsByNameAndBoardIdAndDeletedFalse(name, boardId))
            throw TaskAlreadyExistsException(
                "Task with name '$name' already exists in this board"
            )
    }

    private fun getDefaultState(boardId: Long): TaskState =
        taskStateRepository.findByCodeAndBoardId("NEW", boardId)
            ?: throw TaskStateNotFoundException(
                "Default TODO state not found for board $boardId"
            )

    private fun buildTask(request: TaskCreateRequest, board: Board, state: TaskState) = Task(
        ownerAccountId = userId(),
        name = request.name,
        description = request.description,
        dueDate = request.dueDate,
        priority = request.priority,
        board = board,
        taskState = state
    )
}


/**
 *      ACCOUNT TASK
 **/


interface AccountTaskService {
    fun assignAccountToTask(request: AssignAccountToTaskRequest): AccountTaskResponse
    fun getAllByTaskId(taskId: Long): List<AccountTaskResponse>
    fun disallow(taskId: Long, accountId: Long)
    fun getAccountIdsByTaskId(taskId: Long): MutableList<Long>
}

@Service
class AccountTaskServiceImpl(
    private val accountTaskRepository: AccountTaskRepository,
    private val taskRepository: TaskRepository,
    private val taskActionService: TaskActionService,
    private val organizationClient: OrganizationClient
) : AccountTaskService {

    @Transactional
    override fun assignAccountToTask(request: AssignAccountToTaskRequest): AccountTaskResponse {
        checkPosition(organizationClient)

        val task = taskRepository.findByIdAndDeletedFalse(request.taskId)
            ?: throw TaskNotFoundException("Task not found with id: ${request.taskId}")

        if (task.ownerAccountId != userId()) {
            throw ForbiddenException("You can't disallow account, you aren't the task owner")
        }

        val accountTask = checkAndBuild(request)
        val saved = accountTaskRepository.save(accountTask)

        taskActionService.log(
            task = saved.task,
            type = TaskActionType.ASSIGNED,
            newValue = saved.accountId.toString()
        )

        return AccountTaskResponse(accountId = saved.accountId)
    }

    private fun checkAndBuild(request: AssignAccountToTaskRequest): AccountTask {
        val task = taskRepository.findByIdAndDeletedFalse(request.taskId)
            ?: throw TaskNotFoundException("Task not found with id: ${request.taskId}")

        if (task.ownerAccountId != userId())
            throw ForbiddenException("You can't assign, you aren't task owner")

        organizationClient.getEmp(EmpRequest(request.accountId, currentOrgId()!!))

        if (accountTaskRepository.existsByTaskIdAndAccountIdAndDeletedFalse(request.taskId, request.accountId))
            throw AccountAlreadyAssignedException(
                "Account ${request.accountId} is already assigned to task ${request.taskId}"
            )

        return AccountTask(accountId = request.accountId, task = task)
    }

    override fun getAccountIdsByTaskId(taskId: Long): MutableList<Long> =
        accountTaskRepository.findAllByTaskIdAndDeletedFalse(taskId)
            .map { it.accountId }
            .toMutableList()

    override fun getAllByTaskId(taskId: Long): List<AccountTaskResponse> =
        accountTaskRepository.findAllByTaskIdAndDeletedFalse(taskId)
            .map { AccountTaskResponse(it.accountId) }

    @Transactional
    override fun disallow(taskId: Long, accountId: Long) {
        checkPosition(organizationClient)

        val task = taskRepository.findByIdAndDeletedFalse(taskId)
            ?: throw TaskNotFoundException("Task not found with id: $taskId")

        if (task.ownerAccountId != userId()) {
            throw ForbiddenException("You can't disallow account, you aren't the task owner")
        }

        val entity = accountTaskRepository.findByTaskIdAndAccountIdAndDeletedFalse(taskId, accountId)
            ?: throw AccountTaskNotFoundException("Account $accountId isn't assigned to task $taskId")

        taskActionService.log(
            task = task,
            type = TaskActionType.UNASSIGNED,
            oldValue = accountId.toString()
        )

        accountTaskRepository.trash(entity.id!!)
    }
}

interface TaskActionService {
    fun log(
        task: Task,
        type: TaskActionType,
        oldValue: String? = null,
        newValue: String? = null,
        comment: String? = null
    )
}

@Service
class TaskActionServiceImpl(
    private val taskActionRepository: TaskActionRepository
) : TaskActionService {

    override fun log(task: Task, type: TaskActionType, oldValue: String?, newValue: String?, comment: String?) {
        val action = TaskAction(
            task = task,
            actionType = type,
            updatedBy = userId(),
            oldValue = oldValue,
            newValue = newValue,
            comment = comment
        )
        taskActionRepository.save(action)
    }
}

/// controllerga ko'chirish
private fun checkPosition(organizationClient: OrganizationClient) {
    val currentUserId = userId()
    val orgId = currentOrgId() ?: throw OrganizationDidNotFoundException("Org not found or null")
    val emp = organizationClient.getEmp(EmpRequest(currentUserId, orgId))
    if (emp.position == "ORG_EMPLOYEE")
        throw ForbiddenException("You can't do this operation. Forbidden")
}
