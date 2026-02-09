package org.example.auth.utils

import org.bouncycastle.util.io.pem.PemReader
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.example.auth.constants.JWT_ROLE_KEY
import org.example.auth.constants.JWT_ROLE_LEVEL_KEY
import org.example.auth.constants.JWT_USER_ID_KEY
import java.io.InputStream
import java.io.InputStreamReader

fun getClientPrincipal(): OAuth2ClientAuthenticationToken? {
    val authentication = SecurityContextHolder.getContext().authentication

    if (authentication is OAuth2ClientAuthenticationToken) {
        return authentication
    }

    return null
}

fun getUserJwtPrincipal(): Jwt? {
    val principal = SecurityContextHolder.getContext().authentication?.principal

    if (principal is Jwt) {
        return principal
    }

    return null
}

fun userId(): Long {
    return getUserJwtPrincipal()?.claims?.get(JWT_USER_ID_KEY) as Long
}

fun userIdNullable(): Long? {
    return getUserJwtPrincipal()?.claims?.get(JWT_USER_ID_KEY) as? Long
}

fun role(): String {
    return getUserJwtPrincipal()?.claims?.get(JWT_ROLE_KEY) as String
}

fun roleLevel(): Int {
    return (getUserJwtPrincipal()?.claims?.get(JWT_ROLE_LEVEL_KEY) as Long).toInt()
}

object PemUtils {
    fun readPrivateKey(input: InputStream): ByteArray {
        PemReader(InputStreamReader(input)).use { reader ->
            val pem = reader.readPemObject()
            return pem.content
        }
    }

    fun readPublicKey(input: InputStream): ByteArray {
        PemReader(InputStreamReader(input)).use { reader ->
            val pem = reader.readPemObject()
            return pem.content
        }
    }
}

fun getHeader(headerKey: String): String? {
    return try {
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        request.getHeader(headerKey)
    } catch (e: Exception) {
        null
    }
}

