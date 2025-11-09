package io.brushforge.brushforge.domain.util

/**
 * Input validation utility for user-facing text fields.
 *
 * Provides validation rules and sanitization for:
 * - Paint names, codes, and notes
 * - Recipe names, steps, and tags
 * - Search queries
 *
 * Security features:
 * - Length constraints to prevent UI/DB issues
 * - Whitespace normalization
 * - Control character filtering
 * - Empty/blank validation
 */
object InputValidator {

    // Length constraints
    const val MAX_PAINT_NAME_LENGTH = 100
    const val MAX_PAINT_CODE_LENGTH = 50
    const val MAX_PAINT_BRAND_LENGTH = 100
    const val MAX_PAINT_LINE_LENGTH = 100
    const val MAX_NOTES_LENGTH = 1000
    const val MAX_RECIPE_NAME_LENGTH = 100
    const val MAX_RECIPE_STEP_NOTES_LENGTH = 500
    const val MAX_TAG_LENGTH = 30
    const val MAX_TAGS_COUNT = 20
    const val MAX_SEARCH_QUERY_LENGTH = 100

    // Minimum lengths
    const val MIN_NAME_LENGTH = 1

    /**
     * Validates a paint name.
     *
     * Rules:
     * - Not blank after trimming
     * - Between 1-100 characters
     * - No control characters
     */
    fun validatePaintName(name: String): ValidationResult {
        val sanitized = sanitizeUserInput(name)

        return when {
            sanitized.isBlank() -> ValidationResult.Invalid("Paint name cannot be empty. Please enter a name.")
            sanitized.length > MAX_PAINT_NAME_LENGTH ->
                ValidationResult.Invalid("Paint name is too long (${sanitized.length}/$MAX_PAINT_NAME_LENGTH characters). Please shorten it.")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates a paint code (optional field).
     *
     * Rules:
     * - Can be empty/blank (optional field)
     * - Max 50 characters if provided
     * - No control characters
     */
    fun validatePaintCode(code: String): ValidationResult {
        val sanitized = sanitizeUserInput(code)

        return when {
            sanitized.length > MAX_PAINT_CODE_LENGTH ->
                ValidationResult.Invalid("Paint code must be $MAX_PAINT_CODE_LENGTH characters or less")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates a paint brand.
     *
     * Rules:
     * - Not blank after trimming
     * - Between 1-100 characters
     * - No control characters
     */
    fun validatePaintBrand(brand: String): ValidationResult {
        val sanitized = sanitizeUserInput(brand)

        return when {
            sanitized.isBlank() -> ValidationResult.Invalid("Brand cannot be empty. Please enter a brand name.")
            sanitized.length > MAX_PAINT_BRAND_LENGTH ->
                ValidationResult.Invalid("Brand is too long (${sanitized.length}/$MAX_PAINT_BRAND_LENGTH characters). Please shorten it.")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates paint line (optional field).
     */
    fun validatePaintLine(line: String): ValidationResult {
        val sanitized = sanitizeUserInput(line)

        return when {
            sanitized.length > MAX_PAINT_LINE_LENGTH ->
                ValidationResult.Invalid("Paint line must be $MAX_PAINT_LINE_LENGTH characters or less")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates notes field (optional).
     *
     * Rules:
     * - Can be empty/blank
     * - Max 1000 characters if provided
     * - Control characters allowed (newlines, tabs)
     */
    fun validateNotes(notes: String): ValidationResult {
        val sanitized = sanitizeNotes(notes)

        return when {
            sanitized.length > MAX_NOTES_LENGTH ->
                ValidationResult.Invalid("Notes are too long (${sanitized.length}/$MAX_NOTES_LENGTH characters). Please shorten your notes.")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates a recipe name.
     *
     * Rules:
     * - Not blank after trimming
     * - Between 1-100 characters
     * - No control characters
     */
    fun validateRecipeName(name: String): ValidationResult {
        val sanitized = sanitizeUserInput(name)

        return when {
            sanitized.isBlank() -> ValidationResult.Invalid("Recipe name cannot be empty. Please enter a name for your recipe.")
            sanitized.length > MAX_RECIPE_NAME_LENGTH ->
                ValidationResult.Invalid("Recipe name is too long (${sanitized.length}/$MAX_RECIPE_NAME_LENGTH characters). Please shorten it.")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates recipe step notes (optional).
     */
    fun validateRecipeStepNotes(notes: String): ValidationResult {
        val sanitized = sanitizeNotes(notes)

        return when {
            sanitized.length > MAX_RECIPE_STEP_NOTES_LENGTH ->
                ValidationResult.Invalid("Step notes must be $MAX_RECIPE_STEP_NOTES_LENGTH characters or less")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates a single tag.
     *
     * Rules:
     * - Not blank
     * - Max 30 characters
     * - No special characters except hyphen and underscore
     */
    fun validateTag(tag: String): ValidationResult {
        val sanitized = sanitizeUserInput(tag)

        return when {
            sanitized.isBlank() -> ValidationResult.Invalid("Tag cannot be empty")
            sanitized.length > MAX_TAG_LENGTH ->
                ValidationResult.Invalid("Tag must be $MAX_TAG_LENGTH characters or less")
            !sanitized.matches(Regex("^[a-zA-Z0-9\\s_-]+$")) ->
                ValidationResult.Invalid("Tag can only contain letters, numbers, spaces, hyphens, and underscores")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates a list of tags.
     */
    fun validateTags(tags: List<String>): ValidationResult {
        return when {
            tags.size > MAX_TAGS_COUNT ->
                ValidationResult.Invalid("Cannot have more than $MAX_TAGS_COUNT tags")
            else -> {
                tags.forEach { tag ->
                    val result = validateTag(tag)
                    if (result is ValidationResult.Invalid) {
                        return result
                    }
                }
                ValidationResult.Valid
            }
        }
    }

    /**
     * Validates a search query.
     *
     * Rules:
     * - Can be empty (clears search)
     * - Max 100 characters
     * - No control characters
     */
    fun validateSearchQuery(query: String): ValidationResult {
        val sanitized = sanitizeUserInput(query)

        return when {
            sanitized.length > MAX_SEARCH_QUERY_LENGTH ->
                ValidationResult.Invalid("Search query must be $MAX_SEARCH_QUERY_LENGTH characters or less")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Sanitizes user input by:
     * - Trimming whitespace
     * - Removing control characters (except newlines/tabs)
     * - Normalizing consecutive spaces
     *
     * Use for single-line inputs like names, codes, brands.
     */
    fun sanitizeUserInput(input: String): String {
        return input
            .trim()
            .replace(Regex("\\p{Cntrl}"), "") // Remove control characters
            .replace(Regex("\\s+"), " ") // Normalize consecutive spaces
    }

    /**
     * Sanitizes notes/multi-line input by:
     * - Trimming whitespace
     * - Removing control characters except \n and \t
     * - Normalizing line breaks
     *
     * Use for multi-line inputs like notes.
     */
    fun sanitizeNotes(notes: String): String {
        return notes
            .trim()
            .replace(Regex("\\p{Cntrl}&&[^\n\t]"), "") // Remove control chars except newline/tab
            .replace(Regex("\n{3,}"), "\n\n") // Max 2 consecutive newlines
            .replace(Regex("[ \t]+"), " ") // Normalize spaces
    }

    /**
     * Sanitizes a hex color code.
     *
     * Rules:
     * - Must start with #
     * - Must be 7 characters (#RRGGBB)
     * - Only hex digits after #
     */
    fun validateHexColor(hex: String): ValidationResult {
        val sanitized = hex.trim().uppercase()

        return when {
            !sanitized.matches(Regex("^#[0-9A-F]{6}$")) ->
                ValidationResult.Invalid("Color must be in format #RRGGBB (e.g., #FF5733). Please check the color code.")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Sanitizes and validates all fields for creating/editing a user paint.
     * Returns the first validation error or Valid if all fields are valid.
     */
    fun validateUserPaint(
        name: String,
        brand: String,
        code: String = "",
        line: String = "",
        notes: String = "",
        hex: String
    ): ValidationResult {
        validatePaintName(name).let { if (it is ValidationResult.Invalid) return it }
        validatePaintBrand(brand).let { if (it is ValidationResult.Invalid) return it }
        validatePaintCode(code).let { if (it is ValidationResult.Invalid) return it }
        validatePaintLine(line).let { if (it is ValidationResult.Invalid) return it }
        validateNotes(notes).let { if (it is ValidationResult.Invalid) return it }
        validateHexColor(hex).let { if (it is ValidationResult.Invalid) return it }

        return ValidationResult.Valid
    }

    /**
     * Sanitizes and validates all fields for creating/editing a recipe.
     */
    fun validateRecipe(
        name: String,
        notes: String = "",
        tags: List<String> = emptyList()
    ): ValidationResult {
        validateRecipeName(name).let { if (it is ValidationResult.Invalid) return it }
        validateNotes(notes).let { if (it is ValidationResult.Invalid) return it }
        validateTags(tags).let { if (it is ValidationResult.Invalid) return it }

        return ValidationResult.Valid
    }
}
