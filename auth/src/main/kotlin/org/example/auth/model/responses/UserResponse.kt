package org.example.auth.model.responses


import org.example.auth.enums.UserStatus

data class UserResponse(
    val id: Long,
    val fullName: String,
    val phoneNumber:String,
    val age:Long,
    val username: String,
    val role: String,
    val status: UserStatus
)