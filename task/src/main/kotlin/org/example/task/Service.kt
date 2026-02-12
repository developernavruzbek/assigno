package org.example.task

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


interface ProjectService {
    fun create(request: ProjectCreateRequest): ProjectResponse
    fun getById(projectId: Long): ProjectResponse
    fun getAll(): List<ProjectResponse>
    fun update(projectId: Long, dto: ProjectUpdateRequest): ProjectResponse
    fun getByOrgId(orgId: Long): List<ProjectResponse>
    fun delete(projectId: Long): Boolean
}

@Service
class ProjectServiceImpl(
    private val projectRepository: ProjectRepository,
    private val projectMapper: ProjectMapper,
    private val boardRepository: BoardRepository,
    private val taskRepository: TaskRepository,
    private val accountTaskRepository: AccountTaskRepository,
    private val organizationClient: OrganizationClient
) : ProjectService {


    @Transactional
    override fun create(request: ProjectCreateRequest): ProjectResponse {

        val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))

        if (emp.position=="ORG_EMPLOYEE")
            throw ForbiddenException("Forbidden")



        val orgId = currentOrgId() ?: throw OrganizationDidNotFoundException("Organization not found")
        if (projectRepository.existsByNameAndOrganizationId(request.name, orgId))
            throw ProjectAlreadyExistsException("Project already exists with name: ${request.name}")

        return Project(
            name = request.name,
            description = request.description,
            organizationId = orgId
        ).let { projectRepository.save(it) }
            .let { projectMapper.toDto(it) }
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

    override fun update(projectId: Long, dto: ProjectUpdateRequest): ProjectResponse {
        val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))
        if (emp.position=="ORG_EMPLOYEE")
            throw ForbiddenException("Forbidden")

        val project = getByIdOrThrow(projectId)

        dto.name?.let { project.name = it }
        dto.description?.let { project.description = it }

        return projectMapper.toDto(projectRepository.save(project))
    }

    override fun delete(projectId: Long): Boolean {

        val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))

        if (emp.position=="ORG_EMPLOYEE")
            throw ForbiddenException("Forbidden")


        val project = getByIdOrThrow(projectId)

        val boards = boardRepository.findAllByProjectIdAndDeletedFalse(projectId)
        boards.forEach { board ->
            deleteBoardDeep(board)      // ❗ yangi funksiya
        }

        return projectRepository.trash(projectId)
    }

    private fun deleteBoardDeep(board: Board) {
        // 1. tasklar
        val tasks = taskRepository.findAllByBoardIdAndDeletedFalse(board.id!!)
        tasks.forEach { task ->
            val assigns = accountTaskRepository.findAllByTaskIdAndDeletedFalse(task.id!!)
            assigns.forEach { accountTaskRepository.trash(it.id!!) }
            taskRepository.trash(task.id!!)
        }

        // 2. taskStates — o‘chirilmaydi (default bo‘lishi mumkin)
        // faqat board detach bo‘ladi

        // 3. board
        boardRepository.trash(board.id!!)
    }

    // TODO: check EMP POSITION = ADMIN or MANAGER and projectni boardlari nima bo'ladi ??
}


interface BoardService {
    fun create(request: BoardCreateRequest): BoardResponse
    fun getById(boardId: Long): BoardResponse
    fun getByProjectId(projectId: Long): List<BoardResponse>
    fun getStatesById(boardId: Long): List<TaskStateResponse>
    fun getAll(): List<BoardResponse>
    fun update(boardId: Long, request: BoardUpdateRequest): BoardResponse
    fun delete(boardId: Long): Boolean
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

    override fun create(request: BoardCreateRequest): BoardResponse {
        val project = getProjectOrThrow(request.projectId)

        if (boardRepository.existsByNameAndProjectId(request.name, project.id!!))
            throw BoardAlreadyExistsException("Board already exists with name: ${request.name}")

        val board = boardBuilder(request, project)
        taskStateService.createDefaultStates(board)

        val saved = boardRepository.saveAndRefresh(board)
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

    override fun getStatesById(boardId: Long): List<TaskStateResponse> {
        getByIdOrThrow(boardId)
        return boardRepository.findById(boardId).get()
            .taskStates.map { taskStateMapper.toDto(it) }        //// todo         for TASK SERVICE
    }

    override fun update(boardId: Long, request: BoardUpdateRequest): BoardResponse {
        val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))

        if (emp.position=="ORG_EMPLOYEE")
            throw ForbiddenException("Forbidden")


        val board = getByIdOrThrow(boardId)

        request.name?.let { board.name = it }
        request.title?.let { board.title = it }
        request.active?.let { board.active = it }

