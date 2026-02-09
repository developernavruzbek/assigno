package org.example.auth.services.security

import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.stereotype.Service
import org.example.auth.constants.PASSWORD_KEY
import org.example.auth.constants.PRINCIPAL_KEY
import org.example.auth.constants.USERNAME_KEY
import org.example.auth.enums.AuthServerGrantType
import org.example.auth.model.security.AuthServerAuthenticationToken


@Service
class PasswordAuthServerProvider(
    private val userDetailsService: UserDetailsService,
    private val passwordEncoder: PasswordEncoder
) : AuthServerProvider {
    companion object {

    }

    override fun provide(
        client: RegisteredClient,
        authenticationToken: AuthServerAuthenticationToken
    ): OAuth2Authorization {
        if (authenticationToken.grantTypes != grantType()) throw AuthenticationServiceException("Invalid auth server grantType")

        val username = authenticationToken.parameters[USERNAME_KEY]?.firstOrNull()
            ?: throw AuthenticationServiceException("Invalid username")
        val password = authenticationToken.parameters[PASSWORD_KEY]?.firstOrNull()
            ?: throw AuthenticationServiceException("Invalid username")

        val userDetails = userDetailsService.loadUserByUsername(username)
        if (!passwordEncoder.matches(password, userDetails.password))
            throw AuthenticationServiceException("User not found or invalid password")

        @Suppress("DEPRECATION")
        return OAuth2Authorization
            .withRegisteredClient(client)
            .principalName(username)
            .authorizationGrantType(AuthorizationGrantType.PASSWORD)
            .attributes {
                it[PRINCIPAL_KEY] =
                    UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
            }
            .build()

    }

    override fun grantType() = AuthServerGrantType.PASSWORD

}