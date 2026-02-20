package org.example.organization

import org.springframework.security.access.prepost.PreAuthorize
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
) {
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAll() = organizationService.getAll()

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@RequestBody organizationCreateRequest: OrganizationCreateRequest) =
        organizationService.create(organizationCreateRequest)

    @GetMapping("/{orgId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getOne(@PathVariable orgId: Long) = organizationService.getOne(orgId)

    @PutMapping("/{orgId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(@PathVariable orgId: Long, @RequestBody organizationUpdateRequest: OrganizationUpdateRequest) =
        organizationService.update(orgId, organizationUpdateRequest)

    @DeleteMapping("/{orgId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable orgId: Long) = organizationService.delete(orgId)
}

@RestController
@RequestMapping("employee")
class EmployeeController(
    private val employeeService: EmployeeService
) {
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@RequestBody employeeCreateRequest: EmployeeCreateRequest) =
        employeeService.create(employeeCreateRequest)

    @GetMapping("/{employeeId}")
    fun getOne(@PathVariable employeeId: Long) = employeeService.getOne(employeeId)

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAll() = employeeService.getAll()

    @PutMapping("/{employeeId}")
    fun update(@PathVariable employeeId: Long, @RequestBody employeeUpdateRequest: EmployeeUpdateRequest) =
        employeeService.update(employeeId, employeeUpdateRequest)


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


    @PostMapping("/get-emp")
    fun getEmp(@RequestBody empRequest: EmpRequest) = employeeService.getEmp(empRequest)

}