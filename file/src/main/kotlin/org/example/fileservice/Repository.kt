package org.example.fileservice

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uz.zero.fileservice.File

@Repository
interface FileRepo : JpaRepository<File, Long> {
    fun findAllByTaskIdAndDeletedFalse(ownerId: Long): List<File>
    fun findByKeyName(keyName: String): File?
    fun existsByKeyName(keyName: String): Boolean
}