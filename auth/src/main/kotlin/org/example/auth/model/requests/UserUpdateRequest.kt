package org.example.auth.models.requests

import jakarta.persistence.Column
import jakarta.validation.constraints.Size
import org.example.auth.enums.Role
import org.example.auth.utils.NotSpace


data class UserUpdateRequest(
    @field:Size(min = 3, max = 32)
    @field:NotSpace
    val username: String,
    val roleId: Long,
    @field:Size(max = 255) val fullName: String
)

data class UserUpdate(
    val fullName: String?,
    val phoneNumber: String?,
    val username: String?,
    val age :Long?,
    val role: Role?
)