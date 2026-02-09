package org.example.organization


import org.springframework.stereotype.Component

@Component
class OrganizationMapper{
    fun toEntity(organizationCreateRequest: OrganizationCreateRequest): Organization{
        organizationCreateRequest.run {
            return Organization(
                name  = name ,
                tagline = tagline,
                code  = "code",
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
    fun toEntity(employeeCreateRequest: EmployeeCreateRequest, organization: Organization): Employee{
        employeeCreateRequest.run {
            return Employee(
                accountId = userId,
                organization = organization,
                position = position
            )
        }
    }

    fun toDto(employee: Employee): EmployeeResponse{
        employee.run {
            return EmployeeResponse(
                id  = id!!,
                userId = accountId,
                organizationId = organization.id!!,
                organizationName = organization.name,
                position = position
            )
        }
    }

    fun toFullResponse(employee: Employee, userResponse: UserResponse): EmployeeResponseOrganization{
        employee.run {
            return EmployeeResponseOrganization(
                id = id!!,
                userId = userResponse.id!!,
                fullName = userResponse.fullName,
                phoneNumber = userResponse.phoneNumber,
                age  = userResponse.age,
                position = position
            )
        }
    }

}