package org.example.auth.model.requests

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.example.auth.enums.Role
import org.example.auth.utils.NotSpace


data class UserCreateRequest(
    val username: String,

    @field:Size(min = 8)
    @field:Pattern.List(
        value = [
            Pattern(regexp = ".*[a-z].*", message = "PASSWORD_LOWER_CASE_ERROR"),
            Pattern(regexp = ".*[A-Z].*", message = "PASSWORD_UPPER_CASE_ERROR"),
            Pattern(regexp = ".*\\d.*", message = "PASSWORD_DIGIT_ERROR"),
            Pattern(
                regexp = ".*[~!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*",
                message = "PASSWORD_EXTRA_CHARACTER_ERROR"
            )
        ]
    )
    val password: String,

    @field:Size(max = 255)
    val fullName: String,

    val phoneNumber: String,

    val age: Long,
)

