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
        val log = org.slf4j.LoggerFactory.getLogger(ErrorDecoder::class.java)

        return ErrorDecoder { methodKey, response ->
            val errorMessage = try {
                response.body()?.asInputStream()?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            }

            log.error("Feign xatosi: Metod=$methodKey, Status=${response.status()}, Xabar=$errorMessage")

            when (response.status()) {
                400 -> ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage ?: "Bad request sent")
                401 -> ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization Error (Token may be expired)")
                403 -> ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action")
                404 -> ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage ?: "Resource not found")
                // 429 - Too Many Requests (Exceeding Limit)
                429 -> ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Request limit exceeded")
                500 -> ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Technical error in internal service")
                else -> ErrorDecoder.Default().decode(methodKey, response)
            }
        }
    }
}

