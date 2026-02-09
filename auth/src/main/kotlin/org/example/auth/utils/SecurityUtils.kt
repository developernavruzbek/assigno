package org.example.auth.utils

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import uz.zero.auth.model.security.CustomUserDetails


@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
abstract class LongMixin

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonDeserialize(using = CustomUserDetailsDeserializer::class)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
internal abstract class CustomUserDetailsMixin

class CustomUserDetailsDeserializer : JsonDeserializer<CustomUserDetails>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): CustomUserDetails {
        val mapper = jp.codec as ObjectMapper
        val root = mapper.readTree<JsonNode>(jp)

        fun requireField(field: String): JsonNode {
            return root.get(field)?.takeIf { !it.isNull || !it.isMissingNode }
                ?: throw IllegalArgumentException("Missing required field: $field")
        }

        val id = requireField("id").asLong()
        val username = requireField("username").asText()
        val password = requireField("password").asText()
        val role = requireField("role").asText()
        val enabled = requireField("enabled").asBoolean()
        val currentOrgId = root.get("currentOrgId")?.asLong() ?: 0L

        return CustomUserDetails(
            id = id,
            username = username,
            password = password,
            role = role,
            enabled = enabled,
            currentOrgId = currentOrgId   // ← constructor ga beriladi
        )
    }
}
