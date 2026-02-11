package org.example.organization

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "auth-service", url = "http://localhost:8089", configuration = [FeignOAuth2TokenConfig::class])
interface UserClient {

    @GetMapping("user/{userId}")
    fun getUserById(@PathVariable userId: Long): UserResponse

    @PostMapping("/api/users/batch")
    fun getUsersByIds(@RequestBody dto: UserBatchRequest): List<UserResponse>

    @PostMapping("user/set-org")
    fun changeCurrentOrg(@RequestBody changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest)

}
