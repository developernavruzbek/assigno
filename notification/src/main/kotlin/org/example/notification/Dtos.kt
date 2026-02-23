package org.example.notification

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

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
    val position: String,
    val localNumber: Long
)

data class ActionRequest(
    val content: String,
    val employeeLocalNumbers: List<Long>
)

data class OrganizationResponse(
    val id: Long,
    val name: String
)
