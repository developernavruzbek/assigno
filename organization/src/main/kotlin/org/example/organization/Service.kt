package org.example.organization


import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface OrganizationService{
    fun create(organizationCreateRequest: OrganizationCreateRequest)
    fun getOne(id:Long): OrganizationResponse
    fun update(id:Long, organizationUpdateRequest: OrganizationUpdateRequest)
    fun getAll(): List<OrganizationResponse>
    fun delete(id: Long)
}

@Service
class OrganizationServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val organizationMapper: OrganizationMapper

) : OrganizationService {

    @Transactional
    override fun create(organizationCreateRequest: OrganizationCreateRequest) {
        organizationCreateRequest.run {

            organizationRepository.findByNameAndActive(name, true)?.let {
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
        }
        throw OrganizationNotFoundException()

    }


    @Transactional
    override fun update(
        id: Long,
        organizationUpdateRequest: OrganizationUpdateRequest
    ) {
        val organization = organizationRepository.findByIdAndActive(id, true)
            ?: throw OrganizationNotFoundException()

        organizationUpdateRequest.run {
            if (!name.isNullOrBlank())
                organization.name = name

            if (!tagline.isNullOrBlank())
                organization.tagline = tagline

            if (!address.isNullOrBlank())
                organization.address = address

            if (!phoneNumber.isNullOrBlank())
                organization.phoneNumber = phoneNumber
        }
        organizationRepository.save(organization)
    }

    override fun getAll(): List<OrganizationResponse> {
        val findAll = organizationRepository.findAllNotDeleted()
        return findAll.map { organization ->
            organizationMapper.toDto(organization)
        }
    }

    override fun delete(id: Long) {
        organizationRepository.findByIdAndDeletedFalse(id)
            ?: throw OrganizationNotFoundException()
        organizationRepository.trash(id)
    }
}


interface EmployeeService{
    fun create(employeeCreateRequest: EmployeeCreateRequest)
    fun getOne(id:Long) : EmployeeResponse
    fun update(id: Long, employeeUpdateRequest: EmployeeUpdateRequest)
    fun getAll(): List<EmployeeResponse>
  //  fun getAllOrganization(orgId:Long) : List<EmployeeResponseOrganization>
    fun getAllEmployeesByOrganization(orgId: Long): List<EmployeeResponseOrganization>
    fun getAllByOrganizationId(organizationId: Long): List<EmployeeResponse>
    fun delete(id: Long)
    fun changeCurrentOrg(changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest)
}


@Service
class EmployeeServiceImpl(
    private val employeeRepository: EmployeeRepository,
    private val userClient: UserClient,
    private val organizationRepository: OrganizationRepository,
    private val employeeMapper: EmployeeMapper
) : EmployeeService {

    @Transactional
    override fun create(employeeCreateRequest: EmployeeCreateRequest) {
        userClient.getUserById(employeeCreateRequest.userId)
        val organization = organizationRepository.findByIdAndActive(employeeCreateRequest.organizationId, true)
            ?: throw OrganizationNotFoundException()
        employeeRepository.save(employeeMapper.toEntity(employeeCreateRequest, organization))
        userClient.changeCurrentOrg(ChangeCurrentOrganizationRequest(employeeCreateRequest.userId, organization.id!!))
    }


    override fun getOne(id: Long): EmployeeResponse {
        val employee = employeeRepository.findByIdAndDeletedFalse(id)
            ?: throw EmployeeNotFoundException()
        return employeeMapper.toDto(employee)
    }


    @Transactional
    override fun update(id: Long, employeeUpdateRequest: EmployeeUpdateRequest) {
        val employee = employeeRepository.findByIdAndDeletedFalse(id)
            ?: throw EmployeeNotFoundException()

        employeeUpdateRequest.run {
            userId?.let {
                if (it != 0L) {
                    userClient.getUserById(it)
                    employee.accountId = it
                }
            }
            organizationId?.let {
                if (it != 0L) {
                    val organization = organizationRepository.findByIdAndActive(it, true)
                        ?: throw OrganizationNotFoundException()
                    employee.organization = organization
                }
            }
            if (position != null) {
                    employee.position = position
            }
        }
        employeeRepository.save(employee)
    }

    override fun getAll(): List<EmployeeResponse> {
        val allEmployees = employeeRepository.findAllNotDeleted()
        return allEmployees.map { employee ->
            employeeMapper.toDto(employee)
        }
    }

    override fun getAllEmployeesByOrganization(orgId: Long): List<EmployeeResponseOrganization> {
        organizationRepository.findByIdAndActive(orgId, true)
            ?: throw OrganizationNotFoundException()

        val employees = employeeRepository.findAllByOrganization(orgId)
        if (employees.isEmpty()) return emptyList()

        val userIds = employees.map { it.accountId }
        val userResponses = userClient.getUsersByIds(UserBatchRequest(userIds))
        val userMap = userResponses.associateBy { it.id }

        return employees.mapNotNull { employee ->
            userMap[employee.accountId]?.let { userResponse ->
                employeeMapper.toFullResponse(employee, userResponse)
            }
        }
    }

    override fun getAllByOrganizationId(organizationId: Long): List<EmployeeResponse> {
        organizationRepository.findByIdAndActive(organizationId, true)
            ?: throw OrganizationNotFoundException()
        return employeeRepository.findAllByOrganization(organizationId).map {
            employeeMapper.toDto(it)
        }
    }

    override fun delete(id: Long) {
        employeeRepository.findByIdAndDeletedFalse(id)
            ?: throw EmployeeNotFoundException()
        employeeRepository.trash(id)
    }



    override fun changeCurrentOrg(changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest) {
        val user = userClient.getUserById(changeCurrentOrganizationRequest.userId)

        val organization =
            organizationRepository.findByIdAndActive(changeCurrentOrganizationRequest.newOrgId, true)
                ?: throw OrganizationNotFoundException()

        if (employeeRepository.existsByAccountIdAndOrganization(
                user.id,
                organization
            )
        ) {
            userClient.changeCurrentOrg(changeCurrentOrganizationRequest)
        }
    }
}