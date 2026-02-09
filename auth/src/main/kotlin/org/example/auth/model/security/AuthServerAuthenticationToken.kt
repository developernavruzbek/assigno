package org.example.auth.model.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.example.auth.enums.AuthServerGrantType

class AuthServerAuthenticationToken(
    val parameters: Map<String, Array<String>>,
    val grantTypes: AuthServerGrantType,
    private val clientPrincipal: OAuth2ClientAuthenticationToken
) : AbstractAuthenticationToken(null) {
    override fun getCredentials(): Any {
        return parameters
    }

    override fun getPrincipal(): Any {
        return grantTypes
    }

    fun getClientPrincipal(): OAuth2ClientAuthenticationToken {
        return clientPrincipal
    }

}