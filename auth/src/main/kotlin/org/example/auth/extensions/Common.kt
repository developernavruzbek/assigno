package org.example.auth.extensions

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.oauth2.jwt.Jwt
import org.example.auth.constants.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


fun String.hashed(): String {
    val digest: MessageDigest
    try {
        digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(toByteArray())
        return String.format("%032x", BigInteger(1, bytes))
    } catch (nsae: NoSuchAlgorithmException) {
        throw IllegalStateException("SHA-256 algorithm not available.  Fatal (should be in the JDK).", nsae)
    } catch (uee: UnsupportedEncodingException) {
        throw IllegalStateException("UTF-8 encoding not available.  Fatal (should be in the JDK).", uee)
    }
}

fun Map<String, Any?>.replaceDot(): Map<String, Any?> {
    return map {
        return@map it.key.replaceDot() to it.value
    }.toMap()
}

fun Map<String, Any?>.reverseDot(): Map<String, Any?> {
    return map { (key, value) ->
        val key = key.reverseDot()
        val value = value
        if (value != null) {
            when {
                value is String -> {
                    return@map parse(key, value)
                }

                value is Map<*, *> && value.keys.all { it is String } -> {
                    return@map key to value.map { (key2, value2) ->
                        if (value2 is String) {
                            return@map parse(key2 as String, value2)
                        } else {
                            return@map key2 to value2
                        }
                    }.toMap()
                }
            }
        }
        return@map key to value
    }.toMap()
}

fun parse(key: String, value: String): Pair<String, Any> {
    return when {
        value.isDuration() -> key to Duration.parse(value)
        value.isInstant() -> key to (value.parseInstant() ?: value)
        else -> key to value
    }
}

fun String.parseInstant(): Instant? {
    return try {
        Instant.parse(this)
    } catch (ex: Exception) {
        null
    }
}

private fun String.replaceDot() = this.replace(DOT, HASH)

private fun String.reverseDot() = this.replace(HASH, DOT)

fun String.isDuration(): Boolean {
    val durationPattern = Pattern.compile(
        "([-+]?)P(?:([-+]?[0-9]+)D)?(T(?:([-+]?[0-9]+)H)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?)?",
        Pattern.CASE_INSENSITIVE
    )
    return durationPattern.matcher(this).matches()
}

fun String.isInstant(): Boolean {
    val durationPattern = Pattern.compile("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z$""")
    return durationPattern.matcher(this).matches()
}

fun ObjectMapper.parseMap(data: String?): Map<String, Any> {
    if (data == null) return emptyMap()
    try {
        return readValue(
            data,
            object : TypeReference<Map<String, Any>>() {}
        )
    } catch (ex: Exception) {
        throw IllegalArgumentException(ex.message, ex)
    }
}

fun ObjectMapper.writeMap(data: Map<String, Any>?): String? {
    if (data == null) return null
    try {
        return writeValueAsString(data)
    } catch (ex: Exception) {
        throw IllegalArgumentException(ex.message, ex)
    }
}

fun Jwt.getUserId(): Long {
    return this.claims[JWT_USER_ID_KEY] as Long
}

fun Jwt.getUsername(): String {
    return this.claims[JWT_USERNAME_KEY] as String
}

fun Jwt.getCurrentOrgId(): Long {
    return this.claims[JWT_CURRENT_ORG_ID_KEY] as Long
}
fun String.compress(): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(this) }
    return Base64.getEncoder().encodeToString(bos.toByteArray())
}


fun String.decompress(): String {
    val bytes = Base64.getDecoder().decode(this)
    return GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }
}