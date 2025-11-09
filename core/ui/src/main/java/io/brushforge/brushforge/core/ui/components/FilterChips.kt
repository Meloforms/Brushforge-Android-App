package io.brushforge.brushforge.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Reusable filter chip components to reduce code duplication across features.
 *
 * These components provide consistent styling and behavior for common filter
 * chip patterns used throughout the app.
 */

/**
 * A filter chip with a removable close button.
 * Used for displaying active filters (brands, types, etc.) that can be removed.
 *
 * @param label Text to display in the chip
 * @param onClick Callback when the chip is clicked (typically to remove the filter)
 * @param modifier Optional modifier for the chip
 */
@Composable
fun RemovableFilterChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = true,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove $label"
            )
        },
        modifier = modifier
    )
}

/**
 * A filter chip with a toggle state and optional icons.
 * Used for filters that can be selected/deselected.
 *
 * @param label Text to display in the chip
 * @param selected Whether the chip is currently selected
 * @param onClick Callback when the chip is clicked
 * @param modifier Optional modifier for the chip
 * @param selectedIcon Optional icon to show when selected
 * @param unselectedIcon Optional icon to show when unselected
 */
@Composable
fun ToggleFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedIcon: ImageVector? = null,
    unselectedIcon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = if (selectedIcon != null && unselectedIcon != null) {
            {
                Icon(
                    imageVector = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        modifier = modifier
    )
}

/**
 * A filter chip with a colored indicator box.
 * Used for color family filters.
 *
 * @param label Text to display in the chip
 * @param color Color to display in the indicator box
 * @param selected Whether the chip is currently selected
 * @param onClick Callback when the chip is clicked
 * @param modifier Optional modifier for the chip
 */
@Composable
fun ColorFilterChip(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = color,
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
            )
        },
        modifier = modifier
    )
}
