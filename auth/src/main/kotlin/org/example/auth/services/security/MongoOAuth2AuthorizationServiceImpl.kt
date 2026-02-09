package org.example.auth.services.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.auth.constants.PRINCIPAL_KEY
import org.example.auth.entities.AuthAuthorization
import org.example.auth.extensions.parseMap
import org.example.auth.extensions.writeMap


import org.example.auth.repositories.AuthAuthorizationRepository
import org.example.auth.utils.CustomUserDetailsMixin
import org.example.auth.utils.LongMixin
import uz.zero.auth.model.security.CustomUserDetails

@Service
class MongoOAuth2AuthorizationServiceImpl(
    private val repository: AuthAuthorizationRepository,
    private val registeredClientRepository: RegisteredClientRepository,
) : JpaOAuth2AuthorizationService {

    private val objectMapper = ObjectMapper().apply {
        val classLoader = javaClass.getClassLoader()
        val securityModules = SecurityJackson2Modules.getModules(classLoader)
        registerModules(securityModules)
        addMixIn(Long::class.javaObjectType, LongMixin::class.java)
        addMixIn(CustomUserDetails::class.java, CustomUserDetailsMixin::class.java)
        registerModule(OAuth2AuthorizationServerJackson2Module())
    }

    override fun findByClientIdAndPrincipalName(clientId: String, principalName: String): OAuth2Authorization? {
        val authorization = repository.findByRegisteredClientIdAndPrincipalName(clientId, principalName)
            ?: return null
        return toAuthorization(authorization)
    }

    @Transactional
    override fun save(authorization: OAuth2Authorization) {
        val oldAuthorization = repository.findByRegisteredClientIdAndPrincipalName(
            authorization.registeredClientId,
            authorization.principalName
        )

        if (oldAuthorization != null) {
            remove(toAuthorization(oldAuthorization))
        }

        repository.save(toEntity(authorization))
    }

    override fun remove(authorization: OAuth2Authorization) {
        repository.deleteById(authorization.id)
    }

    override fun findById(id: String): OAuth2Authorization? {
        return toAuthorization(repository.findByIdOrNull(id) ?: return null)
    }

    override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
        val authorization = when {
            OAuth2TokenType.ACCESS_TOKEN.equals(tokenType) -> {
                repository.findByAccessTokenValue(token)
            }

            OAuth2TokenType.REFRESH_TOKEN.equals(tokenType) -> {
                repository.findByRefreshTokenValue(token)
            }

            else -> null
        }
        return authorization?.run { toAuthorization(this) }
    }

    private fun toAuthorization(entity: AuthAuthorization): OAuth2Authorization {
        entity.run {
            val accessToken = OAuth2AccessToken(
                TokenType.BEARER,
                accessTokenValue,
                accessTokenIssuedAt,
                accessTokenExpiresAt,
                authorizedScopes
            )

            val refreshToken = OAuth2RefreshToken(
                refreshTokenValue, refreshTokenIssuedAt, refreshTokenExpiresAt
            )

            val registeredClient = registeredClientRepository.findById(registeredClientId)
                ?: throw AuthenticationServiceException("Invalid client")

            val authorization = OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .id(entity.id)
                .principalName(principalName)
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(authorizationGrantType)
                .token(accessToken) {
                    it.putAll(objectMapper.parseMap(this.accessTokenMetadata))
                }.token(refreshToken) {
                    it.putAll(objectMapper.parseMap(this.refreshTokenMetadata))
                }
                .attributes { attributes ->
                    val attributesMap = objectMapper.parseMap(this.attributes).toMutableMap()
                    val principal = attributesMap[PRINCIPAL_KEY]
                    if (principal != null && principal is Map<*, *>) {
                        attributesMap[PRINCIPAL_KEY] =
                            objectMapper.convertValue(principal, UsernamePasswordAuthenticationToken::class.java)
                    }
                    attributes.putAll(attributesMap)
                }
                .build()

            return authorization
        }
    }

    private fun toEntity(authorization: OAuth2Authorization): AuthAuthorization {
        val accessToken = authorization.accessToken.token
        val refreshToken = authorization.refreshToken?.token
        return AuthAuthorization(
            id = authorization.id,
            registeredClientId = authorization.registeredClientId,
            principalName = authorization.principalName,
            authorizationGrantType = authorization.authorizationGrantType,
            authorizedScopes = authorization.authorizedScopes,
            accessTokenValue = accessToken.tokenValue,
            accessTokenIssuedAt = accessToken.issuedAt,
            accessTokenExpiresAt = accessToken.expiresAt,
            accessTokenMetadata = objectMapper.writeMap(authorization.accessToken.metadata),
            accessTokenScopes = accessToken.scopes,
            refreshTokenValue = refreshToken?.tokenValue,
            refreshTokenIssuedAt = refreshToken?.issuedAt,
            refreshTokenExpiresAt = refreshToken?.expiresAt,
            attributes = objectMapper.writeMap(authorization.attributes),
            refreshTokenMetadata = objectMapper.writeMap(authorization.refreshToken?.metadata),
        )
    }
}