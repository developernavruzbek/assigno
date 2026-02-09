package org.example.auth.entities

import jakarta.persistence.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.oauth2.core.AuthorizationGrantType
import java.time.Instant

@Document
@CompoundIndex(def = "{ 'registeredClientId': 1 'principalName': 1}", unique = true)
class AuthAuthorization(
    @Id var id: String,
    var registeredClientId: String,
    var principalName: String,
    var authorizationGrantType: AuthorizationGrantType,
    var authorizedScopes: MutableSet<String>,
    var attributes: String?,
    @Indexed(unique = true) var accessTokenValue: String,
    var accessTokenIssuedAt: Instant?,
    var accessTokenExpiresAt: Instant?,
    var accessTokenMetadata: String?,
    var accessTokenScopes: Set<String>,
    @Indexed(unique = true) var refreshTokenValue: String?,
    var refreshTokenIssuedAt: Instant?,
    var refreshTokenExpiresAt: Instant?,
    var refreshTokenMetadata: String?,
)