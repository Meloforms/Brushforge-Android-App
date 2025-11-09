package io.brushforge.brushforge.core.ui.haptics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Haptic feedback utilities for improved accessibility and user experience.
 *
 * Provides consistent tactile feedback for:
 * - Cognitive disabilities: Confirms actions were registered
 * - Learning disabilities: Reinforces cause-and-effect relationships
 * - General UX: Makes interactions feel more responsive
 */

/**
 * Performs haptic feedback for successful actions.
 * Use when: User completes a task (save, create, delete success)
 */
fun HapticFeedback.success() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

/**
 * Performs haptic feedback for errors or invalid actions.
 * Use when: Validation fails, action is blocked, error occurs
 */
fun HapticFeedback.error() {
    // Double tap for error indication
    performHapticFeedback(HapticFeedbackType.LongPress)
}

/**
 * Performs haptic feedback for selection/toggle actions.
 * Use when: Toggling checkboxes, selecting items, switching tabs
 */
fun HapticFeedback.selection() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

/**
 * Performs haptic feedback for navigation actions.
 * Use when: Navigating between screens, opening/closing dialogs
 */
fun HapticFeedback.navigate() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

/**
 * Performs haptic feedback for destructive actions.
 * Use when: Deleting items, clearing data (stronger feedback for caution)
 */
fun HapticFeedback.destructive() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

/**
 * Modifier that adds haptic feedback to clickable elements.
 *
 * Provides tactile confirmation that helps users with:
 * - Cognitive disabilities understand that their action was registered
 * - Motor impairments confirm they successfully triggered the action
 * - General users feel the interface is responsive
 *
 * @param feedbackType Type of haptic feedback to perform
 * @param enabled Whether haptic feedback is enabled (default: true)
 * @param onClick Action to perform on click
 */
fun Modifier.hapticClickable(
    feedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    this.clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.material3.ripple(),
        enabled = enabled,
        onClick = {
            if (enabled) {
                haptic.performHapticFeedback(feedbackType)
            }
            onClick()
        }
    )
}

/**
 * Modifier that adds success haptic feedback to clickable elements.
 * Use for: Save buttons, create actions, confirmation buttons
 */
fun Modifier.hapticSuccessClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = hapticClickable(HapticFeedbackType.LongPress, enabled, onClick)

/**
 * Modifier that adds selection haptic feedback to clickable elements.
 * Use for: Toggles, checkboxes, filter chips, selection cards
 */
fun Modifier.hapticSelectionClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = hapticClickable(HapticFeedbackType.TextHandleMove, enabled, onClick)

/**
 * Modifier that adds navigation haptic feedback to clickable elements.
 * Use for: Navigation items, cards that open detail screens, back buttons
 */
fun Modifier.hapticNavigationClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = hapticClickable(HapticFeedbackType.LongPress, enabled, onClick)

/**
 * Modifier that adds destructive haptic feedback to clickable elements.
 * Use for: Delete buttons, clear actions, discard buttons
 */
fun Modifier.hapticDestructiveClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = hapticClickable(HapticFeedbackType.LongPress, enabled, onClick)
