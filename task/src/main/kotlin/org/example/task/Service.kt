package org.example.task

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


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

        val exist =
            projectRepository.existsByNameAndOrganizationIdAndDeletedFalse(request.name, currentOrgId()!!)

        if (exist)
            throw ProjectAlreadyExistsException("Project Already Exists")

        val project = Project(
            name = request.name,
            description = request.description,
            organizationId = currentOrgId()!!
        )
        return projectMapper.toDto(projectRepository.save(project))

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

        if (project.organizationId != currentOrgId())
            throw ProjectNotFoundException("This organization does not have this project")

        if (boardRepository.existsByNameAndProjectIdAndDeletedFalse(request.name, project.id!!))
            throw BoardAlreadyExistsException(
                "Board already exists with name: ${request.name}"
            )

        val board = boardBuilder(request, project)

        val savedBoard = boardRepository.save(board)

        taskStateService.createDefaultStates(savedBoard)

        val saved = getByIdOrThrow(savedBoard.id!!)

        return boardMapper.toDto(saved)
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
    private val accountTaskRepository: AccountTaskRepository,
    private val organizationClient: OrganizationClient
) : TaskService {

    @Transactional
    override fun create(request: TaskCreateRequest): TaskResponse {
        val board = getBoard(request.boardId)
        if (board.project.organizationId != currentOrgId())
            throw ProjectNotFoundException("This organization does not have this project")


        validateTaskUniqueness(request.name, board.id!!)
        val defaultState = getDefaultState(board.id!!)
        val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))

        val task = buildTask(request, board, defaultState, emp.localNumber)
        val saved = taskRepository.save(task)

        val allEmployee = accountTaskService.getAllEmployee(saved.id!!).toMutableList()
        allEmployee.add(emp.localNumber)

        taskActionService.log(
            task = saved,
            type = TaskActionType.CREATED,
            comment = "New Task created",
            // employeeLocalNumbers = allEmployee
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
            .mapValues { entry -> entry.value.map { it.localNumber } }

        return tasks.map { task ->
            val assigns = assignsMap[task.id] ?: emptyList()
            taskMapper.toDto(task, assigns.toMutableList())
        }
    }

    @Transactional
    override fun update(taskId: Long, request: TaskUpdateRequest): TaskResponse {
        val task = getByIdOrThrow(taskId)
        val changes = mutableListOf<String>()

        if (request.name != null && request.name != task.name) {
            changes.add("Nomi: ${task.name} -> ${request.name}")
        }
        if (request.description != null && request.description != task.description) {
            changes.add("Tavsifi o'zgartirildi")
        }
        if (request.dueDate != null && request.dueDate != task.dueDate) {
            changes.add("Muddat: ${task.dueDate} -> ${request.dueDate}")
        }
        if (request.priority != null && request.priority != task.priority) {
            changes.add("Prioritet: ${task.priority} -> ${request.priority}")
        }

        if (changes.isNotEmpty()) {
            taskActionService.log(
                task = task,
                type = TaskActionType.UPDATED,
                comment = changes.joinToString("\n")
            )
        }

        applyUpdates(task, request)
        val saved = taskRepository.save(task)

        val assigns = accountTaskService.getAccountIdsByTaskId(taskId)
        return taskMapper.toDto(saved, assigns)
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
        val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))

        val isOwner = task.ownerLocalNumber == emp.localNumber
        val isAssigned = accountTaskRepository.existsByTaskIdAndLocalNumberAndDeletedFalse(taskId, emp.localNumber)

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

    private fun buildTask(request: TaskCreateRequest, board: Board, state: TaskState, employeeLocalNumber: Long) = Task(
        ownerLocalNumber = employeeLocalNumber,
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
    fun disallow(taskId: Long, localNumber: Long)
    fun getAccountIdsByTaskId(taskId: Long): MutableList<Long>
    fun getAllEmployee(taskId: Long): List<Long>
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
        //  println("1 =========================================")
        // checkPosition(organizationClient)
        // println("2 ====================")

        /* val task = taskRepository.findByIdAndDeletedFalse(request.taskId)
             ?: throw TaskNotFoundException("Task not found with id: ${request.taskId}")



         if (task.ownerAccountId != userId()) {
             throw ForbiddenException("You can't disallow account, you aren't the task owner")
         }
  */
        val accountTask = checkAndBuild(request)
        val saved = accountTaskRepository.saveAndRefresh(accountTask)
        val employeeLocal = organizationClient.getEmployeeLocal(LocalEmpRequest(saved.localNumber, currentOrgId()!!))
        taskActionService.log(
            task = saved.task,
            type = TaskActionType.ASSIGNED,
            newValue = employeeLocal.userId.toString(),
            //    employeeIds = getAllEmployee(saved.task.id!!)
        )

        return AccountTaskResponse(localNumber = saved.localNumber)
    }

    private fun checkAndBuild(request: AssignAccountToTaskRequest): AccountTask {
        val task = taskRepository.findByIdAndDeletedFalse(request.taskId)
            ?: throw TaskNotFoundException("Task not found with id: ${request.taskId}")

        val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))

        if (task.ownerLocalNumber != emp.localNumber)
            throw ForbiddenException("You can't assign, you aren't task owner")
        //  println("AccountId :${request.accountId}")
        //  organizationClient.getEmp(EmpRequest(request.accountId, currentOrgId()!!))
        organizationClient.getEmployeeLocal(LocalEmpRequest(request.localNumber, currentOrgId()!!))
        //  println("AccountId: ${request.accountId}")

        if (accountTaskRepository.existsByTaskIdAndLocalNumberAndDeletedFalse(request.taskId, request.localNumber))
            throw AccountAlreadyAssignedException(
                "Account ${request.localNumber} is already assigned to task ${request.taskId}"
            )

        return AccountTask(localNumber = request.localNumber, task = task)
    }

    override fun getAccountIdsByTaskId(taskId: Long): MutableList<Long> =
        accountTaskRepository.findAllByTaskIdAndDeletedFalse(taskId)
            .map { it.localNumber }
            .toMutableList()

    override fun getAllEmployee(taskId: Long): List<Long> {
        taskRepository.findByIdAndDeletedFalse(taskId)
            ?: throw TaskNotFoundException("Task not found")

        return accountTaskRepository.findAllByTaskIdAndDeletedFalse(taskId)
            .map { it.localNumber }
            .distinct()
    }

    override fun getAllByTaskId(taskId: Long): List<AccountTaskResponse> =
        accountTaskRepository.findAllByTaskIdAndDeletedFalse(taskId)
            .map { AccountTaskResponse(it.localNumber) }

    @Transactional
    override fun disallow(taskId: Long, localNumber: Long) {
        checkPosition(organizationClient)

        val task = taskRepository.findByIdAndDeletedFalse(taskId)
            ?: throw TaskNotFoundException("Task not found with id: $taskId")

        val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))

        if (task.ownerLocalNumber != emp.localNumber) {
            throw ForbiddenException("You can't disallow account, you aren't the task owner")
        }

        val entity = accountTaskRepository.findByTaskIdAndLocalNumberAndDeletedFalse(taskId, localNumber)
            ?: throw AccountTaskNotFoundException("Account $localNumber isn't assigned to task $taskId")

        val employeeLocal = organizationClient.getEmployeeLocal(LocalEmpRequest(localNumber, currentOrgId()!!))
        taskActionService.log(
            task = task,
            type = TaskActionType.UNASSIGNED,
            oldValue = employeeLocal.userId.toString()
        )

        accountTaskRepository.trash(entity.id!!)
    }
}

