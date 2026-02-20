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

}


@FeignClient(name = "task-service", url = "http://localhost:8082", configuration = [FeignOAuth2TokenConfig::class])
interface TaskClient{
    @GetMapping("account-tasks/{taskId}")
    fun getEmployee(@PathVariable taskId:Long): List<AccountTaskResponse>

}