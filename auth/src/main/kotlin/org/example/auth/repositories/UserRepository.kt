package org.example.auth.repositories
import org.example.auth.User


interface UserRepository : BaseRepository<User> {
    fun findByUsernameAndDeletedFalse(username: String): User?
    fun findByUsername(username: String): User?
    fun existsByUsername(username: String): Boolean
    fun existsByPhoneNumber(phoneNumber: String): Boolean
    fun findByPhoneNumber(phoneNumber: String) :User?
}