package org.example.auth.models.responses

data class UserPermissionsResponse(
    val group: PermissionGroupResponse,
    val permissions: Set<PermissionSelectResponse>
)