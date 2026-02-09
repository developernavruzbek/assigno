package org.example.auth.components

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.example.auth.constants.USER_DETAILS_HEADER_KEY
import org.example.auth.extensions.decompress
import org.example.auth.extensions.getUsername
import org.example.auth.model.responses.UserInfoResponse
import org.example.auth.utils.getHeader

@Component
class JwtAuthenticationConverter(
    private val objectMapper: ObjectMapper,
) : Converter<Jwt, JwtAuthenticationToken> {

    override fun convert(source: Jwt): JwtAuthenticationToken {

        // 1) Header bo‘lsa undan ol
        val userDetailsJson = getHeader(USER_DETAILS_HEADER_KEY)?.decompress()
        val userDetails = userDetailsJson?.run {
            objectMapper.readValue(this, UserInfoResponse::class.java)
        }

        val username = userDetails?.username ?: source.getUsername()
        val role = userDetails?.role ?: source.getClaim("rol")
        val currentOrgId = userDetails?.currentOrgId ?: source.getClaim("coid")  // ⭐ MUHIM QO‘SHILDI

        val authorities = mutableListOf<SimpleGrantedAuthority>()
        authorities.add(SimpleGrantedAuthority("ROLE_${role}"))

        val token = JwtAuthenticationToken(source, authorities, username)

        // 2) context.ga user info qo‘shib qo‘yamiz
        val contextUser = UserInfoResponse(
            id = source.getClaim("uid"),
            username = username,
            fullName = userDetails?.fullName ?: "",
            currentOrgId = currentOrgId,
            role = role
        )

        token.details = contextUser   // ⭐ MUHIM

        return token
    }
}
