package org.example.auth.models.responses

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class ClientListResponse(
    val id: String,
    val name: String?,
    val clientId: String,
    @field:JsonFormat(shape = JsonFormat.Shape.NUMBER_INT) val secretExpiresAt: Instant?,
    val active: Boolean
)