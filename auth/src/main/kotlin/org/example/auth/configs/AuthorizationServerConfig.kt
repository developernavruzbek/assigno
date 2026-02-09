package org.example.auth.configs

import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2TokenEndpointConfigurer
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.*
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AccessTokenResponseAuthenticationSuccessHandler
import org.springframework.security.web.SecurityFilterChain
import org.example.auth.constants.JWT_CURRENT_ORG_ID_KEY
import org.example.auth.constants.JWT_ROLE_KEY
import org.example.auth.constants.JWT_USER_ID_KEY
import org.example.auth.components.AuthServerAuthenticationConverter
import org.example.auth.components.AuthServerAuthenticationProvider
import org.example.auth.components.JwtAuthenticationConverter
import uz.zero.auth.model.security.CustomUserDetails

@Configuration
class AuthorizationServerConfig {

    @Bean
    fun authorizationServerSettings(authorizationServerProperties: OAuth2AuthorizationServerProperties): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder().issuer(authorizationServerProperties.issuer).build()
    }

    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(
        http: HttpSecurity,
        passwordGrantAuthenticationConverter: AuthServerAuthenticationConverter,
        passwordGrantAuthenticationProvider: AuthServerAuthenticationProvider,
        tokenHandler: OAuth2AccessTokenResponseAuthenticationSuccessHandler
    ): SecurityFilterChain {
        val authorizationServerConfigurer =
            OAuth2AuthorizationServerConfigurer.authorizationServer()

        http
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .authorizeHttpRequests {
                it
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .with(authorizationServerConfigurer) { authorizationServer: OAuth2AuthorizationServerConfigurer ->
                authorizationServer
                    .tokenEndpoint { tokenEndpoint: OAuth2TokenEndpointConfigurer ->
                        tokenEndpoint
                            .accessTokenRequestConverter(passwordGrantAuthenticationConverter)
                            .accessTokenResponseHandler(tokenHandler)
                            .authenticationProvider(passwordGrantAuthenticationProvider)
                    }
            }

        return http.build()
    }

    @Bean
    @Order(2)
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter
    ): SecurityFilterChain {
        return http
            .authorizeHttpRequests {
                it
                    .requestMatchers("/user/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/internal/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt {
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .csrf { csrf -> csrf.disable() }
            .build()
    }

    //Spring Authorization Serverda yaratilayotgan token
    // (JWT) ning ichiga qo'shimcha ma'lumotlarni qo'shib yuborish uchun ishlatiladigan interfeys.
    @Bean
    fun jwtCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> {
        return OAuth2TokenCustomizer { context ->
            if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
                val principal = context.getPrincipal<UsernamePasswordAuthenticationToken>()?.principal

                if (principal != null && principal is CustomUserDetails) {
                    context.claims.claim(JWT_USER_ID_KEY, principal.getUserId())
                    context.claims.claim(JWT_ROLE_KEY, principal.getRole())
                    context.claims.claim(JWT_CURRENT_ORG_ID_KEY, principal.getCurrentOrgId())
                }
            }
        }
    }


    @Bean
    fun delegatingOAuth2TokenGenerator(jwtEncoder: JwtEncoder): DelegatingOAuth2TokenGenerator {
        val tokenGenerator = JwtGenerator(jwtEncoder)
        tokenGenerator.setJwtCustomizer(jwtCustomizer())
        return DelegatingOAuth2TokenGenerator(tokenGenerator, OAuth2RefreshTokenGenerator())
    }

}