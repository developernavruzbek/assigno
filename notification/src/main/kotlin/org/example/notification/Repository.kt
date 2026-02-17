package org.example.notification

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull


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

    private val isNotDeletedSpecification = Specification<T> { root, _, cb ->
        cb.equal(root.get<Boolean>("deleted"), false)
    }

    override fun findByIdAndDeletedFalse(id: Long) =
        findByIdOrNull(id)?.run { if (this.deleted) null else this }

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

interface TelegramConnectionRepository : BaseRepository<TelegramConnection> {
    fun findByLinkToken(linkToken: String): TelegramConnection?
}

interface NotificationMessageRepository : BaseRepository<NotificationMessage> {
    fun findAllByStatus(status: NotificationStatus): List<NotificationMessage>
}