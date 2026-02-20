package org.example.task

import feign.RequestInterceptor
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.server.ResponseStatusException

@Configuration
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

    @Bean
    fun errorDecoder(): ErrorDecoder {
        return ErrorDecoder { methodKey, response ->
            when (response.status()) {
                400 -> ResponseStatusException(HttpStatus.BAD_REQUEST, "Internal service: Bad request sent")
                401 -> ResponseStatusException(HttpStatus.UNAUTHORIZED, "Internal Service: Authorization Error")
                403 -> ResponseStatusException(HttpStatus.FORBIDDEN, "Internal Service: Access Denied")
                404 -> ResponseStatusException(HttpStatus.NOT_FOUND, "Internal service: Resource not found")
                500 -> ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Technical error in internal service")
                else -> ErrorDecoder.Default().decode(methodKey, response)
            }
        }
    }
}

