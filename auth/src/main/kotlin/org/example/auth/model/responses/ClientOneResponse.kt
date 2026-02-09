package org.example.auth.models.responses

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class ClientOneResponse(
    val id: String,
    val name: String,
    val clientId: String,
    @field:JsonFormat(shape = JsonFormat.Shape.NUMBER_INT) val clientSecretExpiresAt: Instant?,
    val active: Boolean,
    val accessTokenValiditySeconds: Long,
    val refreshTokenValiditySeconds: Long
)