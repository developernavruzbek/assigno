package org.example.auth.model.responses

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserInfoResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val currentOrgId:Long,
    val role: String,
)