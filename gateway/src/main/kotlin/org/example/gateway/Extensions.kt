package org.example.gateway


import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.Exception
import java.lang.IllegalArgumentException

fun ObjectMapper.parseMap(data: String?): Map<String, Any?> {
    try {
        data ?: return emptyMap()
        return this.readValue(data, object : TypeReference<MutableMap<String, Any?>>() {})
    } catch (ex: Exception) {
        throw IllegalArgumentException(ex.message, ex)
    }
}

fun String.extractServiceName(): String? {
    val regex = Regex("^/api/v\\d+/([^/]+)")
    val match = regex.find(this)
    return match?.groupValues?.get(1)
}


fun <T> Class<T>.getIsRoutedKey(): String {
    return "${this.name}.entered"
}
