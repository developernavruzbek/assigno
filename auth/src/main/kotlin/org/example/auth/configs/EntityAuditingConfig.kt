package org.example.auth.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.example.auth.utils.userIdNullable
import java.util.Optional

@Configuration
class EntityAuditingConfig {

    @Bean
    fun userIdAuditorAware() = AuditorAware { Optional.ofNullable(userIdNullable()) }

}