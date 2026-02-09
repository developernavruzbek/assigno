package org.example.auth.components

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.stereotype.Component
import org.example.auth.enums.AuthServerGrantType
import org.example.auth.model.security.AuthServerAuthenticationToken
import org.example.auth.utils.getClientPrincipal


@Component
class AuthServerAuthenticationConverter : AuthenticationConverter {
    override fun convert(request: HttpServletRequest): Authentication? {
        val grantTypeParameter = request.getParameter("grant_type") ?: return null
        val grantType = AuthServerGrantType.findByKey(grantTypeParameter) ?: return null
        val clientPrincipal: OAuth2ClientAuthenticationToken = getClientPrincipal() ?: return null
        return AuthServerAuthenticationToken(request.parameterMap, grantType, clientPrincipal)
    }
}