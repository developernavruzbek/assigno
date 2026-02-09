package org.example.auth.components

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.stereotype.Component
import org.example.auth.model.security.AuthServerAuthenticationToken
import org.example.auth.services.security.AuthServerProvider
import org.example.auth.services.security.JpaOAuth2AuthorizationService
import java.security.Principal
import java.time.Instant
import java.util.*


@Component
class AuthServerAuthenticationProvider(
    private val authServerProviders: List<AuthServerProvider>,
    private val tokenGenerator: OAuth2TokenGenerator<out OAuth2Token>,
    private val authorizationService: JpaOAuth2AuthorizationService
) : AuthenticationProvider {

    private val providerMap = authServerProviders.associateBy { it.grantType() }

    override fun authenticate(authentication: Authentication): Authentication {
        val authenticationToken = authentication as AuthServerAuthenticationToken
        val grantType = authenticationToken.grantTypes
        val clientPrincipal = authentication.getClientPrincipal()
        val registeredClient = clientPrincipal.registeredClient
            ?: throw OAuth2AuthenticationException("Invalid client")
        var authorization = providerMap[grantType]?.provide(registeredClient, authenticationToken)
            ?: throw OAuth2AuthenticationException("${grantType.key} provider not found")

        val savedAuthorization = authorizationService
            .findByClientIdAndPrincipalName(
            authorization.registeredClientId,
            authorization.principalName
        )

        if (savedAuthorization != null && savedAuthorization.accessToken.isActive) {
            val accessToken = savedAuthorization.accessToken.token
            val refreshToken = savedAuthorization.refreshToken?.token
            return OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientPrincipal,
                accessToken,
                refreshToken)
        }

        val principal = authorization.getAttribute<Authentication>(Principal::class.java.name)

        @Suppress("DEPRECATION")
        val tokenContextBuilder = DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(principal)
            .authorizationServerContext(AuthorizationServerContextHolder.getContext())
            .authorization(authorization)
            .authorizedScopes(authorization.authorizedScopes)
            .authorizationGrantType(AuthorizationGrantType.PASSWORD)
            .authorizationGrant(authentication)

        val authorizationBuilder = OAuth2Authorization.from(authorization)

        // ----- Access token -----
        val tokenContext: OAuth2TokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build()
        val generatedAccessToken: OAuth2Token = tokenGenerator.generate(tokenContext)
            ?: throw OAuth2AuthenticationException("The token generator failed to generate the access token.")

        val accessToken = accessToken(authorizationBuilder, generatedAccessToken, tokenContext)

        // ----- Refresh token -----
        var refreshToken: OAuth2RefreshToken? = null
        if (registeredClient.authorizationGrantTypes.contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            val now = Instant.now()
            val expireTime = now.plusMillis(registeredClient.tokenSettings.refreshTokenTimeToLive.toMillis())
            refreshToken = OAuth2RefreshToken(UUID.randomUUID().toString(), now, expireTime)
            authorizationBuilder.refreshToken(refreshToken)
        }

        authorization = authorizationBuilder.build()
        authorizationService.save(authorization)
        return OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken, refreshToken)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return AuthServerAuthenticationToken::class.java.isAssignableFrom(authentication)
    }

    private fun accessToken(
        builder: OAuth2Authorization.Builder, token: OAuth2Token,
        accessTokenContext: OAuth2TokenContext
    ): OAuth2AccessToken {
        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, token.tokenValue,
            token.issuedAt, token.expiresAt, accessTokenContext.authorizedScopes
        )
        val accessTokenFormat = accessTokenContext.registeredClient
            .tokenSettings
            .accessTokenFormat
        builder.token(
            accessToken
        ) { metadata: MutableMap<String?, Any?> ->
            if (token is ClaimAccessor) {
                metadata[OAuth2Authorization.Token.CLAIMS_METADATA_NAME] = token.claims
            }
            metadata[OAuth2Authorization.Token.INVALIDATED_METADATA_NAME] = false
            metadata[OAuth2TokenFormat::class.java.name] = accessTokenFormat.value
//            metadata[]
        }

        return accessToken
    }
}