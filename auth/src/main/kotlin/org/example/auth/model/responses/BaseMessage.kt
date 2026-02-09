package org.example.auth.models.responses

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseMessage(
    val code: Int?,
    val message: String?,
    val fields: MutableList<ValidationFieldError>? = null
) {
    companion object {
        val OK = BaseMessage(0, "OK")
    }

    data class ValidationFieldError(
        val field: String,
        val message: String?
    )
}