package org.example.auth.enums

import org.springframework.security.oauth2.core.AuthorizationGrantType

enum class ClientGrantType(val authorizationGrantType: AuthorizationGrantType) {
    JWT(AuthorizationGrantType.JWT_BEARER), REFRESH_TOKEN(AuthorizationGrantType.REFRESH_TOKEN);
}