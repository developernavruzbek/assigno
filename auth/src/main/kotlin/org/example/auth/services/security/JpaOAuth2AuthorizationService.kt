package org.example.auth.services.security

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService

interface JpaOAuth2AuthorizationService : OAuth2AuthorizationService {
    fun findByClientIdAndPrincipalName(clientId: String, principalName: String): OAuth2Authorization?
}
