package org.example.organization


import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

interface OrganizationService{
    fun create(organizationCreateRequest: OrganizationCreateRequest)
    fun getOne(id:Long): OrganizationResponse
    fun update(id:Long, organizationUpdateRequest: OrganizationUpdateRequest)
    fun getAll(): List<OrganizationResponse>
    fun delete(id:Long)
}

@Service
open class OrganizationServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val organizationMapper: OrganizationMapper

): OrganizationService {

    @Transactional
    override fun create(organizationCreateRequest: OrganizationCreateRequest) {
        organizationCreateRequest.run {

            organizationRepository.findByNameAndActive(name ,true)?.let {
                throw OrganizationNameAlreadyExistsException()
            }
            organizationRepository.findByPhoneNumberAndActive(phoneNumber, true)?.let {
                throw OrganizationPhoneNumberAlreadyExistsException()
            }
        }
        organizationRepository.save(organizationMapper.toEntity(organizationCreateRequest))
    }

    override fun getOne(id: Long): OrganizationResponse {
        organizationRepository.findByIdAndActive(id, true)?.let {
            return organizationMapper.toDto(it)
        }?: throw OrganizationNotFoundException()

    }


    @Transactional
    override fun update(
        id: Long,
        organizationUpdateRequest: OrganizationUpdateRequest
    ) {
        organizationRepository.findByIdAndActive(id, true)?.let { organization ->
            organizationUpdateRequest.run {
                if(!name.isNullOrBlank())
                    organization.name = name

                if(!tagline.isNullOrBlank())
                    organization.tagline = tagline

                if(!address.isNullOrBlank())
                    organization.address = address

                if(!phoneNumber.isNullOrBlank())
                    organization.phoneNumber = phoneNumber
            }
            organizationRepository.save(organization)
        }?:throw OrganizationNotFoundException()
    }

    override fun getAll(): List<OrganizationResponse> {
        var findAll = organizationRepository.findAll()
        return findAll.map { organization ->
            organizationMapper.toDto(organization)
        }
    }

    override fun delete(id: Long) {
        var organization: Optional<Organization?> = organizationRepository.findById(id)
        if (organization==null)
            throw OrganizationNotFoundException()
        organizationRepository.trash(id)
    }
}


interface EmployeeService{
    fun create(employeeCreateRequest: EmployeeCreateRequest)
    fun getOne(id:Long) : EmployeeResponse
    fun update(id: Long, employeeUpdateRequest: EmployeeUpdateRequest)
    fun getAll(): List<EmployeeResponse>
  //  fun getAllOrganization(orgId:Long) : List<EmployeeResponseOrganization>
    fun delete(id:Long)
    fun changeCurrentOrg(changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest)
}


@Service
class EmployeeServiceImpl(
    private val employeeRepository: EmployeeRepository,
    private val userClient: UserClient,
    private val organizationRepository: OrganizationRepository,
    private val employeeMapper: EmployeeMapper
): EmployeeService{

    @Transactional
    override fun create(employeeCreateRequest: EmployeeCreateRequest) {
        val user = userClient.getUserById(employeeCreateRequest.userId)
        var organization = organizationRepository.findByIdAndActive(employeeCreateRequest.organizationId, true)
            ?:throw OrganizationNotFoundException()
        employeeRepository.save(employeeMapper.toEntity(employeeCreateRequest,organization ))
        userClient.changeCurrentOrg(ChangeCurrentOrganizationRequest(user.id, organization.id!!))

    }


    override fun getOne(id: Long): EmployeeResponse {
        var employee = employeeRepository.findById(id)
        if (employee==null)
            throw EmployeeNotFoundException()
        return employeeMapper.toDto(employee as Employee)
    }


    @Transactional
    override fun update(id: Long, employeeUpdateRequest: EmployeeUpdateRequest) {
        var employee : Optional<Employee?> = employeeRepository.findById(id)
        if (employee==null)
            throw EmployeeNotFoundException()
        employeeUpdateRequest.run {
            userId?.let {
                if (userId!=0L)
                    (employee as Employee).accountId = userId
            }
            organizationId?.let {
                if (organizationId!=0L){
                    organizationRepository.findByIdAndActive(organizationId, true)?.let {
                        (employee as Employee).organization = it
                    }?: throw OrganizationNotFoundException()
                }
            }
            if (!position.isNullOrBlank()) {
                (employee as Employee).position = position
            }
        }
        employeeRepository.save(employee as Employee)
    }

    override fun getAll(): List<EmployeeResponse> {
        val allEmployees = employeeRepository.findAll()
        return allEmployees.map { employee ->
            employeeMapper.toDto(employee)
        }
    }

/*
    override fun getAllOrganization(orgId: Long): List<EmployeeResponseOrganization> {
        val org = organizationRepository.findByIdAndActive(orgId, true)
            ?:throw OrganizationNotFoundException()

        val users = employeeRepository.findAllByOrganization(org).map {
            it.accountId
        }
        val userEmployees = userClient.getUsersByIds(UserBatchRequest(users))

        userEmployees.map { userResponse->


        }


    }

 */

    override fun delete(id: Long) {
        var employee = employeeRepository.findById(id)
        if (employee==null)
            throw EmployeeNotFoundException()
        employeeRepository.trash(id)
    }



    override fun changeCurrentOrg(changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest) {
        val user = userClient.getUserById(changeCurrentOrganizationRequest.userId)

        val organization =
            organizationRepository.findByIdAndActive(changeCurrentOrganizationRequest.newOrgId, true)

        if (organization==null)
            throw OrganizationNotFoundException()

        if (employeeRepository.existsByAccountIdAndOrganization(
                user.id,
                organization
            )
        ) {
            userClient.changeCurrentOrg(changeCurrentOrganizationRequest)

        }
    }
}