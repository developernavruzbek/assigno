package org.example.auth.services.security

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.example.auth.enums.UserStatus

import org.example.auth.repositories.UserRepository
import uz.zero.auth.model.security.CustomUserDetails


@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsernameAndDeletedFalse(username)
            ?: throw UsernameNotFoundException(username)
        return user.run {
            CustomUserDetails(
                id = id!!,
                username = username,
                password = password,
                role = role.name,
                enabled = status == UserStatus.ACTIVE && !user.deleted,
                currentOrgId = currentOrgId
            )
        }

    }
}