package org.example.task

import org.springframework.stereotype.Service

interface ProjectService{
    fun create(projectCreateRequest: ProjectCreateRequest)
    fun getOne(projectId:Long): ProjectResponse
    fun getAll(): List<ProjectResponse>
    fun update(projectId:Long)
}

@Service
class ProjectServiceImpl(
    private val projectRepository: ProjectRepository,
    private val projectMapper: ProjectMapper
): ProjectService{
    override fun create(projectCreateRequest: ProjectCreateRequest) {
        val project = projectRepository.findByName(projectCreateRequest.name)
        if (project!=null)
            throw RuntimeException("Project Already exsist")

        projectRepository.save(Project(name = projectCreateRequest.name,
            description = projectCreateRequest.description,
            organizationId = currentOrgId()!!))
    }

    override fun getOne(projectId: Long) : ProjectResponse{
        val project = projectRepository.findByIdAndDeletedFalse(projectId)
        if (project==null)
            throw RuntimeException("ProjectNotFound")
        return projectMapper.toDto(project)
    }

    override fun getAll(): List<ProjectResponse> {
     return  projectRepository.findAll().map { project ->
           projectMapper.toDto(project)
       }
    }

    override fun update(projectId: Long) {
        TODO("Not yet implemented")
    }

}



interface BoardService {
    fun create(request: BoardCreateRequest)
    fun getOne(boardId: Long): BoardResponse
    fun getAll(): List<BoardResponse>
    fun update(boardId: Long, request: BoardUpdateRequest)
    fun delete(boardId: Long)
}

@Service
class BoardServiceImpl(
    private val boardRepository: BoardRepository,
    private val projectRepository: ProjectRepository,
    private val boardMapper: BoardMapper
) : BoardService {

    override fun create(request: BoardCreateRequest) {

        val project = projectRepository.findByIdAndDeletedFalse(request.projectId)
            ?: throw RuntimeException("ProjectNotFound")

        val exists = boardRepository.findByNameAndProjectId(request.name, project.id!!)
        if (exists != null)
            throw RuntimeException("Board already exists in this project")

        val board = Board(
            name = request.name,
            code = request.code,
            title = request.title,
            project = project,
            active = true
        )

        boardRepository.save(board)
    }

    override fun getOne(boardId: Long): BoardResponse {
        val board = boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw RuntimeException("BoardNotFound")
        return boardMapper.toDto(board)
    }

    override fun getAll(): List<BoardResponse> {
        return boardRepository.findAllNotDeleted()
            .map { board -> boardMapper.toDto(board) }
    }

    override fun update(boardId: Long, request: BoardUpdateRequest) {
        val board = boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw RuntimeException("BoardNotFound")

        request.name?.let { board.name = it }
        request.code?.let { board.code = it }
        request.title?.let { board.title = it }
        request.active?.let { board.active = it }

        boardRepository.save(board)
    }

    override fun delete(boardId: Long) {
        val board = boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw RuntimeException("BoardNotFound")
        boardRepository.trash(boardId)
    }
}


interface TaskStateService {
    fun create(request: TaskStateCreateRequest)
    fun getOne(id: Long): TaskStateResponse
    fun getAll(): List<TaskStateResponse>
    fun update(id: Long, request: TaskStateUpdateRequest)
    fun delete(id: Long)
    fun changeState(taskId: Long, request: TaskStateChangeRequest)

}

