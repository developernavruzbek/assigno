package org.example.task

import feign.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken


class FeignOAuth2TokenConfig {
    @Bean
    fun feignOAuth2TokenInterceptor() = RequestInterceptor { requestTemplate ->
        val userDetails = getHeader(USER_DETAILS_HEADER_KEY)
        val accessToken = SecurityContextHolder
            .getContext()
            .authentication as JwtAuthenticationToken

        requestTemplate.header(HttpHeaders.AUTHORIZATION, "${BEARER.value} ${accessToken.token.tokenValue}")
        requestTemplate.header(USER_DETAILS_HEADER_KEY, userDetails)
    }
}

