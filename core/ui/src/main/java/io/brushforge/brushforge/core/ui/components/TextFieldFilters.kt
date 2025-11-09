package io.brushforge.brushforge.core.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import io.brushforge.brushforge.domain.util.InputValidator

/**
 * Visual transformation that enforces maximum length on text fields.
 * Characters beyond the limit are simply not accepted.
 */
class MaxLengthFilter(private val maxLength: Int) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length > maxLength) {
            text.text.substring(0, maxLength)
        } else {
            text.text
        }

        return TransformedText(
            AnnotatedString(trimmed),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return offset.coerceAtMost(maxLength)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return offset
                }
            }
        )
    }
}

/**
 * Common text field filters based on InputValidator constraints.
 */
object TextFieldFilters {
    val PaintName = MaxLengthFilter(InputValidator.MAX_PAINT_NAME_LENGTH)
    val PaintCode = MaxLengthFilter(InputValidator.MAX_PAINT_CODE_LENGTH)
    val PaintBrand = MaxLengthFilter(InputValidator.MAX_PAINT_BRAND_LENGTH)
    val PaintLine = MaxLengthFilter(InputValidator.MAX_PAINT_LINE_LENGTH)
    val Notes = MaxLengthFilter(InputValidator.MAX_NOTES_LENGTH)
    val RecipeName = MaxLengthFilter(InputValidator.MAX_RECIPE_NAME_LENGTH)
    val RecipeStepNotes = MaxLengthFilter(InputValidator.MAX_RECIPE_STEP_NOTES_LENGTH)
    val Tag = MaxLengthFilter(InputValidator.MAX_TAG_LENGTH)
    val SearchQuery = MaxLengthFilter(InputValidator.MAX_SEARCH_QUERY_LENGTH)
}