@Service
class TaskStateServiceImpl(
    private val taskStateRepository: TaskStateRepository,
    private val boardRepository: BoardRepository,
    private val taskStateMapper: TaskStateMapper,
    private val taskRepository: TaskRepository
) : TaskStateService {

    override fun create(request: TaskStateCreateRequest) {

        val board = boardRepository.findByIdAndDeletedFalse(request.boardId)
            ?: throw RuntimeException("BoardNotFound")

        val exists = taskStateRepository.findByNameAndBoardId(request.name, board.id!!)
        if (exists != null)
            throw RuntimeException("TaskState already exists in this board")

        val taskState = TaskState(
            name = request.name,
            code = request.code,
            board = board
        )

        taskStateRepository.save(taskState)
    }

    override fun getOne(id: Long): TaskStateResponse {
        val taskState = taskStateRepository.findByIdAndDeletedFalse(id)
            ?: throw RuntimeException("TaskStateNotFound")
        return taskStateMapper.toDto(taskState)
    }

    override fun getAll(): List<TaskStateResponse> {
        return taskStateRepository.findAllNotDeleted()
            .map { ts -> taskStateMapper.toDto(ts) }
    }

    override fun update(id: Long, request: TaskStateUpdateRequest) {
        val taskState = taskStateRepository.findByIdAndDeletedFalse(id)
            ?: throw RuntimeException("TaskStateNotFound")

        request.name?.let { taskState.name = it }
        request.code?.let { taskState.code = it }

        taskStateRepository.save(taskState)
    }

    override fun delete(id: Long) {
        val taskState = taskStateRepository.findByIdAndDeletedFalse(id)
            ?: throw RuntimeException("TaskStateNotFound")
        taskStateRepository.trash(id)
    }

    override fun changeState(taskId: Long, request: TaskStateChangeRequest) {

        val task = taskRepository.findByIdAndDeletedFalse(taskId)
            ?: throw RuntimeException("TaskNotFound")

        val currentUserId = userId()


        if (task.ownerAccountId != currentUserId && task.workerId != currentUserId) {
            throw RuntimeException("YouAreNotAllowedToChangeTaskState")
        }


        val newState = taskStateRepository.findByIdAndDeletedFalse(request.taskStateId)
            ?: throw RuntimeException("TaskStateNotFound")


        if (newState.board.id != task.board.id)
            throw RuntimeException("TaskStateNotBelongToThisBoard")

        task.taskState = newState


        taskRepository.save(task)
    }
}

interface TaskService {
    fun create(request: TaskCreateRequest)
    fun getOne(taskId: Long): TaskResponse
    fun getAll(): List<TaskResponse>
    fun update(taskId: Long, request: TaskUpdateRequest)
    fun delete(taskId: Long)
}


@Service
class TaskServiceImpl(
    private val taskRepository: TaskRepository,
    private val boardRepository: BoardRepository,
    private val taskStateRepository: TaskStateRepository,
    private val taskMapper: TaskMapper
) : TaskService {

    override fun create(request: TaskCreateRequest) {

        val board = boardRepository.findByIdAndDeletedFalse(request.boardId)
            ?: throw RuntimeException("BoardNotFound")

        val taskState = taskStateRepository.findByIdAndDeletedFalse(request.taskStateId)
            ?: throw RuntimeException("TaskStateNotFound")

        if (taskRepository.existsByNameAndBoardId(request.name, board.id!!))
            throw RuntimeException("Task already exists in this board")

        val task = Task(
            ownerAccountId = userId(),
            workerId = request.workerId,
            name = request.name,
            description = request.description,
            dueDate = request.dueDate,
            priority = request.priority,
            board = board,
            taskState = taskState
        )

        taskRepository.save(task)
    }

    override fun getOne(taskId: Long): TaskResponse {
        val task = taskRepository.findByIdAndDeletedFalse(taskId)
            ?: throw RuntimeException("TaskNotFound")

        return taskMapper.toDto(task)
    }

    override fun getAll(): List<TaskResponse> =
        taskRepository.findAllNotDeleted().map { taskMapper.toDto(it) }

    override fun update(taskId: Long, request: TaskUpdateRequest) {
        val task = taskRepository.findByIdAndDeletedFalse(taskId)
            ?: throw RuntimeException("TaskNotFound")

        request.workerId?.let { task.workerId = it }
        request.name?.let { task.name = it }
        request.description?.let { task.description = it }
        request.dueDate?.let { task.dueDate = it }
        request.priority?.let { task.priority = it }

        request.taskStateId?.let {
            val newState = taskStateRepository.findByIdAndDeletedFalse(it)
                ?: throw RuntimeException("TaskStateNotFound")
            task.taskState = newState
        }

        taskRepository.save(task)
    }

    override fun delete(taskId: Long) {
        val task = taskRepository.findByIdAndDeletedFalse(taskId)
            ?: throw RuntimeException("TaskNotFound")
        taskRepository.trash(taskId)
    }
}




