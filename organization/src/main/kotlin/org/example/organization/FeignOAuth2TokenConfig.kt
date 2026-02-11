package org.example.organization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import feign.RequestInterceptor
import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken


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
    fun feignErrorDecoder(objectMapper: ObjectMapper): ErrorDecoder = FeignErrorDecoder(objectMapper)
}

class FeignErrorDecoder(private val objectMapper: ObjectMapper) : ErrorDecoder {
    override fun decode(methodKey: String?, response: Response): Exception {
        val status = response.status()
        val body = try {
            response.body()?.asInputStream()?.readAllBytes()?.toString(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }

        var code: Long? = null
        var message: String? = null

        if (!body.isNullOrBlank()) {
            try {
                val node = objectMapper.readTree(body)
                if (node is ObjectNode) {
                    if (node.has("code") && !node.get("code").isNull) {
                        val codeNode = node.get("code")
                        code = when {
                            codeNode.isIntegralNumber -> codeNode.asLong()
                            codeNode.isTextual -> codeNode.asText().toLongOrNull()
                            else -> null
                        }
                    }
                    if (node.has("message") && !node.get("message").isNull) {
                        message = node.get("message").asText()
                    }
                }
            } catch (_: Exception) {
                // ignore parsing issues; will fallback below
            }
        }

        val finalMessage = message ?: "Downstream service error (HTTP $status)"
        return FeignClientException(code, finalMessage, status)
    }
}

class FeignClientException(val code: Long?, override val message: String, val httpStatus: Int) :
    RuntimeException(message)

