package org.example.organization


import org.springframework.stereotype.Component

@Component
class OrganizationMapper{
    fun toEntity(organizationCreateRequest: OrganizationCreateRequest): Organization{
        organizationCreateRequest.run {
            return Organization(
                name  = name ,
                tagline = tagline,
                code  = "code", // codes generate ??
                address = address,
                phoneNumber = phoneNumber,
                active = true
            )
        }
    }

    fun toDto(organization: Organization): OrganizationResponse{
        organization.run {
            return OrganizationResponse(
                id = id!!,
                name  = name,
                tagline = tagline,
                address = address,
                code = code,
                phoneNumber =  phoneNumber,
                active = active
            )
        }
    }

}


@Component
class EmployeeMapper{
    fun toEntity(req: EmployeeCreateRequest, org: Organization): Employee {
        return Employee(
            accountId = req.userId,
            organization = org,
            position = req.position,
            localNumber = 0
        )
    }

    fun toDto(employee: Employee): EmployeeResponse {
        return EmployeeResponse(
            id = employee.id!!,
            userId = employee.accountId,
            organizationId = employee.organization.id!!,
            organizationName = employee.organization.name,
            position = employee.position,
            localNumber = employee.localNumber
        )
    }

    fun toFullResponse(employee: Employee, user: UserResponse): EmployeeResponseOrganization {
        return EmployeeResponseOrganization(
            id = employee.id!!,
            userId = user.id!!,
            fullName = user.fullName,
            phoneNumber = user.phoneNumber,
            age = user.age,
            position = employee.position,
            localNumber = employee.localNumber
        )
    }
}