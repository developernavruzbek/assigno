package org.example.auth.model.requests

data class LoginRequest(
    val username:String,
    val password:String
)