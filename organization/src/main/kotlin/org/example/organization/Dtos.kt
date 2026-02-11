package org.example.organization

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.*

data class BaseMessage(
    val code: Long? = null,
    val message: String? = null
)

data class ChangeCurrentOrganizationRequest(
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @field:NotNull(message = "New organization ID is required")
    val newOrgId: Long
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


data class OrganizationCreateRequest(

    @field:NotBlank(message = "Name cannot be empty")
    @field:Size(max = 150, message = "Name must be 150 characters or less")
    val name: String,

    @field:Size(max = 250, message = "Tagline must be 250 characters or less")
    val tagline: String,

    @field:NotBlank(message = "Address cannot be empty")
    @field:Size(max = 172, message = "Address must be 172 characters or less")
    val address: String,

    @field:NotBlank(message = "Phone number cannot be empty")
    @field:Size(max = 20, message = "Phone number must be 20 characters or less")
    val phoneNumber: String,
)

data class OrganizationUpdateRequest(

    @field:Size(max = 150, message = "Name must be 150 characters or less")
    val name: String? = null,

    @field:Size(max = 250, message = "Tagline must be 250 characters or less")
    val tagline: String? = null,

    @field:Size(max = 172, message = "Address must be 172 characters or less")
    val address: String? = null,

    @field:Size(max = 20, message = "Phone number must be 20 characters or less")
    val phoneNumber: String? = null,

    @field:Min(value = 0, message = "Employee count cannot be negative")
    val employeeCount: Int? = null
)

data class OrganizationResponse(
    val id: Long,
    val name: String,
    val tagline: String,
    val address: String,
    val phoneNumber: String,
    val code: String,
    val active: Boolean
)


data class EmployeeCreateRequest(

    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @field:NotNull(message = "Organization ID is required")
    val organizationId: Long,

    @field:NotNull(message = "Position is required")
    val position: Position
)

// todo boshqattan korib chiqish kerak
data class EmployeeUpdateRequest(
    val userId: Long? = null,
    val organizationId: Long? = null,
    val position: Position? = null
)

data class EmployeeResponseOrganization(
    val id: Long,
    val userId: Long,
    val fullName: String,
    val phoneNumber: String,
    val age: Long,
    val position: Position
)

data class EmployeeResponse(
    val id: Long,
    val userId: Long,
    val organizationId: Long,
    val organizationName: String,
    val position: Position
)


data class EmpRequest(
    val userId:Long,
    val orgId:Long
)

data class EmpResponse(
    val id:Long,
    val userId:Long,
    val orgId:Long,
    val position: Position

)
