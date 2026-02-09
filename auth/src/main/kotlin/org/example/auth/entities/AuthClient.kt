package org.example.auth.entities

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@CompoundIndex(def = "{ 'clientId': 1 'active': 1}", unique = true)
class AuthClient(
    @Id var id: String,
    @Indexed(unique = true) var clientId: String,
    var clientIdIssuedAt: Instant?,
    var clientSecret: String?,
    var clientSecretExpiresAt: Instant? = null,
    var clientName: String? = null,
    var clientAuthenticationMethods: Set<String>,
    var authorizationGrantTypes: Set<String>,
    var redirectUris: Set<String>,
    var postLogoutRedirectUris: Set<String>,
    var scopes: Set<String>,
    var clientSettings: String?,
    var tokenSettings: String?,
    var active: Boolean = true
)