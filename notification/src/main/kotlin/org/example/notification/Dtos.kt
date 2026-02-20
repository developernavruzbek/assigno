package org.example.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.*

data class BaseMessage(
    val code: Long? = null,
    val message: String? = null
)

data class ChangeCurrentOrganizationRequest(
    @field:NotNull(message = "User ID is required")
    var userId: Long,

    @field:NotNull(message = "New organization ID is required")
    var newOrgId: Long
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserInfoResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val role: String,
)

data class UserResponse(
    val id: Long,
    val fullName: String,
    val phoneNumber: String,
    val age: Long,
    val username: String,
    val role: String,
)

data class UserBatchRequest(
    @field:NotEmpty(message = "User ID list cannot be empty")
    val userIds: List<Long>
)

data class EmpRequest(
    val userId:Long,
    val orgId:Long
)

data class EmpResponse(
    val id:Long,
    val userId:Long,
    val orgId:Long,
    val position: String

)

data class AccountTaskResponse(
    val accountId: Long
)


data class ActionRequest(
    val taskId: Long,
    val taskOwnerId: Long,
    val content: String
)

