package io.brushforge.brushforge.domain.util

/**
 * A type-safe result wrapper for operations that can fail.
 *
 * This sealed class represents the outcome of an operation that can either succeed
 * with a value or fail with an error. It provides a clean way to handle errors
 * without throwing exceptions, making error handling explicit and type-safe.
 *
 * @param T The type of the success value
 * @param E The type of the error (must be a subtype of DomainError)
 */
sealed class Result<out T, out E : DomainError> {
    /**
     * Represents a successful operation with a value.
     */
    data class Success<out T>(val value: T) : Result<T, Nothing>()

    /**
     * Represents a failed operation with an error.
     */
    data class Failure<out E : DomainError>(val error: E) : Result<Nothing, E>()

    /**
     * Returns true if the result is a success.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if the result is a failure.
     */
    val isFailure: Boolean
        get() = this is Failure

    /**
     * Returns the encapsulated value if this is a success, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    /**
     * Returns the encapsulated error if this is a failure, or null otherwise.
     */
    fun errorOrNull(): E? = when (this) {
        is Success -> null
        is Failure -> error
    }

    /**
     * Executes the given block if this is a success.
     */
    inline fun onSuccess(block: (T) -> Unit): Result<T, E> {
        if (this is Success) block(value)
        return this
    }

    /**
     * Executes the given block if this is a failure.
     */
    inline fun onFailure(block: (E) -> Unit): Result<T, E> {
        if (this is Failure) block(error)
        return this
    }

    /**
     * Maps the success value using the given transform function.
     */
    inline fun <R> map(transform: (T) -> R): Result<R, E> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(error)
    }

    /**
     * Maps the error using the given transform function.
     */
    inline fun <F : DomainError> mapError(transform: (E) -> F): Result<T, F> = when (this) {
        is Success -> Success(value)
        is Failure -> Failure(transform(error))
    }

    /**
     * Returns the success value or the result of calling the default function with the error.
     */
    inline fun getOrElse(default: (E) -> @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> default(error)
    }

    companion object {
        /**
         * Wraps a function call in a try-catch and returns a Result.
         * Catches all exceptions and maps them to the provided error mapper.
         */
        inline fun <T, E : DomainError> catch(
            errorMapper: (Throwable) -> E,
            block: () -> T
        ): Result<T, E> = try {
            Success(block())
        } catch (e: Throwable) {
            Failure(errorMapper(e))
        }
    }
}

/**
 * Base class for all domain errors.
 *
 * All domain-level errors should extend this class to provide
 * user-friendly error messages and error codes for logging.
 */
sealed class DomainError {
    /**
     * User-friendly error message that can be displayed directly to users.
     */
    abstract val userMessage: String

    /**
     * Technical error message for logging and debugging.
     */
    abstract val technicalMessage: String

    /**
     * Optional error code for tracking and analytics.
     */
    open val errorCode: String? = null
}

/**
 * Database-related errors.
 */
sealed class DatabaseError : DomainError() {
    data class WriteError(
        override val technicalMessage: String
    ) : DatabaseError() {
        override val userMessage: String = "Unable to save your changes. Please try again."
        override val errorCode: String = "DB_WRITE_ERROR"
    }

    data class ReadError(
        override val technicalMessage: String
    ) : DatabaseError() {
        override val userMessage: String = "Unable to load data. Please restart the app."
        override val errorCode: String = "DB_READ_ERROR"
    }

    data class ConstraintViolation(
        override val technicalMessage: String
    ) : DatabaseError() {
        override val userMessage: String = "This item already exists."
        override val errorCode: String = "DB_CONSTRAINT_VIOLATION"
    }

    data class DiskError(
        override val technicalMessage: String
    ) : DatabaseError() {
        override val userMessage: String = "Not enough storage space. Please free up some space and try again."
        override val errorCode: String = "DB_DISK_ERROR"
    }

    data class CorruptionError(
        override val technicalMessage: String
    ) : DatabaseError() {
        override val userMessage: String = "Database is corrupted. Please reinstall the app."
        override val errorCode: String = "DB_CORRUPTION_ERROR"
    }
}

/**
 * File I/O related errors.
 */
sealed class FileError : DomainError() {
    data class NotFound(
        val fileName: String,
        override val technicalMessage: String
    ) : FileError() {
        override val userMessage: String = "Required file '$fileName' not found. Please reinstall the app."
        override val errorCode: String = "FILE_NOT_FOUND"
    }

    data class ReadError(
        val fileName: String,
        override val technicalMessage: String
    ) : FileError() {
        override val userMessage: String = "Unable to read file '$fileName'."
        override val errorCode: String = "FILE_READ_ERROR"
    }

    data class ParseError(
        val fileName: String,
        override val technicalMessage: String
    ) : FileError() {
        override val userMessage: String = "File '$fileName' is corrupted. Please reinstall the app."
        override val errorCode: String = "FILE_PARSE_ERROR"
    }
}

/**
 * Validation errors.
 */
sealed class ValidationError : DomainError() {
    data class InvalidInput(
        override val userMessage: String,
        override val technicalMessage: String = userMessage
    ) : ValidationError() {
        override val errorCode: String = "VALIDATION_ERROR"
    }
}

/**
 * Business logic errors.
 */
sealed class BusinessError : DomainError() {
    data class InventoryLimitReached(
        val current: Int,
        val limit: Int
    ) : BusinessError() {
        override val userMessage: String =
            "You've reached your inventory limit ($current/$limit paints). Please remove some paints or increase your limit in settings."
        override val technicalMessage: String = "Inventory limit reached: $current/$limit"
        override val errorCode: String = "INVENTORY_LIMIT_REACHED"
    }

    data class NotFound(
        val itemType: String,
        val identifier: String
    ) : BusinessError() {
        override val userMessage: String = "$itemType not found."
        override val technicalMessage: String = "$itemType with ID '$identifier' not found"
        override val errorCode: String = "NOT_FOUND"
    }

    data class AlreadyExists(
        val itemType: String,
        val identifier: String
    ) : BusinessError() {
        override val userMessage: String = "This $itemType already exists."
        override val technicalMessage: String = "$itemType with ID '$identifier' already exists"
        override val errorCode: String = "ALREADY_EXISTS"
    }
}

/**
 * Unknown/unexpected errors.
 */
data class UnknownError(
    override val technicalMessage: String
) : DomainError() {
    override val userMessage: String = "Something went wrong. Please try again."
    override val errorCode: String = "UNKNOWN_ERROR"
}
