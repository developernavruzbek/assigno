package org.example.fileservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EntityScan(basePackages = ["uz.zero.fileservice"])
@EnableJpaRepositories(basePackages = ["org.example.fileservice"])
class FileServiceApplication
fun main(args: Array<String>) {
    runApplication<FileServiceApplication>(*args)
}
