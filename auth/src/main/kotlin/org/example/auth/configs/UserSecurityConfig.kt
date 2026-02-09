package org.example.auth.configs

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AccessTokenResponseAuthenticationSuccessHandler
import java.time.Instant
import java.time.temporal.ChronoUnit

@Configuration
class UserSecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
    @Bean
    @Primary
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper {
        val objectMapper = builder.createXmlMapper(false).build<ObjectMapper>()
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return objectMapper
    }

    @Bean
    fun tokenHandler(): OAuth2AccessTokenResponseAuthenticationSuccessHandler {
        val tokenHandler = OAuth2AccessTokenResponseAuthenticationSuccessHandler()
        tokenHandler.setAccessTokenResponseCustomizer {
            val authentication: Authentication = it.getAuthentication()
            if (authentication is OAuth2AccessTokenAuthenticationToken) {
                val accessToken = authentication.accessToken
                it.accessTokenResponse.expiresIn(ChronoUnit.SECONDS.between(Instant.now(), accessToken.expiresAt))
            }
        }
        return tokenHandler
    }

    @Bean
    fun errorMessageSource() = ResourceBundleMessageSource().apply {
        setDefaultEncoding(Charsets.UTF_8.name())
        setBasename("error")
    }

}