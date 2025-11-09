package io.brushforge.brushforge.feature.palettes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.brushforge.brushforge.domain.model.CatalogPaint
import io.brushforge.brushforge.domain.model.PaintFinish
import io.brushforge.brushforge.domain.model.PaintType
import io.brushforge.brushforge.domain.model.Recipe
import io.brushforge.brushforge.domain.model.RecipeStep
import io.brushforge.brushforge.domain.repository.RecipeRepository
import io.brushforge.brushforge.domain.repository.UserPaintRepository
import io.brushforge.brushforge.domain.util.InputValidator
import io.brushforge.brushforge.domain.util.ValidationResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val userPaintRepository: UserPaintRepository,
    private val catalogPaintRepository: io.brushforge.brushforge.domain.repository.CatalogPaintRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle["recipeId"])

    private val _state = MutableStateFlow(RecipeDetailUiState())
    val state: StateFlow<RecipeDetailUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RecipeDetailEvent>()
    val events = _events.asSharedFlow()

    init {
        observeRecipe()
        observeUserPaints()
    }

    private fun observeRecipe() {
        viewModelScope.launch {
            recipeRepository.observeRecipe(recipeId).collect { recipe ->
                recipe?.let {
                    _state.update { state ->
                        state.copy(
                            recipe = it,
                            isLoading = false,
                            hasUnsavedChanges = false
                        )
                    }
                }
            }
        }
    }

    private fun observeUserPaints() {
        viewModelScope.launch {
            userPaintRepository.observeUserPaints().collect { userPaints ->
                val ownedStableIds = userPaints.filter { it.isOwned }.map { it.stableId }.toSet()
                _state.update {
                    it.copy(
                        ownedPaintStableIds = ownedStableIds,
                        availablePaints = userPaints
                    )
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        // Sanitize input as user types, but allow empty during editing
        val sanitized = InputValidator.sanitizeUserInput(name)
        _state.update {
            it.copy(
                recipe = it.recipe?.copy(name = sanitized),
                hasUnsavedChanges = true
            )
        }
    }

    fun onToggleFavorite() {
        _state.update {
            it.copy(
                recipe = it.recipe?.copy(isFavorite = !it.recipe.isFavorite),
                hasUnsavedChanges = true
            )
        }
    }

    fun onNotesChanged(notes: String) {
        // Sanitize notes but allow empty
        val sanitized = InputValidator.sanitizeNotes(notes)
        _state.update {
            it.copy(
                recipe = it.recipe?.copy(notes = sanitized.ifBlank { null }),
                hasUnsavedChanges = true
            )
        }
    }

    fun onTagsChanged(tags: List<String>) {
        // Sanitize each tag
        val sanitizedTags = tags.map { InputValidator.sanitizeUserInput(it) }.filter { it.isNotBlank() }
        _state.update {
            it.copy(
                recipe = it.recipe?.copy(tags = sanitizedTags),
                hasUnsavedChanges = true
            )
        }
    }

    fun onSaveRecipe() {
        val recipe = _state.value.recipe ?: return

        // Validate before saving
        val validation = InputValidator.validateRecipe(
            name = recipe.name,
            notes = recipe.notes ?: "",
            tags = recipe.tags
        )

        if (validation is ValidationResult.Invalid) {
            viewModelScope.launch {
                _events.emit(RecipeDetailEvent.ShowError(validation.errorMessage))
            }
            return
        }

        viewModelScope.launch {
            val updated = recipe.copy(dateModified = Instant.now())
            recipeRepository.updateRecipe(updated)
            _state.update { it.copy(hasUnsavedChanges = false) }
        }
    }

    fun onDeleteRecipe() {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipeId)
            _state.update { it.copy(isDeleted = true) }
        }
    }

    fun onShowDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun onDismissDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    fun onShowEditNameDialog() {
        _state.update { it.copy(showEditNameDialog = true, editNameText = it.recipe?.name ?: "") }
    }

    fun onDismissEditNameDialog() {
        _state.update { it.copy(showEditNameDialog = false, editNameText = "") }
    }

    fun onEditNameTextChanged(text: String) {
        _state.update { it.copy(editNameText = text) }
    }

    fun onConfirmEditName() {
        val name = _state.value.editNameText.trim()
        if (name.isNotEmpty()) {
            onNameChanged(name)
            onDismissEditNameDialog()
        }
    }

    fun onShowEditTagsDialog() {
        _state.update {
            it.copy(
                showEditTagsDialog = true,
                editTagsText = it.recipe?.tags?.joinToString(", ") ?: ""
            )
        }
    }

    fun onDismissEditTagsDialog() {
        _state.update { it.copy(showEditTagsDialog = false, editTagsText = "") }
    }

    fun onEditTagsTextChanged(text: String) {
        _state.update { it.copy(editTagsText = text) }
    }

    fun onConfirmEditTags() {
        val tagsText = _state.value.editTagsText.trim()
        val tags = if (tagsText.isNotEmpty()) {
            tagsText.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        } else {
            emptyList()
        }
        onTagsChanged(tags)
        onDismissEditTagsDialog()
    }

    fun onShowEditNotesDialog() {
        _state.update { it.copy(showEditNotesDialog = true, editNotesText = it.recipe?.notes ?: "") }
    }

    fun onDismissEditNotesDialog() {
        _state.update { it.copy(showEditNotesDialog = false, editNotesText = "") }
    }

    fun onEditNotesTextChanged(text: String) {
        _state.update { it.copy(editNotesText = text) }
    }

    fun onConfirmEditNotes() {
        onNotesChanged(_state.value.editNotesText)
        onDismissEditNotesDialog()
    }

    fun onDeleteStep(stepId: String) {
        val recipe = _state.value.recipe ?: return
        val updatedSteps = recipe.steps.filter { it.id != stepId }
            .mapIndexed { index, step -> step.copy(stepIndex = index) }

        _state.update {
            it.copy(
                recipe = recipe.copy(steps = updatedSteps),
                hasUnsavedChanges = true
            )
        }
    }

    fun onMoveStepUp(stepIndex: Int) {
        if (stepIndex <= 0) return
        val recipe = _state.value.recipe ?: return
        val steps = recipe.steps.toMutableList()

        val temp = steps[stepIndex]
        steps[stepIndex] = steps[stepIndex - 1].copy(stepIndex = stepIndex)
        steps[stepIndex - 1] = temp.copy(stepIndex = stepIndex - 1)

        _state.update {
            it.copy(
                recipe = recipe.copy(steps = steps),
                hasUnsavedChanges = true
            )
        }
    }

    fun onMoveStepDown(stepIndex: Int) {
        val recipe = _state.value.recipe ?: return
        if (stepIndex >= recipe.steps.size - 1) return
        val steps = recipe.steps.toMutableList()

        val temp = steps[stepIndex]
        steps[stepIndex] = steps[stepIndex + 1].copy(stepIndex = stepIndex)
        steps[stepIndex + 1] = temp.copy(stepIndex = stepIndex + 1)

        _state.update {
            it.copy(
                recipe = recipe.copy(steps = steps),
                hasUnsavedChanges = true
            )
        }
    }

    fun onShowStepNoteDialog(stepId: String) {
        val step = _state.value.recipe?.steps?.find { it.id == stepId }
        _state.update {
            it.copy(
                showStepNoteDialog = true,
                editingStepId = stepId,
                editStepNoteText = step?.notes ?: ""
            )
        }
    }

    fun onDismissStepNoteDialog() {
        _state.update {
            it.copy(
                showStepNoteDialog = false,
                editingStepId = null,
                editStepNoteText = ""
            )
        }
    }

    fun onEditStepNoteTextChanged(text: String) {
        _state.update { it.copy(editStepNoteText = text) }
    }

    fun onConfirmStepNote() {
        val recipe = _state.value.recipe ?: return
        val stepId = _state.value.editingStepId ?: return
        val noteText = _state.value.editStepNoteText.trim()

        val updatedSteps = recipe.steps.map { step ->
            if (step.id == stepId) {
                step.copy(notes = noteText.ifBlank { null })
            } else {
                step
            }
        }

        _state.update {
            it.copy(
                recipe = recipe.copy(steps = updatedSteps),
                hasUnsavedChanges = true,
                showStepNoteDialog = false,
                editingStepId = null,
                editStepNoteText = ""
            )
        }
    }

    fun onShowPaintPicker() {
        _state.update { it.copy(showPaintPicker = true) }
    }

    fun onDismissPaintPicker() {
        _state.update {
            it.copy(
                showPaintPicker = false,
                replacingStepId = null
            )
        }
    }

    fun onShowReplacePaintDialog(stepId: String) {
        _state.update {
            it.copy(
                showPaintPicker = true,
                replacingStepId = stepId
            )
        }
    }

    fun onPaintSelected(paint: io.brushforge.brushforge.domain.model.UserPaint) {
        val recipe = _state.value.recipe ?: return
        val replacingStepId = _state.value.replacingStepId

        if (replacingStepId != null) {
            // Replace paint in existing step
            val updatedSteps = recipe.steps.map { step ->
                if (step.id == replacingStepId) {
                    step.copy(
                        paintName = paint.name,
                        paintBrand = paint.brand,
                        paintHex = paint.hex,
                        paintStableId = paint.stableId
                    )
                } else {
                    step
                }
            }

            _state.update {
                it.copy(
                    recipe = recipe.copy(steps = updatedSteps),
                    hasUnsavedChanges = true,
                    showPaintPicker = false,
                    replacingStepId = null
                )
            }
        } else {
            // Add new step
            val newStep = RecipeStep(
                id = UUID.randomUUID().toString(),
                stepIndex = recipe.steps.size,
                paintName = paint.name,
                paintBrand = paint.brand,
                paintHex = paint.hex,
                paintStableId = paint.stableId,
                notes = null
            )

            val updatedSteps = recipe.steps + newStep

            _state.update {
                it.copy(
                    recipe = recipe.copy(steps = updatedSteps),
                    hasUnsavedChanges = true,
                    showPaintPicker = false
                )
            }
        }
    }

    fun onFindSubstitutes(step: RecipeStep) {
        viewModelScope.launch {
            val paint = resolveCatalogPaintForStep(step)
            if (paint != null) {
                _state.update { it.copy(paintToFindSubstitutesFor = paint) }
            } else {
                android.util.Log.w(
                    "RecipeDetailViewModel",
                    "Unable to resolve catalog paint for step=${step.paintName}"
                )
            }
        }
    }

    private suspend fun resolveCatalogPaintForStep(step: RecipeStep): CatalogPaint? {
        return try {
            val stableId = step.paintStableId
            val exactMatch = stableId?.let { catalogPaintRepository.findByStableId(it) }
            if (exactMatch != null) {
                android.util.Log.d(
                    "RecipeDetailViewModel",
                    "Resolved paint '${exactMatch.name}' by stableId=$stableId"
                )
                return exactMatch
            }

            val nameMatches = catalogPaintRepository.search(step.paintName, limit = 20)
            val brandMatch = nameMatches.firstOrNull {
                it.brand.equals(step.paintBrand, ignoreCase = true)
            } ?: nameMatches.firstOrNull()

            if (brandMatch != null) {
                android.util.Log.d(
                    "RecipeDetailViewModel",
                    "Resolved paint '${brandMatch.name}' via name search for ${step.paintName}"
                )
                return brandMatch
            }

            val fallback = createFallbackCatalogPaint(step)
            if (fallback != null) {
                android.util.Log.w(
                    "RecipeDetailViewModel",
                    "Falling back to synthesized paint for ${step.paintName}"
                )
            }
            fallback
        } catch (t: Throwable) {
            android.util.Log.e("RecipeDetailViewModel", "Failed to resolve paint for step", t)
            _events.emit(RecipeDetailEvent.ShowError("Unable to find paint for substitutes"))
            null
        }
    }

    private fun createFallbackCatalogPaint(step: RecipeStep): CatalogPaint? {
        val hex = step.paintHex.takeIf { it.isNotBlank() } ?: return null
        return try {
            CatalogPaint.fromHex(
                name = step.paintName,
                code = null,
                brand = step.paintBrand,
                line = inferLineFromStableId(step.paintStableId),
                lineVariant = null,
                hex = hex,
                type = inferPaintType(step.paintStableId),
                finish = inferPaintFinish(step.paintStableId)
            )
        } catch (t: Throwable) {
            android.util.Log.e(
                "RecipeDetailViewModel",
                "Failed to build fallback CatalogPaint for ${step.paintName}",
                t
            )
            null
        }
    }

    private fun inferLineFromStableId(stableId: String?): String? {
        val token = stableId?.split("-")?.getOrNull(1) ?: return null
        return token.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
        }
    }

    private fun inferPaintType(stableId: String?): PaintType {
        val token = stableId
            ?.split("-")
            ?.getOrNull(1)
            ?.lowercase(Locale.ROOT)
            ?: return PaintType.Unknown

        return when {
            token.contains("base") -> PaintType.Base
            token.contains("layer") -> PaintType.Layer
            token.contains("shade") -> PaintType.Shade
            token.contains("wash") -> PaintType.Wash
            token.contains("contrast") -> PaintType.Contrast
            token.contains("air") -> PaintType.Air
            token.contains("dry") -> PaintType.Dry
            token.contains("glaze") -> PaintType.Glaze
            token.contains("primer") -> PaintType.Primer
            else -> PaintType.Unknown
        }
    }

    private fun inferPaintFinish(stableId: String?): PaintFinish {
        val lower = stableId?.lowercase(Locale.ROOT) ?: return PaintFinish.Unknown
        return when {
            lower.contains("gloss") -> PaintFinish.Gloss
            lower.contains("satin") -> PaintFinish.Satin
            lower.contains("metal") -> PaintFinish.Metallic
            else -> PaintFinish.Unknown
        }
    }

    fun onSubstitutesNavigationHandled() {
        _state.update {
            it.copy(paintToFindSubstitutesFor = null)
        }
    }

    fun onToggleEditMode() {
        _state.update {
            it.copy(isEditMode = !it.isEditMode)
        }
    }
}

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val ownedPaintStableIds: Set<String> = emptySet(),
    val availablePaints: List<io.brushforge.brushforge.domain.model.UserPaint> = emptyList(),
    val isLoading: Boolean = true,
    val hasUnsavedChanges: Boolean = false,
    val isDeleted: Boolean = false,
    val isEditMode: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showEditNameDialog: Boolean = false,
    val editNameText: String = "",
    val showEditTagsDialog: Boolean = false,
    val editTagsText: String = "",
    val showEditNotesDialog: Boolean = false,
    val editNotesText: String = "",
    val showStepNoteDialog: Boolean = false,
    val editingStepId: String? = null,
    val editStepNoteText: String = "",
    val showPaintPicker: Boolean = false,
    val replacingStepId: String? = null,
    val paintToFindSubstitutesFor: io.brushforge.brushforge.domain.model.CatalogPaint? = null
)

sealed class RecipeDetailEvent {
    data class ShowError(val message: String) : RecipeDetailEvent()
    data class ShowMessage(val message: String) : RecipeDetailEvent()
}
