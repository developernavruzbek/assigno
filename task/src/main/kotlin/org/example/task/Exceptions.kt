package org.example.task

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
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
            is OrganizationDidNotFoundException, is TaskNotFoundException,
            is TaskStateNotFoundException, is AccountTaskNotFoundException,
            is ProjectNotFoundException, is BoardNotFoundException -> HttpStatus.NOT_FOUND

            is TaskAlreadyExistsException, is AccountAlreadyAssignedException,
            is ProjectAlreadyExistsException, is BoardAlreadyExistsException,
            is TaskStateAlreadyExistsException -> HttpStatus.CONFLICT

            is ForbiddenException -> HttpStatus.FORBIDDEN
            is BadRequestException -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_REQUEST
        }
        return buildResponse(status, ex.errorCode.name, ex.message, request)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException, request: WebRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(
            HttpStatus.valueOf(ex.statusCode.value()),
            "INTERNAL_SERVICE_ERROR",
            ex.reason ?: "Ichki servisda xatolik",
            request
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", errors, request)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val message = "Parametr '${ex.name}' noto'g'ri turda. Kutilgan: ${ex.requiredType?.simpleName}"
        return buildResponse(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", message, request)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException, request: WebRequest): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.BAD_REQUEST, "JSON_ERROR", "JSON formatida xato yoki body bo'sh", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        val log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
        log.error("Kutilmagan xato: ", ex)

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.message, request)
    }

    private fun buildResponse(
        status: HttpStatus,
        code: String,
        message: String?,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            code = code,
            message = message ?: "Kutilmagan xato yuz berdi",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity(response, status)
    }
}