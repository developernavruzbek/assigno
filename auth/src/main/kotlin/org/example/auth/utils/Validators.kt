package org.example.auth.utils

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [NotSpaceValidator::class])
annotation class NotSpace(
    val message: String = "Not space on text",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)

class NotSpaceValidator : ConstraintValidator<NotSpace, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        return value.replace(" ", "") != ""
    }
}