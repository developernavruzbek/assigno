package org.example.organization

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class BaseMessage(
    val code: Long? = null,
    val message: String? = null
)

data class ChangeCurrentOrganizationRequest(
    val userId:Long,
    val newOrgId:Long
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
    val phoneNumber:String,
    val age:Long,
    val username: String,
    val role: String,
)

data class UserBatchRequest(
    val userIds: List<Long>
)


data class OrganizationCreateRequest(
    val name:String,
    val tagline:String,
    val address:String,
    val phoneNumber:String,
    )

data class OrganizationUpdateRequest(
    val name:String?,
    val tagline:String?,
    val address:String?,
    val phoneNumber:String?,
    val employeeCount: Int?
)

data class OrganizationResponse(
    val id:Long,
    val name:String,
    val tagline:String,
    val address:String,
    val phoneNumber:String,
    val code:String,
    val active: Boolean
)

data class EmployeeCreateRequest(
    val userId:Long,
    val organizationId:Long,
    val position: Position
)

//todo boshqattan korib chiqish kerak
data class EmployeeUpdateRequest(
    val userId:Long?,
    val organizationId:Long?,
    val position: Position?
)


data class EmployeeResponseOrganization(
    val id: Long,
    val userId:Long,
    val fullName:String,
    val phoneNumber:String,
    val age:Long,
    val position: Position
)

data class EmployeeResponse(
    val id: Long,
    val userId:Long,
    val organizationId:Long,
    val organizationName:String,
    val position: Position
)