/*

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface TaskService {
    fun create(request: CreateTaskRequest): TaskResponse
    fun getById(id: Long): TaskResponse
    fun getAll(): List<TaskResponse>
    fun updateDetails(id: Long, request: UpdateTaskRequest): TaskResponse
    fun move(id: Long, request: MoveTaskRequest): TaskResponse
    fun delete(id: Long)
}

interface ProjectService {
    fun create(request: CreateProjectRequest): ProjectResponse
    fun getById(id: Long): ProjectResponse
    fun getAll(): List<ProjectResponse>
    fun update(id: Long, request: UpdateProjectRequest): ProjectResponse
    fun delete(id: Long)
}

interface BoardService {
    fun create(request: CreateBoardRequest): BoardResponse
    fun getById(id: Long): BoardResponse
    fun getAll(): List<BoardResponse>
    fun update(id: Long, request: UpdateBoardRequest): BoardResponse
    fun delete(id: Long)
}

interface TaskStateService {
    fun create(request: CreateTaskStateRequest): TaskStateResponse
    fun getById(id: Long): TaskStateResponse
    fun getAllByBoard(boardId: Long): List<TaskStateResponse>
    fun update(id: Long, request: UpdateTaskStateRequest): TaskStateResponse
    fun delete(id: Long)
}

interface TaskFileService {
    fun create(request: CreateTaskFileRequest): TaskFileResponse
    fun getById(id: Long): TaskFileResponse
    fun getAll(): List<TaskFileResponse>
    fun delete(id: Long)
}

interface AccountTaskService {
    fun assignAccountToTask(request: AssignAccountToTaskRequest): AccountTaskResponse
    fun getById(id: Long): AccountTaskResponse
    fun getAllByTaskId(taskId: Long): List<AccountTaskResponse>
    fun delete(id: Long)
}

@Service
class TaskServiceImpl(
    private val taskRepository: TaskRepository,
    private val boardRepository: BoardRepository,
    private val taskStateRepository: TaskStateRepository,
    private val accountTaskRepository: AccountTaskRepository
) : TaskService {

    @Transactional
    override fun create(request: CreateTaskRequest): TaskResponse {
        val board = boardRepository.findByIdAndDeletedFalse(request.boardId)
            ?: throw NotFoundException("Board not found with id: ${request.boardId}")

        val initialTaskState = taskStateRepository.findByNameAndBoardIdAndDeletedFalse("Not started", board.id!!)
            ?: throw IllegalStateException("Board with id ${board.id} does not have an initial 'Not started' state.")

        val task = Task(
            ownerAccountId = request.ownerAccountId,
            name = request.name,
            description = request.description,
            dueDate = request.dueDate,
            maxEmployee = request.maxEmployee,
            priority = request.priority,
            boardId = board,
            taskStateId = initialTaskState
        )
        val savedTask = taskRepository.save(task)
        return savedTask.toResponse()
    }

    override fun getById(id: Long): TaskResponse {
        val task = taskRepository.findByIdAndDeletedFalse(id)
            ?: throw NotFoundException("Task not found with id: $id")
        return task.toResponse()
    }

    override fun getAll(): List<TaskResponse> {
        return taskRepository.findAllByDeletedFalse().map { it.toResponse() }
    }

    @Transactional
    override fun updateDetails(id: Long, request: UpdateTaskRequest): TaskResponse {
        val task = taskRepository.findByIdAndDeletedFalse(id)
            ?: throw NotFoundException("Task not found with id: $id")

        task.name = request.name ?: task.name
        task.description = request.description ?: task.description
        task.dueDate = request.dueDate ?: task.dueDate
        task.maxEmployee = request.maxEmployee ?: task.maxEmployee
        task.priority = request.priority ?: task.priority

        val updatedTask = taskRepository.save(task)
        return updatedTask.toResponse()
    }

    @Transactional
    override fun move(id: Long, request: MoveTaskRequest): TaskResponse {
        val task = taskRepository.findByIdAndDeletedFalse(id)
            ?: throw NotFoundException("Task not found with id: $id")
        val targetState = taskStateRepository.findByIdAndDeletedFalse(request.taskStateId)
            ?: throw NotFoundException("TaskState not found with id: ${request.taskStateId}")

        if (targetState.board.id != task.boardId.id) {
            throw IllegalStateException("Cannot move task to a state in a different board.")
        }

        val assignedAccounts = accountTaskRepository.findAllByTaskIdAndDeletedFalse(id)
        val isAssignee = assignedAccounts.any { it.accountId == request.assigneeId }
        val isOwner = task.ownerAccountId == request.assigneeId

        if (!isAssignee && !isOwner) {
            throw IllegalStateException("User with id ${request.assigneeId} is not authorized to move this task.")
        }

        if (targetState.name == "Completed" && !isOwner) {
            throw IllegalStateException("Only the task owner can move the task to 'Completed' state.")
        }

        task.taskStateId = targetState
        val movedTask = taskRepository.save(task)
        return movedTask.toResponse()
    }

    override fun delete(id: Long) {
        taskRepository.trash(id)
    }
}

@Service
class ProjectServiceImpl(private val projectRepository: ProjectRepository) : ProjectService {
    override fun create(request: CreateProjectRequest): ProjectResponse {
        val project = Project(
            name = request.name,
            description = request.description,
            organizationId = request.organizationId
        )
        return projectRepository.save(project).toResponse()
    }

    override fun getById(id: Long): ProjectResponse {
        return projectRepository.findByIdAndDeletedFalse(id)?.toResponse()
            ?: throw NotFoundException("Project not found with id: $id")
    }

    override fun getAll(): List<ProjectResponse> {
        return projectRepository.findAllByDeletedFalse().map { it.toResponse() }
    }

    override fun update(id: Long, request: UpdateProjectRequest): ProjectResponse {
        val project = projectRepository.findByIdAndDeletedFalse(id)
            ?: throw NotFoundException("Project not found with id: $id")
        project.name = request.name ?: project.name
        project.description = request.description ?: project.description
        return projectRepository.save(project).toResponse()
    }

    override fun delete(id: Long) {
        projectRepository.trash(id)
    }
}

@Service
class BoardServiceImpl(
    private val boardRepository: BoardRepository,
    private val projectRepository: ProjectRepository
) : BoardService {
    @Transactional
    override fun create(request: CreateBoardRequest): BoardResponse {
        val project = projectRepository.findByIdAndDeletedFalse(request.projectId)
            ?: throw NotFoundException("Project not found with id: ${request.projectId}")

        val board = Board(
            name = request.name,
            code = request.code,
            title = request.title,
            projectId = project,
            active = request.active
        )

        val savedBoard = boardRepository.save(board)

        // Create default task states
        val defaultStates = listOf(
            TaskState(name = "Not started", code = "NOT_STARTED", board = savedBoard),
            TaskState(name = "Ongoing", code = "ONGOING", board = savedBoard),
            TaskState(name = "Completed", code = "COMPLETED", board = savedBoard)
        )
        savedBoard.taskStates.addAll(defaultStates)

        return boardRepository.save(savedBoard).toResponse()
    }

    override fun getById(id: Long): BoardResponse {
        return boardRepository.findByIdAndDeletedFalse(id)?.toResponse()
            ?: throw NotFoundException("Board not found with id: $id")
    }

    override fun getAll(): List<BoardResponse> {
        return boardRepository.findAllByDeletedFalse().map { it.toResponse() }
    }

    override fun update(id: Long, request: UpdateBoardRequest): BoardResponse {
        val board = boardRepository.findByIdAndDeletedFalse(id)
            ?: throw NotFoundException("Board not found with id: $id")
        board.name = request.name ?: board.name
        board.title = request.title ?: board.title
        board.active = request.active ?: board.active
        return boardRepository.save(board).toResponse()
    }

    override fun delete(id: Long) {
        boardRepository.trash(id)
    }
}

@Service
class TaskStateServiceImpl(
    private val taskStateRepository: TaskStateRepository,
    private val boardRepository: BoardRepository
) : TaskStateService {
    override fun create(request: CreateTaskStateRequest): TaskStateResponse {
        val board = boardRepository.findByIdAndDeletedFalse(request.boardId)
            ?: throw NotFoundException("Board not found with id: ${request.boardId}")
        val taskState = TaskState(
            name = request.name,
            code = request.code,
            board = board
        )
        return taskStateRepository.save(taskState).toResponse()
    }

    override fun getById(id: Long): TaskStateResponse {
        return taskStateRepository.findByIdAndDeletedFalse(id)?.toResponse()
            ?: throw NotFoundException("TaskState not found with id: $id")
    }

    override fun getAllByBoard(boardId: Long): List<TaskStateResponse> {
        return taskStateRepository.findAllByBoardIdAndDeletedFalse(boardId).map { it.toResponse() }
    }

    override fun update(id: Long, request: UpdateTaskStateRequest): TaskStateResponse {
        val taskState = taskStateRepository.findByIdAndDeletedFalse(id)
            ?: throw NotFoundException("TaskState not found with id: $id")
        taskState.name = request.name ?: taskState.name
        taskState.code = request.code ?: taskState.code
        return taskStateRepository.save(taskState).toResponse()
    }

    override fun delete(id: Long) {
        taskStateRepository.trash(id)
    }
}

@Service
class TaskFileServiceImpl(
    private val taskFileRepository: TaskFileRepository,
    private val taskRepository: TaskRepository
) : TaskFileService {
    override fun create(request: CreateTaskFileRequest): TaskFileResponse {
        val task = taskRepository.findByIdAndDeletedFalse(request.taskId)
            ?: throw NotFoundException("Task not found with id: ${request.taskId}")
        val taskFile = TaskFile(
            taskId = task,
            keyName = request.keyName
        )
        return taskFileRepository.save(taskFile).toResponse()
    }

    override fun getById(id: Long): TaskFileResponse {
        return taskFileRepository.findByIdAndDeletedFalse(id)?.toResponse()
            ?: throw NotFoundException("TaskFile not found with id: $id")
    }

    override fun getAll(): List<TaskFileResponse> {
        return taskFileRepository.findAllByDeletedFalse().map { it.toResponse() }
    }

    override fun delete(id: Long) {
        taskFileRepository.trash(id)
    }
}

@Service
class AccountTaskServiceImpl(
    private val accountTaskRepository: AccountTaskRepository,
    private val taskRepository: TaskRepository
) : AccountTaskService {
    override fun assignAccountToTask(request: AssignAccountToTaskRequest): AccountTaskResponse {
        val task = taskRepository.findByIdAndDeletedFalse(request.taskId)
            ?: throw NotFoundException("Task not found with id: ${request.taskId}")
        val accountTask = AccountTask(
            accountId = request.accountId,
            task = task
        )
        val saved = accountTaskRepository.save(accountTask)
        return AccountTaskResponse(saved.id!!, saved.accountId)
    }

    override fun getById(id: Long): AccountTaskResponse {
        val accountTask = accountTaskRepository.findByIdAndDeletedFalse(id)
            ?: throw NotFoundException("AccountTask not found with id: $id")
        return AccountTaskResponse(accountTask.id!!, accountTask.accountId)
    }

    override fun getAllByTaskId(taskId: Long): List<AccountTaskResponse> {
        return accountTaskRepository.findAllByTaskIdAndDeletedFalse(taskId).map { AccountTaskResponse(it.id!!, it.accountId) }
    }

    override fun delete(id: Long) {
        accountTaskRepository.trash(id)
    }
}


fun Task.toResponse() = TaskResponse(
    id = id!!,
    ownerAccountId = ownerAccountId,
    name = name,
    description = description,
    dueDate = dueDate,
    maxEmployee = maxEmployee,
    priority = priority,
    board = boardId.toResponse(),
    taskState = taskStateId.toResponse()
)


 */