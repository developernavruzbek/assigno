package org.example.auth.mappers

import org.example.auth.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.example.auth.model.requests.UserCreateRequest
import org.example.auth.model.responses.UserInfoResponse
import org.example.auth.model.responses.UserResponse
import org.example.auth.enums.Role

@Component
class UserEntityMapper(
    private val passwordEncoder: PasswordEncoder,
) {

    fun toUserInfo(user: User): UserInfoResponse {
        return UserInfoResponse(
            id = user.id!!,
            fullName = user.fullName,
            username = user.username!!,
            currentOrgId = user.currentOrgId,
            role = user.role.name,
        )
    }

    fun toResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id!!,
            username = user.username!!,
            fullName = user.fullName,
            phoneNumber = user.phoneNumber,
            age = user.age,
            role = user.role.name,
            status = user.status,

        )
    }
/*
    fun toEntity(request: UserCreateRequest, role: Role) = request.run {
        User(fullName, phoneNumber, username, age  ,passwordEncoder.encode(password),role)
    }

 */

    fun toEntity(request: UserCreateRequest): User{
        request.run {
            return User(
                username = username,
                fullName = fullName,
                phoneNumber = phoneNumber,
                age = age,
                password = passwordEncoder.encode(password),
                currentOrgId = 0,
                role = Role.USER
            )
        }
    }
}