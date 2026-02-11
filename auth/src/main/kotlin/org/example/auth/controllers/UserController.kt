package org.example.auth.controllers

import org.example.auth.UserService
import org.example.auth.model.requests.ChangeCurrentOrganizationRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.example.auth.model.requests.UserCreateRequest
import org.example.auth.models.requests.UserUpdate
import org.example.auth.models.requests.UserUpdateRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping

@RestController
@RequestMapping("user")
class UserController(
    private val userService: UserService
){
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    fun registerUser(@RequestBody request: UserCreateRequest) = userService.create(request)


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAll() = userService.getAll()

    @PostMapping("/set-org")
    fun changeCurrentOrg(@RequestBody changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest) = userService.changeCurrentOrganization(changeCurrentOrganizationRequest)

    @GetMapping("/{userId}")
    fun getOne(@PathVariable userId:Long) = userService.getOne(userId)

    @PutMapping("/{userId}")
    fun update(@PathVariable userId:Long, @RequestBody userUpdate: UserUpdate)  = userService.update(userId, userUpdate)

    @DeleteMapping("/{userId}")
    fun delete(@PathVariable userId:Long) = userService.delete(userId)
}

