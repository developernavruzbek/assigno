package org.example.auth.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.auth.enums.Role
import org.example.auth.mappers.UserEntityMapper
import org.example.auth.model.requests.UserCreateRequest
import org.example.auth.model.responses.UserInfoResponse
import org.example.auth.repositories.UserRepository
import org.example.auth.utils.userId

/*
@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserEntityMapper
) {
    fun userMe(): UserInfoResponse {
        return userMapper
            .toUserInfo(userRepository.findByIdAndDeletedFalse(userId())!!)
    }

    @Transactional
    fun registerUser(request: UserCreateRequest) {
        if (userRepository.existsByUsername(request.username!!))
            throw IllegalArgumentException("Username ${request.username} is already registered")

        userRepository.save(userMapper.toEntity(request, Role.USER))
    }
}

 */