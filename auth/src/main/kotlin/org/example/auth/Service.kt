package org.example.auth

import org.example.auth.enums.Role
import org.example.auth.enums.UserStatus
import org.example.auth.exceptions.PhoneNumberAlreadyExistsException
import org.example.auth.exceptions.UserNotFoundException
import org.example.auth.exceptions.UsernameAlreadyExistsException
import org.example.auth.extensions.currentUserId
import org.example.auth.extensions.getUserId
import org.example.auth.mappers.UserEntityMapper
import org.example.auth.model.requests.ChangeCurrentOrganizationRequest
import org.example.auth.model.requests.UserCreateRequest
import org.example.auth.model.responses.UserResponse
import org.example.auth.models.requests.UserUpdate
import org.example.auth.repositories.UserRepository
import org.example.auth.services.security.JpaOAuth2AuthorizationService
import org.example.auth.services.security.RegisteredClientRepositoryImpl
import org.example.auth.utils.getUserJwtPrincipal
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.server.ResponseStatusException
import uz.zero.auth.model.security.CustomUserDetails

interface UserService {
    fun create(userCreateRequest: UserCreateRequest)
    fun getOne(userId:Long): UserResponse
    fun getAll(): List<UserResponse>
    fun delete(userId:Long)
    fun changeCurrentOrganization(changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest)
    fun update(userId:Long, userUpdate: UserUpdate)
}

@Service
open class UserServiceImpl(
    private val userRepository: UserRepository,
    private val userEntityMapper: UserEntityMapper,
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
        userRepository.trash(userId)
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


    @Transactional
    override fun update(userId: Long, userUpdate: UserUpdate) {
        val admin = userRepository.findByIdAndDeletedFalse(currentUserId()!!)
        println("Admin bor ")
/*
        if (userId!=currentUserId())
             throw ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update this user")

        if (admin!!.role!= Role.ADMIN)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update this user")


 */
        userRepository.findByIdAndDeletedFalse(userId)?.let { user->
                    println("User bor")
                userUpdate.fullName?.let{
                    user.fullName = it
                }
                userUpdate.phoneNumber?.let {
                    userRepository.findByPhoneNumber(it)?.let {
                        if (it.id!=user.id)
                            throw PhoneNumberAlreadyExistsException()
                    }
                    user.phoneNumber = it
                }
                userUpdate.username?.let {
                    userRepository.findByUsername(it)?.let {
                        if(it.id!=user.id)
                            throw UsernameAlreadyExistsException()
                    }
                    user.username = it
                }

                userUpdate.age?.let {
                    user.age = it
                }
                userUpdate.role?.let {
                    user.role = it
                }
                userRepository.save(user)
            }?:throw UserNotFoundException()

    }

    private fun clearOldAuthorizations(username: String) {
        val existing = authorizationService.findByClientIdAndPrincipalName("ios", username)
        if (existing != null) {
            authorizationService.remove(existing)
            println("✔ Old token removed for user: $username")
        }
    }
}