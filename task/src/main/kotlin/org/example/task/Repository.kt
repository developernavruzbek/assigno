package org.example.task

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.security.access.method.P
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> :
    JpaRepository<T, Long>,
    JpaSpecificationExecutor<T> {

    fun findByIdAndDeletedFalse(id: Long): T?

    fun trash(id: Long): Boolean  // <-- boolean

    fun findAllNotDeleted(): List<T>
    fun findAllNotDeletedForPageable(pageable: Pageable): Page<T>

    fun saveAndRefresh(t: T): T
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    private val isNotDeletedSpecification = Specification<T> {
            root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false)
    }

    override fun findByIdAndDeletedFalse(id: Long) =
        findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): Boolean {
        val entity = findByIdOrNull(id) ?: return false
        if (entity.deleted) return false

        entity.deleted = true
        save(entity)
        return true
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)

    override fun findAllNotDeletedForPageable(pageable: Pageable): Page<T> =
        findAll(isNotDeletedSpecification, pageable)

    @Transactional
    override fun saveAndRefresh(t: T): T =
        save(t).apply { entityManager.refresh(this) }
}


@Repository
interface ProjectRepository: BaseRepository<Project>{
    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long): List<Project>
    fun findAllByDeletedFalse(): List<Project>
    fun existsByNameAndOrganizationId(name: String, organizationId: Long): Boolean
}

@Repository
interface BoardRepository : BaseRepository<Board> {
    fun existsByNameAndProjectId(name: String, projectId: Long): Boolean
    fun findAllByProjectIdAndDeletedFalse(projectId: Long): List<Board>
}
@Repository
interface TaskStateRepository : BaseRepository<TaskState> {
    @Query("select t from TaskState t where t.code in ('TODO', 'IN_PROGRESS', 'DONE') and t.deleted = false")
    fun findDefaultStates(): List<TaskState>
    fun findByCodeAndDeletedFalse(code: String): TaskState?
    fun existsByCode(code: String): Boolean
    fun existsByNameAndDeletedFalse(name: String): Boolean
    fun existsByCodeAndDeletedFalse(code: String): Boolean
}

@Repository
interface TaskRepository : BaseRepository<Task> {
    fun findAllByBoardIdAndDeletedFalse(boardId: Long): List<Task>
    fun existsByNameAndBoardId(name: String, boardId: Long): Boolean
}

@Repository
interface AccountTaskRepository: BaseRepository<AccountTask> {
    fun existsByTaskIdAndAccountId(taskId: Long, accountId: Long): Boolean
    fun findByTaskIdAndAccountIdAndDeletedFalse(taskId: Long, accountId: Long): AccountTask?
    fun findAllByTaskId(taskId: Long): List<AccountTask>
    fun deleteByTaskIdAndAccountId(taskId: Long, accountId: Long)
    fun findAllByTaskIdAndDeletedFalse(taskId: Long): List<AccountTask>
}
