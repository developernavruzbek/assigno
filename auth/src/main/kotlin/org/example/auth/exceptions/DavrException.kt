package org.example.auth.exceptions

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.example.auth.models.responses.BaseMessage
import org.example.auth.enums.ErrorCode

sealed class DavrException : RuntimeException() {

    abstract fun errorCode(): ErrorCode

    open fun getErrorMessageArguments(): Array<Any?>? = null

    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource): BaseMessage {
        val errorMessage = try {
            errorMessageSource.getMessage(errorCode().name, getErrorMessageArguments(), LocaleContextHolder.getLocale())
        } catch (e: Exception) {
            e.message
        }
        return BaseMessage(errorCode().code, errorMessage)
    }
}


class UserNotFoundException : DavrException() {
    override fun errorCode() = ErrorCode.USER_NOT_FOUND
}

class UsernameAlreadyExistsException : DavrException() {
    override fun errorCode() = ErrorCode.USERNAME_ALREADY_EXISTS
}
class PhoneNumberAlreadyExistsException: DavrException() {
    override fun errorCode()  = ErrorCode.PHONE_NUMBER_ALREADY_EXISTS
}
