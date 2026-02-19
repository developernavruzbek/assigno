package org.example.task

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

sealed class BaseException(
    val errorCode: ErrorCode,
    override val message: String? = null
) : RuntimeException(message)

class OrganizationDidNotFoundException(msg: String) : BaseException(ErrorCode.ORGANIZATION_NOT_FOUND, msg)

class TaskNotFoundException(msg: String) : BaseException(ErrorCode.TASK_NOT_FOUND, msg)
class TaskAlreadyExistsException(msg: String) : BaseException(ErrorCode.TASK_ALREADY_EXISTS, msg)

class AccountAlreadyAssignedException(msg: String) : BaseException(ErrorCode.ACCOUNT_ALREADY_ASSIGNED, msg)
class AccountTaskNotFoundException(msg: String) : BaseException(ErrorCode.ACCOUNT_TASK_NOT_FOUND, msg)

class TaskStateNotFoundException(msg: String) : BaseException(ErrorCode.TASK_STATE_NOT_FOUND, msg)
class TaskStateAlreadyExistsException(msg: String) : BaseException(ErrorCode.TASK_STATE_ALREADY_EXISTS, msg)

class ForbiddenException(msg: String) : BaseException(ErrorCode.FORBIDDEN_EXCEPTION, msg)
class BadRequestException(msg: String) : BaseException(ErrorCode.BAD_REQUEST, msg)

class ProjectNotFoundException(msg: String) : BaseException(ErrorCode.PROJECT_NOT_FOUND, msg)
class ProjectAlreadyExistsException(msg: String) : BaseException(ErrorCode.PROJECT_ALREADY_EXISTS, msg)

class BoardNotFoundException(msg: String) : BaseException(ErrorCode.BOARD_NOT_FOUND, msg)
class BoardAlreadyExistsException(msg: String) : BaseException(ErrorCode.BOARD_ALREADY_EXISTS, msg)


data class ErrorResponse(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val path: String
)

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BaseException::class)
    fun handleBaseException(ex: BaseException, request: WebRequest): ResponseEntity<ErrorResponse> {

        val status = when (ex) {
            is OrganizationDidNotFoundException -> HttpStatus.NOT_FOUND
            is TaskNotFoundException -> HttpStatus.NOT_FOUND
            is TaskStateNotFoundException -> HttpStatus.NOT_FOUND
            is AccountTaskNotFoundException -> HttpStatus.NOT_FOUND
            is ProjectNotFoundException -> HttpStatus.NOT_FOUND
            is BoardNotFoundException -> HttpStatus.NOT_FOUND

            is TaskAlreadyExistsException -> HttpStatus.CONFLICT
            is AccountAlreadyAssignedException -> HttpStatus.CONFLICT
            is ProjectAlreadyExistsException -> HttpStatus.CONFLICT
            is BoardAlreadyExistsException -> HttpStatus.CONFLICT
            is TaskStateAlreadyExistsException -> HttpStatus.CONFLICT

            is BadRequestException -> HttpStatus.BAD_REQUEST

            is ForbiddenException -> HttpStatus.FORBIDDEN

            else -> HttpStatus.BAD_REQUEST
        }

        val response = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            code = ex.errorCode.name,
            message = ex.message ?: "Unexpected error",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity(response, status)
    }


    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        val response = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            code = "INTERNAL_ERROR",
            message = ex.message ?: "An unexpected error occurred",
            path = request.getDescription(false).removePrefix("uri=")
        )

        return ResponseEntity(response, status)
    }
}