@Service
class FileIntegrationService(
    private val fileClient: FileClient,
    private val taskActionService: TaskActionService,
    private val taskRepository: TaskRepository
) {

    @Transactional
    fun uploadAndLog(file: MultipartFile, taskId: Long): FileResponse {
        val task = getTask(taskId)
        val response = fileClient.uploadFile(file, taskId)

        taskActionService.log(
            task = task,
            type = TaskActionType.FILE_UPLOADED,
            oldValue = null,
            newValue = response.keyName
        )
        return response
    }

    @Transactional
    fun deleteAndLog(fileId: Long, taskId: Long) {
        val task = getTask(taskId)
        fileClient.deleteFile(fileId)

        taskActionService.log(
            task = task,
            type = TaskActionType.FILE_DELETED,
            oldValue = "file_id: $fileId",
            newValue = null
        )
    }

    @Transactional
    fun deleteByKeyNameAndLog(keyName: String, taskId: Long) {
        val task = getTask(taskId)
        fileClient.deleteFileByKeyName(keyName)

        taskActionService.log(
            task = task,
            type = TaskActionType.FILE_DELETED,
            oldValue = "keyName: $keyName",
            newValue = null
        )
    }

    @Transactional
    fun deleteAllFilesByTask(taskId: Long) {
        val task = getTask(taskId)
        fileClient.deleteFilesByTask(taskId)

        taskActionService.log(
            task = task,
            type = TaskActionType.TASK_ALL_FILES_DELETED,
            oldValue = "All files removed",
            newValue = null
        )
    }

    private fun getTask(taskId: Long): Task = taskRepository.findById(taskId)
        .orElseThrow { TaskNotFoundException("Task not found with id: $taskId") }
}


