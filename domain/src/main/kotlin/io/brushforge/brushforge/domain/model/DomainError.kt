package io.brushforge.brushforge.domain.model

/**
 * Base contract for errors emitted from use cases.
 * Adding this now keeps downstream layers strongly typed while we flesh out concrete variants.
 */
sealed interface DomainError {
    data object Unknown : DomainError
}
