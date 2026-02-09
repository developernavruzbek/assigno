package org.example.auth.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.ConfigurationSettingNames
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Component
import org.example.auth.model.requests.ClientAddRequest
import org.example.auth.model.requests.ClientUpdateRequest
import org.example.auth.models.responses.ClientListResponse
import org.example.auth.models.responses.ClientOneResponse
import org.example.auth.entities.AuthClient
import org.example.auth.enums.ClientGrantType
import org.example.auth.extensions.parseMap
import org.example.auth.extensions.writeMap
import java.time.Duration

@Component
class AuthClientMapper(
    private val passwordEncoder: PasswordEncoder,
) {
    private val objectMapper = ObjectMapper().apply {
        val classLoader = javaClass.getClassLoader()
        val securityModules = SecurityJackson2Modules.getModules(classLoader)
        registerModules(securityModules)
        registerModule(OAuth2AuthorizationServerJackson2Module())
    }

    fun toListResponse(authClient: AuthClient): ClientListResponse {
        return authClient.run {
            ClientListResponse(id, clientName, clientId, clientSecretExpiresAt, active)
        }
    }

    fun toOneResponse(authClient: AuthClient): ClientOneResponse {
        val client = toClient(authClient)
        return ClientOneResponse(
            id = client.id,
            name = client.clientName,
            clientId = client.clientId,
            clientSecretExpiresAt = client.clientSecretExpiresAt,
            active = authClient.active,
            accessTokenValiditySeconds = client.tokenSettings.accessTokenTimeToLive.seconds,
            refreshTokenValiditySeconds = client.tokenSettings.refreshTokenTimeToLive.seconds,
        )
    }

    fun toClient(id: String, request: ClientAddRequest): RegisteredClient {
        request.run {
            return RegisteredClient
                .withId(id)
                .clientId(clientId)
                .clientName(name)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientIdIssuedAt(request.clientSecretExpiresAt)
                .clientSettings(ClientSettings.builder().build())
                .authorizationGrantTypes {
                    ClientGrantType.entries.forEach { grantType ->
                        it.add(grantType.authorizationGrantType)
                    }
                }
                .tokenSettings(
                    TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(accessTokenValiditySeconds))
                        .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenValiditySeconds))
                        .build()
                ).build()
        }
    }

    fun toClient(id: String, authClient: AuthClient, request: ClientUpdateRequest): RegisteredClient {
        request.run {
            return RegisteredClient
                .withId(id)
                .clientId(clientId)
                .clientName(name)
                .clientSecret(authClient.clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientIdIssuedAt(request.clientSecretExpiresAt)
                .clientSettings(ClientSettings.builder().build())
                .authorizationGrantTypes {
                    ClientGrantType.entries.forEach { grantType ->
                        it.add(grantType.authorizationGrantType)
                    }
                }
                .tokenSettings(
                    TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(accessTokenValiditySeconds))
                        .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenValiditySeconds))
                        .build()
                ).build()
        }
    }

    fun toEntity(client: RegisteredClient) = client.run {
        AuthClient(
            id = id,
            clientId = clientId,
            clientIdIssuedAt = clientIdIssuedAt,
            clientSecret = clientSecret,
            clientSecretExpiresAt = clientSecretExpiresAt,
            clientName = clientName,
            clientAuthenticationMethods = clientAuthenticationMethods.map { it.value }.toSet(),
            authorizationGrantTypes = authorizationGrantTypes.map { it.value }.toSet(),
            redirectUris = redirectUris,
            postLogoutRedirectUris = postLogoutRedirectUris,
            scopes = scopes,
            clientSettings = objectMapper.writeMap(clientSettings.settings),
            tokenSettings = objectMapper.writeMap(tokenSettings.settings)
        )
    }

    fun toClient(authClient: AuthClient): RegisteredClient = authClient.run {
        val tokenSettingsMap: Map<String, Any?> = objectMapper.parseMap(tokenSettings)
        val tokenSettingsBuilder = TokenSettings.withSettings(tokenSettingsMap)
        if (!tokenSettingsMap.containsKey(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT)) {
            tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
        } else {
            when (val tokenFormat = tokenSettingsMap[ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT]) {
                is OAuth2TokenFormat -> {
                    tokenSettingsBuilder.accessTokenFormat(tokenFormat)
                }

                is LinkedHashMap<*, *> -> {
                    tokenSettingsBuilder.accessTokenFormat(
                        objectMapper.convertValue(tokenFormat, OAuth2TokenFormat::class.java)
                    )
                }

                is String -> {
                    tokenSettingsBuilder.accessTokenFormat(
                        objectMapper.convertValue(tokenFormat, OAuth2TokenFormat::class.java)
                    )
                }

                else -> {
                    tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                }
            }
        }

        RegisteredClient
            .withId(authClient.id)
            .clientId(clientId)
            .clientIdIssuedAt(clientIdIssuedAt)
            .clientSecret(clientSecret)
            .clientSecretExpiresAt(clientSecretExpiresAt)
            .clientName(clientName)
            .clientAuthenticationMethods { methods ->
                methods.addAll(clientAuthenticationMethods.map { ClientAuthenticationMethod(it) })
            }
            .authorizationGrantTypes { types ->
                types.addAll(authorizationGrantTypes.map { AuthorizationGrantType(it) })
            }
            .redirectUris { urls -> urls.addAll(redirectUris) }
            .postLogoutRedirectUris { uris -> uris.addAll(postLogoutRedirectUris) }
            .scopes { it.addAll(scopes) }
            .clientSettings(ClientSettings.withSettings(objectMapper.parseMap(clientSettings)).build())
            .tokenSettings(tokenSettingsBuilder.build())
            .build()

    }
}