        return boardRepository.save(board)
            .let { boardMapper.toDto(it) }
    }

    override fun delete(boardId: Long): Boolean {
        val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))

        if (emp.position=="ORG_EMPLOYEE")
            throw ForbiddenException("Forbidden")


        val board = getByIdOrThrow(boardId)

        deleteBoardWithChildren(board)

        return true
    }
    // TODO: check EMP POSITION = ADMIN or MANAGER and boarddagi tasklar statelar nima bo'ladi ??

    private fun deleteBoardWithChildren(board: Board) {

        // 1. delete account tasks
        val tasks = taskRepository.findAllByBoardIdAndDeletedFalse(board.id!!)
        tasks.forEach { task ->

            val accountTasks = accountTaskRepository.findAllByTaskIdAndDeletedFalse(task.id!!)
            accountTasks.forEach { accountTask ->
                accountTaskRepository.trash(accountTask.id!!)
            }

            // 2. delete task
            taskRepository.trash(task.id!!)
        }

        // 3. DO NOT delete TaskStates (defaultlar ham bor)
        // Board.taskStates aynan shu boardga tegishli bo‘lgani uchun
        // detach bo'ladi, lekin o‘chirib yuborilmaydi

        // 4. delete board
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
    // TODO: check EMP POSITION = task's owner
    override fun create(boardId: Long, request: TaskStateCreateRequest): TaskStateResponse {
        val board = boardRepository.findByIdAndDeletedFalse(boardId)
            ?: throw BoardNotFoundException("Board not found with id: $boardId")

        val saved = taskStateRepository.saveAndRefresh(buildAndCheck(request))

        board.taskStates.add(saved)

        val savedBoard = boardRepository.save(board)

        val savedState = savedBoard.taskStates.last()

        return taskStateMapper.toDto(savedState)
    }

    override fun createDefaultStates(board: Board) {
        val defaultStates = taskStateRepository.findDefaultStates() // TODO, IN_PROGRESS, DONE

        board.taskStates.addAll(defaultStates)
        boardRepository.save(board)
    }

    override fun getById(id: Long): TaskStateResponse {
        val taskState = getByIdOrThrow(id)
        return taskStateMapper.toDto(taskState)
    }

    override fun getAll(): List<TaskStateResponse> {
        return taskStateRepository.findAllNotDeleted()
            .map { taskState -> taskStateMapper.toDto(taskState) }
    }

    override fun update(id: Long, request: TaskStateUpdateRequest) {
        val taskState = getByIdOrThrow(id)

        request.name?.let { taskState.name = it }
        request.code?.let { taskState.code = it }

        taskStateRepository.save(taskState)
    }
 // todo    bor stateni biror boardga bog'lash
    override fun delete(id: Long) {
        getByIdOrThrow(id)
        taskStateRepository.trash(id)
    }

    private fun buildAndCheck(request: TaskStateCreateRequest): TaskState {
        if (taskStateRepository.existsByNameAndDeletedFalse(request.name))
            throw TaskStateNotFoundException("TaskState with this name already exists")

        if (taskStateRepository.existsByCodeAndDeletedFalse(request.code))
            throw TaskStateNotFoundException("TaskState with this name already exists")

        return TaskState(
            name = request.name,
            code = request.code,
        )
    }

    private fun getByIdOrThrow(id: Long): TaskState =
        taskStateRepository.findByIdAndDeletedFalse(id) ?:
            throw TaskStateNotFoundException("TaskState not found with id: $id")
}


interface TaskService {
    fun create(request: TaskCreateRequest): TaskResponse
    fun getById(taskId: Long): TaskResponse
    fun getAllByBoardId(boardId: Long): List<TaskResponse>
    fun update(taskId: Long, request: TaskUpdateRequest): TaskResponse
    fun updateState(taskId: Long, request: TaskStateChangeRequest): TaskResponse
    fun delete(taskId: Long): Boolean
}

