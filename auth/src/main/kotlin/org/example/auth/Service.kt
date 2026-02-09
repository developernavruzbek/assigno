package org.example.auth

import org.example.auth.enums.UserStatus
import org.example.auth.exceptions.PhoneNumberAlreadyExistsException
import org.example.auth.exceptions.UserNotFoundException
import org.example.auth.exceptions.UsernameAlreadyExistsException
import org.example.auth.mappers.UserEntityMapper
import org.example.auth.model.requests.ChangeCurrentOrganizationRequest
import org.example.auth.model.requests.UserCreateRequest
import org.example.auth.model.responses.UserResponse
import org.example.auth.repositories.UserRepository
import org.example.auth.services.security.JpaOAuth2AuthorizationService
import org.example.auth.services.security.RegisteredClientRepositoryImpl
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uz.zero.auth.model.security.CustomUserDetails

interface UserService {
    fun create(userCreateRequest: UserCreateRequest)
    fun login()
    fun getOne(userId:Long): UserResponse
    fun getAll(): List<UserResponse>
    fun delete(userId:Long)
    fun changeCurrentOrganization(changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest)
}

@Service
open class UserServiceImpl(
    private val userRepository: UserRepository,
    private val userEntityMapper: UserEntityMapper,
    private val tokenGenerator: DelegatingOAuth2TokenGenerator,
    private val registeredClientRepositoryImpl: RegisteredClientRepositoryImpl,
    private val authorizationService: JpaOAuth2AuthorizationService,
): UserService{

    @Transactional
    override fun create(userCreateRequest: UserCreateRequest) {
         if (userRepository.existsByUsername(userCreateRequest.username))
             throw UsernameAlreadyExistsException()
        if (userRepository.existsByPhoneNumber(userCreateRequest.phoneNumber))
            throw PhoneNumberAlreadyExistsException()

        userRepository.save(userEntityMapper.toEntity(userCreateRequest))

    }

    override fun login() {

    }

    override fun getOne(userId: Long): UserResponse {
        val user = userRepository.findByIdAndDeletedFalse(userId)
        if (user==null)
            throw UserNotFoundException()
        return userEntityMapper.toResponse(user)
    }

    override fun getAll(): List<UserResponse> {
        val users = userRepository.findAll()
        return users.map { user->
            userEntityMapper.toResponse(user)
        }
    }

    override fun delete(userId: Long) {
        TODO("Not yet implemented")
    }



    @Transactional
    override fun changeCurrentOrganization(req: ChangeCurrentOrganizationRequest) {
        val user = userRepository.findByIdAndDeletedFalse(req.userId)
            ?: throw IllegalArgumentException("User not found: ${req.userId}")

        user.currentOrgId = req.newOrgId
        userRepository.save(user)


        val userDetails = CustomUserDetails(
            id = user.id!!,
            username = user.username,
            password = user.password,
            role = user.role.name,
            enabled = user.status == UserStatus.ACTIVE && !user.deleted,
            currentOrgId = req.newOrgId
        )

        val principal = UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities
        )
        SecurityContextHolder.getContext().authentication = principal

        clearOldAuthorizations(user.username)
        println("✔ Organization changed. Old tokens removed. User must re-login.")
    }

    private fun clearOldAuthorizations(username: String) {
        val existing = authorizationService.findByClientIdAndPrincipalName("ios", username)
        if (existing != null) {
            authorizationService.remove(existing)
            println("✔ Old token removed for user: $username")
        }
    }

}