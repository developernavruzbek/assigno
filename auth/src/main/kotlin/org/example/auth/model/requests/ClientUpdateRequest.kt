package org.example.auth.model.requests

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class ClientUpdateRequest(
    val clientId: String,
    @field:JsonFormat(shape = JsonFormat.Shape.NUMBER_INT) val clientSecretExpiresAt: Instant?,
    val name: String,
    val accessTokenValiditySeconds: Long,
    val refreshTokenValiditySeconds: Long
)