@Service
class TaskServiceImpl(
    private val taskRepository: TaskRepository,
    private val boardRepository: BoardRepository,
    private val taskStateRepository: TaskStateRepository,
    private val taskMapper: TaskMapper,
    private val accountTaskService: AccountTaskServiceImpl,
    private val accountTaskRepository: AccountTaskRepository
) : TaskService {
    override fun create(request: TaskCreateRequest): TaskResponse {
        val board = getBoard(request.boardId)
        validateTaskUniqueness(request.name, board.id!!)
        val defaultState = getDefaultState(board)

        val task = buildTask(request, board, defaultState)
        val saved = taskRepository.save(task)

        val assigns = accountTaskService.getAccountIdsByTaskId(saved.id!!)

        return taskMapper.toDto(saved, assigns)
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
            ?: throw BoardNotFoundException("Board not found")

        return taskRepository.findAllByBoardIdAndDeletedFalse(boardId)
            .map { task ->
                val assigns = accountTaskService.getAccountIdsByTaskId(task.id!!)
                taskMapper.toDto(task, assigns)
            }
    }

    override fun update(taskId: Long, request: TaskUpdateRequest): TaskResponse {
        val task = getByIdOrThrow(taskId)
        applyUpdates(task, request)
        val saved = taskRepository.save(task)

        val assigns = accountTaskService.getAccountIdsByTaskId(taskId)
        return taskMapper.toDto(saved, assigns)
    }

    override fun updateState(taskId: Long, request: TaskStateChangeRequest): TaskResponse {
        val task = getByIdOrThrow(taskId)

        if (task.ownerAccountId==userId() || accountTaskRepository.existsByTaskIdAndAccountId(taskId, currentOrgId()!!)){
            val newState = getStateByCode(request.code)

            validateStateBelongsToBoard(task, newState)
            task.taskState = newState

            val saved = taskRepository.save(task)
            val assigns = accountTaskService.getAccountIdsByTaskId(taskId)

            return taskMapper.toDto(saved, assigns)
        }else{
            throw ForbiddenException("Forbidden")
        }

    }

    override fun delete(taskId: Long): Boolean {
        val task = getByIdOrThrow(taskId)

        val assigns = accountTaskRepository.findAllByTaskIdAndDeletedFalse(task.id!!)
        assigns.forEach { accountTaskRepository.trash(it.id!!) }

        return taskRepository.trash(task.id!!)
    }


    /**  PRIVATE HELPER  **/
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
        if (taskRepository.existsByNameAndBoardId(name, boardId))
            throw TaskAlreadyExistsException("Task with name '$name' already exists in this board")
    }

    private fun getDefaultState(board: Board): TaskState =
        board.taskStates.firstOrNull()
            ?: throw TaskStateNotFoundException("TaskState not found in board id: ${board.id}, name: ${board.name}")

    private fun buildTask(request: TaskCreateRequest, board: Board, state: TaskState) = Task(
            ownerAccountId = userId(),
            name = request.name,
            description = request.description,
            dueDate = request.dueDate,
            priority = request.priority,
            board = board,
            taskState = state
        )

    private fun getStateByCode(code: String): TaskState =
        taskStateRepository.findByCodeAndDeletedFalse(code)
            ?: throw TaskStateNotFoundException("TaskState not found with code=$code")

    private fun validateStateBelongsToBoard(task: Task, newState: TaskState) {
        if (task.board.taskStates.none { it.code == newState.code })
            throw TaskStateNotFoundException("State '${newState.code}' does not belong to this task's board")
    }
}

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
    private val organizationClient: OrganizationClient
) : AccountTaskService {
    override fun assignAccountToTask(request: AssignAccountToTaskRequest): AccountTaskResponse {
        // TODO: check EMP POSITION = task's owner
        val accountTask = checkAndBuild(request)
        val saved = accountTaskRepository.save(accountTask)
        return AccountTaskResponse(
            accountId = saved.accountId
        )
    }

    private fun checkAndBuild(request: AssignAccountToTaskRequest, ): AccountTask {

        val task = taskRepository.findByIdAndDeletedFalse(request.taskId)
            ?: throw TaskNotFoundException("Task not found with id: ${request.taskId}")
        if (task.ownerAccountId!=userId())
            throw ForbiddenException("You can't assign, you aren't task owner")

         //organizationClient.getEmp(EmpRequest(request.accountId, currentOrgId()!!))
         organizationClient.getEmp2(EmpRequest(request.accountId, currentOrgId()!!))

        if (accountTaskRepository.existsByTaskIdAndAccountId(request.taskId, request.accountId))
            throw AccountAlreadyAssignedException("Account ${request.accountId} is already assigned to task ${request.taskId}")

        return AccountTask(
            accountId = request.accountId,
            task = task
        )
    }

    override fun getAccountIdsByTaskId(taskId: Long): MutableList<Long> =
        accountTaskRepository.findAllByTaskIdAndDeletedFalse(taskId)
            .map { it.accountId }
            .toMutableList()

    override fun getAllByTaskId(taskId: Long): List<AccountTaskResponse> =
        accountTaskRepository.findAllByTaskIdAndDeletedFalse(taskId)
            .map { AccountTaskResponse(it.accountId) }
    // TODO: check EMP POSITION = task's owner
    override fun disallow(taskId: Long, accountId: Long) {
        val entity = accountTaskRepository.findByTaskIdAndAccountIdAndDeletedFalse(taskId, accountId)
            ?: throw AccountTaskNotFoundException("Account $accountId isn't assigned to task $taskId")

        accountTaskRepository.trash(entity.id!!)
    }
}
fun checkPosition( organizationClient: OrganizationClient){
    val emp = organizationClient.getEmp(EmpRequest(userId(), currentOrgId()!!))

    if (emp.position=="ORG_EMPLOYEE")
        throw ForbiddenException("Forbidden Exception")
}
