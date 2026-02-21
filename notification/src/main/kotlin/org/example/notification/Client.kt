package org.example.notification

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "organization-service", url = "http://localhost:8080", configuration = [FeignOAuth2TokenConfig::class])
interface OrganizationClient{
    @PostMapping("employee/get-emp")
    fun getEmp(@RequestBody empRequest: EmpRequest): EmpResponse

    @GetMapping("/org/{orgId}")
    fun getOne(@PathVariable orgId: Long): OrganizationResponse

}
