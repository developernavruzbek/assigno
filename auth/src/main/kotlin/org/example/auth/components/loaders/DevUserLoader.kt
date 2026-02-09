package org.example.auth.components.loaders

import org.example.auth.User
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.example.auth.enums.Role
import org.example.auth.repositories.UserRepository


@Component
class DevUserLoader(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(this::class.java)


    @Transactional
    override fun run(vararg args: String?) {
        createDevUser()
    }


    private fun createDevUser() {
        try {
            val username = "dev1"
            if (userRepository.findByUsernameAndDeletedFalse(username) != null) return
            logger.info("Creating dev user...")

            val devUser = User(
                fullName = "Developer Navruzbek",
                phoneNumber = "+998901234567",
                username = username,
                age = 21L,
                currentOrgId = 0,
                password = passwordEncoder.encode("DeV#2025"),
                role = Role.ADMIN
            )
            userRepository.save(devUser)
            logger.info("Created dev user...")
        } catch (e: Exception) {
            logger.warn("Couldn't create dev user. Stacktrace: ${e.stackTraceToString()}")
        }
    }

}