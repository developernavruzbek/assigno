package org.example.task

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@FeignClient(
    name = "organization-service",
    url = "http://localhost:8080",
    configuration = [FeignOAuth2TokenConfig::class]
)
interface OrganizationClient {
    @PostMapping("employee/get-emp")
    fun getEmp(@RequestBody empRequest: EmpRequest): EmpResponse

    @GetMapping("employee/{employeeId}")
    fun getEmployee(@PathVariable employeeId: Long): EmpResponse

    @PostMapping("employee/get-employee")
    fun getEmp2(@RequestBody empRequest: EmpRequest): EmpResponse

    @GetMapping("/org/{orgId}")
    fun getOne(@PathVariable("orgId") orgId: Long): OrganizationResponse
}

@FeignClient(
    name = "auth-service",
    url = "http://localhost:8089",
    configuration = [FeignOAuth2TokenConfig::class]
)
interface UserClient {

    @GetMapping("/user/{userId}")
    fun getUserById(@PathVariable("userId") userId: Long): UserResponse
}

@FeignClient(
    name = "notification-service",
    url = "http://localhost:8084",
    configuration = [FeignOAuth2TokenConfig::class]
)
interface NotificationClient {
    @PostMapping("/notification/send")
    fun sendNotification(@RequestBody request: ActionRequest)
}

@FeignClient(
    name = "file-service",
    url = "http://localhost:8085",
    configuration = [FeignOAuth2TokenConfig::class]
)
interface FileClient {

    @PostMapping("/upload/{taskId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart("file") file: MultipartFile,
        @PathVariable("taskId") taskId: Long
    ): FileResponse

    @DeleteMapping("/{id}")
    fun deleteFile(@PathVariable("id") id: Long)

    @DeleteMapping
    fun deleteFileByKeyName(@RequestParam("keyName") keyName: String)

    @DeleteMapping("/task/{taskId}")
    fun deleteFilesByTask(@PathVariable("taskId") taskId: Long)

    @GetMapping("/task/{taskId}")
    fun getFilesByTask(@PathVariable("taskId") taskId: Long): List<FileResponse>
}

data class FileResponse(val id: Long, val keyName: String, val taskId: Long)