package org.example.organization

import feign.FeignException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(DemoExceptionHandler::class)
    fun handleAccountException(exception: DemoExceptionHandler): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }

    @ExceptionHandler(FeignClientException::class)
    fun handleFeignClientException(exception: FeignClientException): ResponseEntity<BaseMessage> {
        val status = if (exception.httpStatus in 400..599) exception.httpStatus else HttpStatus.BAD_REQUEST.value()
        return ResponseEntity.status(status).body(BaseMessage(exception.code, exception.message))
    }

    @ExceptionHandler(FeignException::class)
    fun handleRawFeignException(exception: FeignException): ResponseEntity<BaseMessage> {
        val status = if (exception.status() in 400..599) exception.status() else HttpStatus.BAD_REQUEST.value()
        val message = exception.message ?: "Downstream service error"
        return ResponseEntity.status(status).body(BaseMessage(null, message))
    }
}

sealed class DemoExceptionHandler() : RuntimeException() {
    abstract fun errorCode(): ErrorCodes
    open fun getArguments(): Array<Any?>? = null

    fun getErrorMessage(resourceBundleMessageSource: ResourceBundleMessageSource): BaseMessage {
        val message = try {
            resourceBundleMessageSource.getMessage(
                errorCode().name, getArguments(), LocaleContextHolder.getLocale()
            )
        } catch (e: Exception) {
            e.message
        }
        return BaseMessage(errorCode().code, message)
    }
}

class OrganizationNameAlreadyExistsException : DemoExceptionHandler() {
    override fun errorCode() = ErrorCodes.ORGANIZATION_NAME_ALREADY_EXISTS
}


class OrganizationNotFoundException : DemoExceptionHandler() {
    override fun errorCode() = ErrorCodes.ORGANIZATION_NOT_FOUND
}


class OrganizationAlreadyExistsException() : DemoExceptionHandler() {
    override fun errorCode() = ErrorCodes.ORGANIZATION_ALREADY_EXISTS

}

class OrganizationPhoneNumberAlreadyExistsException() : DemoExceptionHandler() {
    override fun errorCode() = ErrorCodes.ORGANIZATION_PHONE_NUMBER_ALREADY_EXISTS
}

class PasswordIsIncorrect : DemoExceptionHandler() {
    override fun errorCode() = ErrorCodes.PASSWORD_IS_INCORRECT
}

class FollowAlreadyExistsException() : DemoExceptionHandler() {
    override fun errorCode() = ErrorCodes.FOLLOW_ALREADY_EXISTS
}

class FollowNotFoundException() : DemoExceptionHandler() {
    override fun errorCode() = ErrorCodes.FOLLOW_NOT_FOUND
}

class EmployeeNotFoundException : DemoExceptionHandler() {
    override fun errorCode() = ErrorCodes.EMPLOYEE_NOT_FOUND
}

class EmployeeAlreadyExistsException : DemoExceptionHandler() {
    override fun errorCode() = ErrorCodes.EMPLOYEE_ALREADY_EXISTS
}

