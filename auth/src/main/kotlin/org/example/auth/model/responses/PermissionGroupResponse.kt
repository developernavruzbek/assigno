package org.example.auth.models.responses

data class PermissionGroupResponse(
    val id: Long,
    val key: String,
    val name: String,
    val description: String
)