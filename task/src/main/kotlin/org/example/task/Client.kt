package org.example.task

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "organization-service", url = "http://localhost:8080", configuration = [FeignOAuth2TokenConfig::class])
interface OrganizationClient{
    @PostMapping("employee/get-emp")
    fun getEmp(@RequestBody empRequest: EmpRequest): EmpResponse

    @GetMapping("employee/{employeeId}")
    fun getEmployee(@PathVariable employeeId:Long): EmpResponse

}