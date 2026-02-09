package org.example.task

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import kotlin.apply
import kotlin.jvm.java
import kotlin.run


@Configuration
class ResourceServerConfig(
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun resourceServerFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests {
                it.requestMatchers("/error").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }

        return http.build()
    }


    fun jwtAuthenticationConverter(): Converter<Jwt, JwtAuthenticationToken> {
        return Converter<Jwt, JwtAuthenticationToken> { source ->
            source
            val userDetailsJson = getHeader(USER_DETAILS_HEADER_KEY)?.decompress()
            val userDetails = userDetailsJson?.run { objectMapper.readValue(this, UserInfoResponse::class.java) }
            val username = userDetails?.username ?: username()
            val authorities = mutableListOf<SimpleGrantedAuthority>()
            if (userDetails != null) {
                authorities.add(SimpleGrantedAuthority("ROLE_${userDetails.role}"))
            }
            JwtAuthenticationToken(source, authorities, username).apply {
                details = userDetails
            }
        }
    }
}


