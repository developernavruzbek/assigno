package org.example.auth.models.requests

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UserChangePasswordRequest(
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
    val password: String
)