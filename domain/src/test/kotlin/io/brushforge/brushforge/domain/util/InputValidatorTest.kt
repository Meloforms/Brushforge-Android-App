package io.brushforge.brushforge.domain.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for InputValidator to ensure data integrity and security.
 *
 * These tests verify:
 * - Length constraints are enforced
 * - Whitespace normalization works correctly
 * - Control character filtering prevents injection
 * - Empty/blank validation catches invalid input
 * - Hex color validation accepts only valid formats
 */
class InputValidatorTest {

    // ============================================================================
    // Paint Name Validation Tests
    // ============================================================================

    @Test
    fun `validatePaintName rejects empty string`() {
        val result = InputValidator.validatePaintName("")
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            "Paint name cannot be empty. Please enter a name.",
            (result as ValidationResult.Invalid).errorMessage
        )
    }

    @Test
    fun `validatePaintName rejects blank string`() {
        val result = InputValidator.validatePaintName("   ")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validatePaintName accepts valid name`() {
        val result = InputValidator.validatePaintName("Abaddon Black")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validatePaintName rejects name exceeding 100 characters`() {
        val longName = "a".repeat(101)
        val result = InputValidator.validatePaintName(longName)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errorMessage.contains("too long"))
    }

    @Test
    fun `validatePaintName accepts name at exactly 100 characters`() {
        val exactName = "a".repeat(100)
        val result = InputValidator.validatePaintName(exactName)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validatePaintName trims whitespace`() {
        val result = InputValidator.validatePaintName("  Valid Name  ")
        assertTrue(result is ValidationResult.Valid)
    }

    // ============================================================================
    // Paint Brand Validation Tests
    // ============================================================================

    @Test
    fun `validatePaintBrand rejects empty string`() {
        val result = InputValidator.validatePaintBrand("")
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            "Brand cannot be empty. Please enter a brand name.",
            (result as ValidationResult.Invalid).errorMessage
        )
    }

    @Test
    fun `validatePaintBrand accepts valid brand`() {
        val result = InputValidator.validatePaintBrand("Citadel")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validatePaintBrand rejects brand exceeding 100 characters`() {
        val longBrand = "a".repeat(101)
        val result = InputValidator.validatePaintBrand(longBrand)
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // Paint Code Validation Tests (Optional Field)
    // ============================================================================

    @Test
    fun `validatePaintCode accepts empty string for optional field`() {
        val result = InputValidator.validatePaintCode("")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validatePaintCode accepts valid code`() {
        val result = InputValidator.validatePaintCode("ABC-123")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validatePaintCode rejects code exceeding 50 characters`() {
        val longCode = "a".repeat(51)
        val result = InputValidator.validatePaintCode(longCode)
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // Notes Validation Tests
    // ============================================================================

    @Test
    fun `validateNotes accepts empty string`() {
        val result = InputValidator.validateNotes("")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateNotes accepts valid notes with newlines`() {
        val result = InputValidator.validateNotes("Line 1\nLine 2\nLine 3")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateNotes rejects notes exceeding 1000 characters`() {
        val longNotes = "a".repeat(1001)
        val result = InputValidator.validateNotes(longNotes)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errorMessage.contains("too long"))
        assertTrue(result.errorMessage.contains("1001/1000"))
    }

    @Test
    fun `validateNotes accepts notes at exactly 1000 characters`() {
        val exactNotes = "a".repeat(1000)
        val result = InputValidator.validateNotes(exactNotes)
        assertTrue(result is ValidationResult.Valid)
    }

    // ============================================================================
    // Recipe Name Validation Tests
    // ============================================================================

    @Test
    fun `validateRecipeName rejects empty string`() {
        val result = InputValidator.validateRecipeName("")
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            "Recipe name cannot be empty. Please enter a name for your recipe.",
            (result as ValidationResult.Invalid).errorMessage
        )
    }

    @Test
    fun `validateRecipeName accepts valid name`() {
        val result = InputValidator.validateRecipeName("My Painting Recipe")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateRecipeName rejects name exceeding 100 characters`() {
        val longName = "a".repeat(101)
        val result = InputValidator.validateRecipeName(longName)
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // Tag Validation Tests
    // ============================================================================

    @Test
    fun `validateTag rejects empty string`() {
        val result = InputValidator.validateTag("")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateTag accepts valid tag with letters and numbers`() {
        val result = InputValidator.validateTag("warhammer40k")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateTag accepts tag with hyphen and underscore`() {
        val result = InputValidator.validateTag("space-marines_2023")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateTag rejects tag with special characters`() {
        val result = InputValidator.validateTag("tag@with#special!")
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errorMessage.contains("letters, numbers"))
    }

    @Test
    fun `validateTag rejects tag exceeding 30 characters`() {
        val longTag = "a".repeat(31)
        val result = InputValidator.validateTag(longTag)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateTags rejects more than 20 tags`() {
        val tooManyTags = List(21) { "tag$it" }
        val result = InputValidator.validateTags(tooManyTags)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errorMessage.contains("20 tags"))
    }

    @Test
    fun `validateTags accepts exactly 20 tags`() {
        val maxTags = List(20) { "tag$it" }
        val result = InputValidator.validateTags(maxTags)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateTags returns first tag error`() {
        val tagsWithInvalid = listOf("valid", "also-valid", "invalid@tag")
        val result = InputValidator.validateTags(tagsWithInvalid)
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // Search Query Validation Tests
    // ============================================================================

    @Test
    fun `validateSearchQuery accepts empty string`() {
        val result = InputValidator.validateSearchQuery("")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateSearchQuery accepts valid query`() {
        val result = InputValidator.validateSearchQuery("ultramarine blue")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateSearchQuery rejects query exceeding 100 characters`() {
        val longQuery = "a".repeat(101)
        val result = InputValidator.validateSearchQuery(longQuery)
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // Hex Color Validation Tests
    // ============================================================================

    @Test
    fun `validateHexColor accepts valid uppercase hex`() {
        val result = InputValidator.validateHexColor("#FF5733")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateHexColor accepts valid lowercase hex`() {
        val result = InputValidator.validateHexColor("#ff5733")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateHexColor accepts valid mixed case hex`() {
        val result = InputValidator.validateHexColor("#Ff5733")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateHexColor rejects hex without hash`() {
        val result = InputValidator.validateHexColor("FF5733")
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errorMessage.contains("#RRGGBB"))
    }

    @Test
    fun `validateHexColor rejects short hex`() {
        val result = InputValidator.validateHexColor("#FFF")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateHexColor rejects long hex`() {
        val result = InputValidator.validateHexColor("#FF5733AA")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateHexColor rejects invalid characters`() {
        val result = InputValidator.validateHexColor("#GGGGGG")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateHexColor trims whitespace`() {
        val result = InputValidator.validateHexColor("  #FF5733  ")
        assertTrue(result is ValidationResult.Valid)
    }

    // ============================================================================
    // Sanitization Tests
    // ============================================================================

    @Test
    fun `sanitizeUserInput trims whitespace`() {
        val result = InputValidator.sanitizeUserInput("  text  ")
        assertEquals("text", result)
    }

    @Test
    fun `sanitizeUserInput removes control characters`() {
        val result = InputValidator.sanitizeUserInput("text\u0000with\u0001control")
        assertEquals("textwithcontrol", result)
    }

    @Test
    fun `sanitizeUserInput normalizes consecutive spaces`() {
        val result = InputValidator.sanitizeUserInput("text    with    spaces")
        assertEquals("text with spaces", result)
    }

    @Test
    fun `sanitizeUserInput preserves single spaces`() {
        val result = InputValidator.sanitizeUserInput("text with spaces")
        assertEquals("text with spaces", result)
    }

    @Test
    fun `sanitizeNotes preserves newlines`() {
        val result = InputValidator.sanitizeNotes("line 1\nline 2\nline 3")
        assertEquals("line 1\nline 2\nline 3", result)
    }

    @Test
    fun `sanitizeNotes normalizes tabs to spaces`() {
        val result = InputValidator.sanitizeNotes("indented\ttext")
        assertEquals("indented text", result)
    }

    @Test
    fun `sanitizeNotes limits consecutive newlines to 2`() {
        val result = InputValidator.sanitizeNotes("line 1\n\n\n\nline 2")
        assertEquals("line 1\n\nline 2", result)
    }

    @Test
    fun `sanitizeNotes normalizes spaces but not newlines`() {
        val result = InputValidator.sanitizeNotes("text    with\nspaces   and\nnewlines")
        assertEquals("text with\nspaces and\nnewlines", result)
    }

    // ============================================================================
    // Composite Validation Tests
    // ============================================================================

    @Test
    fun `validateUserPaint accepts all valid fields`() {
        val result = InputValidator.validateUserPaint(
            name = "Abaddon Black",
            brand = "Citadel",
            code = "ABC-123",
            line = "Base",
            notes = "Great coverage",
            hex = "#000000"
        )
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateUserPaint returns first error for invalid name`() {
        val result = InputValidator.validateUserPaint(
            name = "",
            brand = "Citadel",
            hex = "#000000"
        )
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errorMessage.contains("name"))
    }

    @Test
    fun `validateUserPaint returns error for invalid hex`() {
        val result = InputValidator.validateUserPaint(
            name = "Valid Paint",
            brand = "Valid Brand",
            hex = "INVALID"
        )
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errorMessage.contains("#RRGGBB"))
    }

    @Test
    fun `validateRecipe accepts all valid fields`() {
        val result = InputValidator.validateRecipe(
            name = "My Recipe",
            notes = "Some notes",
            tags = listOf("tag1", "tag2")
        )
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateRecipe accepts empty optional fields`() {
        val result = InputValidator.validateRecipe(
            name = "My Recipe",
            notes = "",
            tags = emptyList()
        )
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateRecipe returns error for invalid name`() {
        val result = InputValidator.validateRecipe(
            name = "",
            notes = "Valid notes",
            tags = emptyList()
        )
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errorMessage.contains("Recipe name"))
    }

    @Test
    fun `validateRecipe returns error for too many tags`() {
        val result = InputValidator.validateRecipe(
            name = "Valid Recipe",
            notes = "",
            tags = List(21) { "tag$it" }
        )
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).errorMessage.contains("20 tags"))
    }

    // ============================================================================
    // Edge Case Tests
    // ============================================================================

    @Test
    fun `sanitizeUserInput handles only whitespace`() {
        val result = InputValidator.sanitizeUserInput("     ")
        assertEquals("", result)
    }

    @Test
    fun `sanitizeNotes handles empty string`() {
        val result = InputValidator.sanitizeNotes("")
        assertEquals("", result)
    }

    @Test
    fun `validatePaintName handles unicode characters`() {
        val result = InputValidator.validatePaintName("漢字 Paint")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateHexColor case insensitive`() {
        val upper = InputValidator.validateHexColor("#ABCDEF")
        val lower = InputValidator.validateHexColor("#abcdef")
        val mixed = InputValidator.validateHexColor("#AbCdEf")

        assertTrue(upper is ValidationResult.Valid)
        assertTrue(lower is ValidationResult.Valid)
        assertTrue(mixed is ValidationResult.Valid)
    }
}
