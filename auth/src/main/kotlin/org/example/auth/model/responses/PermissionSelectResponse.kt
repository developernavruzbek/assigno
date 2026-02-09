package org.example.auth.models.responses

data class PermissionSelectResponse(
    val id: Long,
    val key: String,
    val name: String,
    val description: String,
    val isSelected: Boolean
)
