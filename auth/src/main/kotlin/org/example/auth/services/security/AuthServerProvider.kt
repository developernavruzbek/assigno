package org.example.auth.services.security

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.example.auth.enums.AuthServerGrantType
import org.example.auth.model.security.AuthServerAuthenticationToken

interface AuthServerProvider {
    fun provide(client: RegisteredClient, authenticationToken: AuthServerAuthenticationToken): OAuth2Authorization
    fun grantType(): AuthServerGrantType
}
