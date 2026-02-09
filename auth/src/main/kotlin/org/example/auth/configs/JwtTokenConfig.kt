package org.example.auth.configs

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.example.auth.utils.PemUtils
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

@Configuration
class JwtTokenConfig {
    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val rsaKey = RSAKey.Builder(loadPublicKey())
            .privateKey(loadPrivateKey())
            .keyID("d728cb1f-2042-48f7-9c61-2c26e37af5a2")
            .build()
        return ImmutableJWKSet(JWKSet(rsaKey))
    }

    @Bean
    fun jwtEncoder(jwkSource: JWKSource<SecurityContext>): JwtEncoder {
        return NimbusJwtEncoder(jwkSource)
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder.withPublicKey(loadPublicKey()).build()
    }

    private fun loadPrivateKey(): RSAPrivateKey {
        val keyBytes = ClassPathResource("keys/private_key.pem").inputStream.use {
            PemUtils.readPrivateKey(it)
        }
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun loadPublicKey(): RSAPublicKey {
        val keyBytes = ClassPathResource("keys/public_key.pem").inputStream.use {
            PemUtils.readPublicKey(it)
        }
        val keySpec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(keySpec) as RSAPublicKey
    }
}