package org.example.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.*

data class BaseMessage(
    val code: Long? = null,
    val message: String? = null
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class UserInfoResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val role: String,
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


data class ActionRequest(
    val taskId: Long,
    val ownerId: Long,
    val content: String,
    val employees: List<Long>
)

data class OrganizationResponse(
    val id: Long,
    val name: String
)