interface TaskActionService {
    fun log(
        task: Task,
        type: TaskActionType,
        oldValue: String? = null,
        newValue: String? = null,
        comment: String? = null,
    )

    fun logsByTaskId(taskId: Long): List<String>
}

@Service
class TaskActionServiceImpl(
    private val taskActionRepository: TaskActionRepository,
    private val notificationClient: NotificationClient,
    private val organizationClient: OrganizationClient,
    private val userClient: UserClient,
    private val accountTaskRepository: AccountTaskRepository
) : TaskActionService {

    override fun log(
        task: Task, type: TaskActionType, oldValue: String?, newValue: String?, comment: String?
    ) {
        val currentUserId = userId()

        val action = TaskAction(
            task = task,
            actionType = type,
            updatedBy = currentUserId,
            oldValue = oldValue,
            newValue = newValue,
            comment = comment
        )
        taskActionRepository.save(action)

        val content = buildTelegramMessage(task, type, oldValue, newValue, comment, currentUserId)
        val accountTasks = accountTaskRepository.findAllByTaskIdAndDeletedFalse(task.id!!)
        val employees = accountTasks.map { accountTask -> accountTask.localNumber }.toMutableList()
        employees.add(task.ownerLocalNumber)

        println("========== $employees ==============")

        notificationClient.sendNotification(
            ActionRequest(
                taskId = task.id!!,
                content = content,
                employeeLocalNumbers = employees
            )
        )
    }

    override fun logsByTaskId(taskId: Long): List<String> {
        val logs = taskActionRepository.getAllByTaskIdAndDeletedFalse(taskId)
        if (logs.isEmpty()) return emptyList()

        val dateFormat = SimpleDateFormat("MM.dd HH:mm:ss")

        return logs.map { log ->
            val time = dateFormat.format(log.createdDate)

            val actor = try {
                userClient.getUserById(log.updatedBy).fullName
            } catch (e: Exception) {
                "ID: ${log.updatedBy} | ${e.message}"
            }

            val change = when (log.actionType) {
                TaskActionType.ASSIGNED -> {
                    val name = log.newValue?.toLongOrNull()?.let {
                        try {
                            userClient.getUserById(it).fullName
                        } catch (e: Exception) {
                            "ID: $it | ${e.message}"
                        }
                    } ?: "noma'lum"
                    "biriktirildi: $name"
                }

                TaskActionType.UNASSIGNED -> {
                    val name = log.oldValue?.toLongOrNull()?.let {
                        try {
                            userClient.getUserById(it).fullName
                        } catch (e: Exception) {
                            "ID: $it | ${e.message}"
                        }
                    } ?: "noma'lum"
                    "chetlatildi: $name"
                }

                else -> when {
                    !log.oldValue.isNullOrBlank() && !log.newValue.isNullOrBlank() ->
                        "${log.oldValue} dan -> ${log.newValue} ga o'zgardi"

                    !log.newValue.isNullOrBlank() -> "+ ${log.newValue}"
                    !log.oldValue.isNullOrBlank() -> "- ${log.oldValue}"
                    else -> log.actionType.name.lowercase().replace("_", " ")
                }
            }

            val commentStr = if (!log.comment.isNullOrBlank()) " | (${log.comment})" else ""

            "[$time] | $actor | ${log.task.name} | $change  $commentStr"
        }
    }

    private fun buildTelegramMessage(
        task: Task,
        type: TaskActionType,
        oldValue: String?,
        newValue: String?,
        comment: String?,
        currentUserId: Long
    ): String {
        val now = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        )

        val actorFullName = try {
            userClient.getUserById(currentUserId).fullName
        } catch (e: Exception) {
            "User Not Found with ID: $currentUserId. Exception message: ${e.message}"
        }

        val orgName = try {
            organizationClient.getOne(currentOrgId()!!).name
        } catch (e: Exception) {
            "Organization Not Found with ID: ${task.board.project.organizationId} ${e.message}"
        }


        val actionHeader = when (type) {
            TaskActionType.CREATED -> "🟢🆕 YANGI TOPSHIRIQ YARATILDI"
            TaskActionType.UPDATED -> "📝 TOPSHIRIQ TAHRIRLANDI"
            TaskActionType.MOVED_FORWARD -> "📈 HOLAT OLDINGA O'ZGARTIRILDI"
            TaskActionType.MOVED_BACKWARD -> "📉 HOLAT ORQAGA QAYTARILDI"
            TaskActionType.FILE_UPLOADED -> "📎 YANGI FAYL YUKLANDI"
            TaskActionType.FILE_DELETED -> "🗑 FAYL O'CHIRILDI"
            TaskActionType.TASK_ALL_FILES_DELETED -> "⚠️ BARCHA FAYLLAR O'CHIRILDI"
            TaskActionType.DELETED -> "🚫 TOPSHIRIQ O'CHIRILDI"
            TaskActionType.ASSIGNED -> "🎯 MAS'UL BIRIKTIRILDI"
            TaskActionType.UNASSIGNED -> "❌ MAS'UL CHETLATILDI"
        }

        val sb = StringBuilder()
        sb.append("$actionHeader\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━\n\n")

        sb.append("👤 Bajaruvchi: $actorFullName\n")
        sb.append("📌 Topshiriq: ${task.name}\n\n")

        sb.append("🏢 Tashkilot: $orgName\n")
        sb.append("📚 Loyiha: ${task.board.project.name}\n")
        sb.append("🕒 Vaqt: $now\n\n")

        when (type) {
            TaskActionType.ASSIGNED -> {
                val assignedUserId = newValue?.toLongOrNull()
                val assignedUserName = assignedUserId?.let {
                    try {
                        userClient.getUserById(it).fullName
                    } catch (e: Exception) {
                        "ID: $it. Exception message: ${e.message}"
                    }
                } ?: "Noma'lum"
                sb.append("👥 Mas'ul o‘zgarishi: ✅ $assignedUserName topshiriqqa biriktirildi\n")
            }

            TaskActionType.UNASSIGNED -> {
                val unassignedUserId = oldValue?.toLongOrNull()
                val unassignedUserName = unassignedUserId?.let {
                    try {
                        userClient.getUserById(it).fullName
                    } catch (e: Exception) {
                        "ID: $it. Exception message: ${e.message}"
                    }
                } ?: "Noma'lum"
                sb.append("👥 Mas’ul o‘zgarishi: ❌ $unassignedUserName topshiriqdan chetlatildi\n")
            }

            TaskActionType.FILE_UPLOADED -> sb.append("📎 Fayl: ✅ $newValue yuklandi\n")
            TaskActionType.FILE_DELETED -> sb.append("📎 Fayl: ❌ $oldValue o'chirildi\n")
            TaskActionType.TASK_ALL_FILES_DELETED -> sb.append("📎 Barcha fayllar tizimdan olib tashlandi!\n")
            TaskActionType.MOVED_FORWARD,
            TaskActionType.MOVED_BACKWARD -> {
                sb.append("🔄 Holat o'zgarishi:\n")
                sb.append("🔸  $oldValue ➡️ $newValue\n")
            }

            else -> {
                if (!comment.isNullOrBlank()) sb.append("💬 Izoh:\n$comment\n\n")
                if (oldValue != null && newValue != null) {
                    sb.append("🔄 O'zgarishlar:\n")
                    sb.append("🔸 Oldingi holat: $oldValue ➡️ Yangi holat: $newValue\n")
                }
            }
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🔗 [Topshiriqni Postmandan ko'rish](")
            .append("http://localhost:8080/api/v1/task/tasks/")
            .append(task.id)
            .append(")")
        return sb.toString()
    }
}

private fun checkPosition(organizationClient: OrganizationClient) {
    val currentUserId = userId()
    val orgId = currentOrgId() ?: throw OrganizationDidNotFoundException("Org not found or null")
    val emp = organizationClient.getEmp(EmpRequest(currentUserId, orgId))
    if (emp.position == "ORG_EMPLOYEE")
        throw ForbiddenException("You can't do this operation. Forbidden")
}