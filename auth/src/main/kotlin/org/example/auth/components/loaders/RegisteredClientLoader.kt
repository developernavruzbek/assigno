package org.example.auth.components.loaders

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.*

@Component
class RegisteredClientLoader(
    private val authClientRepository: RegisteredClientRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun run(vararg args: String?) {
        try {
            if (authClientRepository.findByClientId("ios") != null) return
            logger.info("Creating client...")
            val newClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("ios")
                .clientIdIssuedAt(Instant.now())
                .clientSecret(passwordEncoder.encode("test"))
                .clientName("ios")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantTypes {
                    it.add(AuthorizationGrantType.JWT_BEARER)
                    it.add(AuthorizationGrantType.REFRESH_TOKEN)
                }
                .clientSettings(ClientSettings.builder().build())
                .tokenSettings(
                    TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(2))
                        .refreshTokenTimeToLive(Duration.ofHours(6))
                        .build()
                )
                .build()

            authClientRepository.save(newClient)
            logger.info("Created client...")
        } catch (e: Exception) {
            logger.warn("Couldn't create client. Stacktrace: ${e.stackTraceToString()}")
        }
    }
}

