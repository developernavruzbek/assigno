package org.example.organization

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("org")
class OrganizationController(
    private val organizationService: OrganizationService
){
    @GetMapping
    fun getAll() = organizationService.getAll()

    @PostMapping
    fun create(@RequestBody organizationCreateRequest: OrganizationCreateRequest) = organizationService.create(organizationCreateRequest)

    @GetMapping("/{orgId}")
    fun getOne(@PathVariable orgId:Long) = organizationService.getOne(orgId)

    @PutMapping("/{orgId}")
    fun update(@PathVariable orgId:Long, @RequestBody organizationUpdateRequest: OrganizationUpdateRequest) = organizationService.update(orgId, organizationUpdateRequest)

    @DeleteMapping("/{orgId}")
    fun delete(@PathVariable orgId: Long) = organizationService.delete(orgId)
}

@RestController
@RequestMapping("employee")
class EmployeeController(
    private val employeeService: EmployeeService
){
    @PostMapping
    fun create(@RequestBody employeeCreateRequest: EmployeeCreateRequest) = employeeService.create(employeeCreateRequest)

    @GetMapping("/{employeeId}")
    fun getOne(@PathVariable employeeId:Long) =  employeeService.getOne(employeeId)

    @GetMapping
    fun getAll() = employeeService.getAll()

    @PutMapping("/{employeeId}")
    fun update(@PathVariable employeeId:Long, @RequestBody employeeUpdateRequest: EmployeeUpdateRequest) = employeeService.update(employeeId, employeeUpdateRequest)


    @DeleteMapping("/{employeeId}")
    fun delete(@PathVariable employeeId: Long) = employeeService.delete(employeeId)

    @GetMapping("/organization/{organizationId}")
    fun getEmployeesOrganization(@PathVariable organizationId: Long) =
        employeeService.getAllEmployeesByOrganization(organizationId)

    @GetMapping("/organization/{organizationId}/simple")
    fun getEmployeesOrganizationSimple(@PathVariable organizationId: Long) =
        employeeService.getAllByOrganizationId(organizationId)

    @PostMapping("/change")
    fun changeCurrentOrg(@RequestBody changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest) =
        employeeService.changeCurrentOrg(changeCurrentOrganizationRequest)
}