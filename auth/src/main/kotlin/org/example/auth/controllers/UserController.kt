package org.example.auth.controllers

import org.example.auth.UserService
import org.example.auth.model.requests.ChangeCurrentOrganizationRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.example.auth.model.requests.UserCreateRequest
import org.springframework.web.bind.annotation.PathVariable

@RestController
@RequestMapping("user")
class UserController(
    private val userService: UserService
){
    @PostMapping("/register")
    fun registerUser(@RequestBody request: UserCreateRequest) = userService.create(request)


    @GetMapping
    fun getAll() = userService.getAll()

    @PostMapping("/set-org")
    fun changeCurrentOrg(@RequestBody changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest) = userService.changeCurrentOrganization(changeCurrentOrganizationRequest)

    @GetMapping("/{userId}")
    fun getOne(@PathVariable userId:Long) = userService.getOne(userId)
}

