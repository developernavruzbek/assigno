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
        validateUniqueFields(organizationCreateRequest)
        val entity = organizationMapper.toEntity(organizationCreateRequest)
        organizationRepository.save(entity)
    }

    override fun getOne(id: Long): OrganizationResponse {
        val organization = getActiveOrganization(id)
        return organizationMapper.toDto(organization)
    }

    @Transactional
    override fun update(id: Long, organizationUpdateRequest: OrganizationUpdateRequest) {
        val org = getActiveOrganization(id)

        organizationUpdateRequest.name?.let { org.name = it }
        organizationUpdateRequest.tagline?.let { org.tagline = it }
        organizationUpdateRequest.address?.let { org.address = it }
        organizationUpdateRequest.phoneNumber?.let { org.phoneNumber = it }

        organizationRepository.save(org)
    }

    override fun getAll(): List<OrganizationResponse> =
        organizationRepository.findAllNotDeleted()
            .map { organizationMapper.toDto(it) }

    override fun delete(id: Long) {
        getNotDeletedOrganization(id)
        organizationRepository.trash(id)
    }

    /** private helper **/

    private fun validateUniqueFields(req: OrganizationCreateRequest) {
        organizationRepository.findByNameAndActive(req.name, true)?.let {
            throw OrganizationNameAlreadyExistsException()
        }
        organizationRepository.findByPhoneNumberAndActive(req.phoneNumber, true)?.let {
            throw OrganizationPhoneNumberAlreadyExistsException()
        }
    }

    private fun getActiveOrganization(id: Long) =
        organizationRepository.findByIdAndActive(id, true)
            ?: throw OrganizationNotFoundException()

    private fun getNotDeletedOrganization(id: Long) =
        organizationRepository.findByIdAndDeletedFalse(id)
            ?: throw OrganizationNotFoundException()
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
    fun getEmp(empRequest: EmpRequest): EmpResponse }

@Service
class EmployeeServiceImpl(
    private val employeeRepository: EmployeeRepository,
    private val userClient: UserClient,
    private val organizationRepository: OrganizationRepository,
    private val employeeMapper: EmployeeMapper
) : EmployeeService {

    @Transactional
    override fun create(employeeCreateRequest: EmployeeCreateRequest) {
        validateUserExists(employeeCreateRequest.userId)
        println("User bor===================")
        val activeOrganization = getActiveOrganization(employeeCreateRequest.organizationId)
        if (employeeRepository.existsByAccountIdAndOrganization(employeeCreateRequest.userId, activeOrganization)) {
            throw EmployeeAlreadyExistsException()
        }
        employeeRepository.save(employeeMapper.toEntity(employeeCreateRequest, activeOrganization))
        userClient.changeCurrentOrg(ChangeCurrentOrganizationRequest(employeeCreateRequest.userId, activeOrganization.id!!))

    }

    override fun getOne(id: Long): EmployeeResponse {
        val employee = getEmployee(id)
        return employeeMapper.toDto(employee)
    }

    @Transactional
    override fun update(id: Long, employeeUpdateRequest: EmployeeUpdateRequest) {
        val employee = getEmployee(id)

        employeeUpdateRequest.userId?.takeIf { it != 0L }?.let {
            validateUserExists(it)
            employee.accountId = it
        }

        employeeUpdateRequest.organizationId?.takeIf { it != 0L }?.let {
            employee.organization = getActiveOrganization(it)
        }

        employeeUpdateRequest.position?.let { employee.position = it }

        employeeRepository.save(employee)
    }

    override fun getAll(): List<EmployeeResponse> =
        employeeRepository.findAllNotDeleted()
            .map { employeeMapper.toDto(it) }

    override fun getAllEmployeesByOrganization(orgId: Long): List<EmployeeResponseOrganization> {


        val employees = employeeRepository.findAllByOrganization( getActiveOrganization(orgId))
        if (employees.isEmpty()) return emptyList()

        val userIds = employees.map { it.accountId }
        val userMap = userClient.getUsersByIds(UserBatchRequest(userIds)).associateBy { it.id }

        return employees.mapNotNull { emp ->
            userMap[emp.accountId]?.let { user -> employeeMapper.toFullResponse(emp, user) }
        }
    }

    override fun getAllByOrganizationId(organizationId: Long): List<EmployeeResponse> {
        getActiveOrganization(organizationId)
        return employeeRepository.findAllByOrganization(   getActiveOrganization(organizationId)
        ).map { employeeMapper.toDto(it) }
    }

    override fun delete(id: Long) {
        getEmployee(id)
        employeeRepository.trash(id)
    }

    override fun changeCurrentOrg(changeCurrentOrganizationRequest: ChangeCurrentOrganizationRequest) {
        val user = validateUserExists(changeCurrentOrganizationRequest.userId)
        val organization = getActiveOrganization(changeCurrentOrganizationRequest.newOrgId)

        if (employeeRepository.existsByAccountIdAndOrganization(user.id, organization)) {
            userClient.changeCurrentOrg(changeCurrentOrganizationRequest)
        }
    }

    override fun getEmp(empRequest: EmpRequest): EmpResponse {
        val org = organizationRepository.findByIdAndActive(empRequest.orgId, true)
            ?:throw OrganizationNotFoundException()

        val emp = employeeRepository.findByAccountIdAndOrganization(empRequest.userId, org)
                              ?:throw EmployeeNotFoundException()

        return EmpResponse(
            id  = emp.id!!,
            userId = emp.accountId,
            orgId = emp.organization.id!!,
            position = emp.position
        )
    }


    /** private helper **/

    private fun validateUserExists(userId: Long) =
        userClient.getUserById(userId)

    private fun getEmployee(id: Long) =
        employeeRepository.findByIdAndDeletedFalse(id)
            ?: throw EmployeeNotFoundException()

    private fun getActiveOrganization(id: Long) =
        organizationRepository.findByIdAndActive(id, true)
            ?: throw OrganizationNotFoundException()
}
