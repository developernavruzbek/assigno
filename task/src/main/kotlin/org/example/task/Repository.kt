package org.example.task

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
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

    fun trash(id: Long)

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
    override fun trash(id: Long) {
        val entity = findByIdOrNull(id) ?: return
        if (entity.deleted) return

        entity.deleted = true
        save(entity)
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
    fun existsByNameAndOrganizationIdAndDeletedFalse(name: String, organizationId: Long): Boolean

    fun findAllByOrganizationIdAndDeletedFalse(organizationId: Long): List<Project>
    fun findAllByDeletedFalse(): List<Project>
}

@Repository
interface BoardRepository : BaseRepository<Board> {
    fun existsByNameAndProjectIdAndDeletedFalse(name: String, projectId: Long): Boolean

    fun findAllByProjectIdAndDeletedFalse(projectId: Long): List<Board>
}


@Repository
interface TaskStateRepository : BaseRepository<TaskState> {
    @Query("""
    select ts
    from Board b
    join b.taskStates ts
    where b.id = :boardId
      and ts.code = :code
      and ts.deleted = false
      and b.deleted = false
""")
    fun findByCodeAndBoardId(@Param("code") code: String, @Param("boardId") boardId: Long): TaskState?

    @Query("select t from TaskState t where t.code in ('NEW', 'IN_PROGRESS', 'REVIEW','DONE') and t.deleted = false")
    fun existsByCode(code: String): Boolean

    @Modifying
    @Transactional
    @Query("""
    update TaskState ts 
    set ts.position = ts.position + 1
    where ts.board.id = :boardId 
      and ts.position >= :from
      and ts.deleted = false
""")
    fun incrementPositions(boardId: Long, from: Int)

    @Query("""
    select ts from TaskState ts 
    where ts.board.id = :boardId and ts.position < :pos
    order by ts.position desc limit 1
""")
    fun findPrev(boardId: Long, pos: Int): TaskState?

    @Query("""
    select ts from TaskState ts 
    where ts.board.id = :boardId and ts.position > :pos
    order by ts.position asc limit 1
""")
    fun findNext(boardId: Long, pos: Int): TaskState?

    @Modifying
    @Transactional
    @Query("""
    update TaskState ts 
    set ts.position = ts.position - 1
    where ts.board.id = :boardId 
      and ts.position > :deletedPosition 
      and ts.deleted = false
""")
    fun decrementPositions(boardId: Long, deletedPosition: Int)

    @Query("SELECT MAX(t.position) FROM TaskState t WHERE t.board.id = :boardId AND t.deleted = false")
    fun findMaxPositionByBoardId(boardId: Long): Int?

    fun findByCodeAndBoardIdAndDeletedFalse(code: String, boardId: Long): TaskState?

    fun existsByNameAndBoardIdAndDeletedFalse(name: String, boardId: Long): Boolean
    fun existsByCodeAndBoardIdAndDeletedFalse(code: String, boardId: Long): Boolean
}

@Repository
interface TaskRepository : BaseRepository<Task> {
    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.deleted = true WHERE t.id IN :taskIds AND t.deleted = false")
    fun trashAllByIds(taskIds: List<Long>)

    fun existsByNameAndBoardIdAndDeletedFalse(name: String, boardId: Long): Boolean

    fun findAllByBoardIdAndDeletedFalse(boardId: Long): List<Task>
}

@Repository
interface AccountTaskRepository: BaseRepository<AccountTask> {
    fun existsByTaskIdAndAccountIdAndDeletedFalse(taskId: Long, accountId: Long): Boolean

    @Modifying
    @Transactional
    @Query("UPDATE AccountTask a SET a.deleted = true WHERE a.task.id IN :taskIds AND a.deleted = false")
    fun trashAllByTaskIds(taskIds: List<Long>)

    fun findAllByTaskIdInAndDeletedFalse(taskIds: List<Long>): List<AccountTask>
    fun findByTaskIdAndAccountIdAndDeletedFalse(taskId: Long, accountId: Long): AccountTask?
    fun findAllByTaskIdAndDeletedFalse(taskId: Long): List<AccountTask>
}

@Repository
interface TaskActionRepository: BaseRepository<TaskAction> {

}