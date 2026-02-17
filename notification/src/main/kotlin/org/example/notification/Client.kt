package org.example.notification

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "employee-service", url = "\${services.employee.url}")
interface EmployeeClient {

    @PutMapping("/employees/{id}/telegram")
    fun updateTelegram(
        @PathVariable("id") id: Long,
        @RequestBody request: TelegramUpdateRequest
    )
}
