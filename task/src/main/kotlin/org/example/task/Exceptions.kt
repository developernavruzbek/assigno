package org.example.task




import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

sealed class BaseException(
    errorCode: ErrorCode,
    override val message: String? = null
    ) : RuntimeException(message)

class OrganizationDidNotFoundException(msg: String) : BaseException(ErrorCode.ORGANIZATION_NOT_FOUND, msg)

class TaskNotFoundException(msg: String) : BaseException(ErrorCode.TASK_NOT_FOUND, msg)
class TaskAlreadyExistsException(msg: String) : BaseException(ErrorCode.TASK_ALREADY_EXISTS, msg)

class AccountAlreadyAssignedException(msg: String) : BaseException(ErrorCode.ACCOUNT_ALREADY_ASSIGNED, msg)
class AccountTaskNotFoundException(msg: String) : BaseException(ErrorCode.ACCOUNT_TASK_NOT_FOUND, msg)

class TaskStateNotFoundException(msg: String) : BaseException(ErrorCode.TASK_STATE_NOT_FOUND, msg)
class ForbiddenException(msg: String) : BaseException(ErrorCode.FORBIDDEN_EXCEPTION, msg)

class ProjectNotFoundException(msg: String) : BaseException(ErrorCode.PROJECT_NOT_FOUND, msg)
class ProjectAlreadyExistsException(msg: String) : BaseException(ErrorCode.PROJECT_ALREADY_EXISTS, msg)

class BoardNotFoundException(msg: String) : BaseException(ErrorCode.BOARD_NOT_FOUND, msg)
class BoardAlreadyExistsException(msg: String) : BaseException(ErrorCode.BOARD_ALREADY_EXISTS, msg)


@ControllerAdvice
class GlobalExceptionHandler {

//    @ExceptionHandler(NotFoundException::class)
//    fun handleNotFoundException(ex: NotFoundException, request: WebRequest): ResponseEntity<ErrorDetails> {
//        val errorDetails = ErrorDetails(ex.message ?: "Resource not found")
//        return ResponseEntity(errorDetails, HttpStatus.NOT_FOUND)
//    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ErrorDetails> {
        val errorDetails = ErrorDetails(ex.message ?: "An error occurred")
        return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}

data class ErrorDetails(val message: String)
