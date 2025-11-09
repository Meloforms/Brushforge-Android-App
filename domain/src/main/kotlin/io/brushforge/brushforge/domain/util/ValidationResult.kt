package io.brushforge.brushforge.domain.util

/**
 * Represents the result of input validation.
 */
sealed class ValidationResult {
    /**
     * Input is valid and can be used.
     */
    object Valid : ValidationResult()

    /**
     * Input is invalid with a specific error message.
     * @param errorMessage Human-readable error message to display to the user
     */
    data class Invalid(val errorMessage: String) : ValidationResult()
}

/**
 * Extension function to check if validation result is valid.
 */
fun ValidationResult.isValid(): Boolean = this is ValidationResult.Valid

/**
 * Extension function to get error message if validation failed.
 */
fun ValidationResult.getErrorOrNull(): String? = when (this) {
    is ValidationResult.Valid -> null
    is ValidationResult.Invalid -> this.errorMessage
}
