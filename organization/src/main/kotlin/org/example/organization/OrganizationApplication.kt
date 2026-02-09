package org.example.organization

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
@EnableJpaAuditing
@EnableFeignClients
class OrganizationApplication

fun main(args: Array<String>) {
    runApplication<OrganizationApplication>(*args)
}
