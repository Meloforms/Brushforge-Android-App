package io.brushforge.brushforge.feature.palettes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.brushforge.brushforge.domain.model.Recipe
import io.brushforge.brushforge.domain.model.RecipeStep
import io.brushforge.brushforge.domain.repository.RecipeRepository
import io.brushforge.brushforge.domain.repository.UserPaintRepository
import io.brushforge.brushforge.domain.sample.SampleRecipeData
import io.brushforge.brushforge.domain.util.InputValidator
import io.brushforge.brushforge.domain.util.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PalettesViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val userPaintRepository: UserPaintRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PalettesUiState())
    val state: StateFlow<PalettesUiState> = _state.asStateFlow()

    init {
        observeRecipes()
        observeUserPaints()
        loadSampleRecipesIfEmpty()
    }

    private fun loadSampleRecipesIfEmpty() {
        viewModelScope.launch {
            val count = recipeRepository.getRecipeCount()
            if (count == 0) {
                // Load sample recipes for demonstration
                SampleRecipeData.getAllSamples().forEach { recipe ->
                    recipeRepository.createRecipe(recipe)
                }
            }
        }
    }

    private fun observeRecipes() {
        viewModelScope.launch {
            recipeRepository.observeAllRecipes().collect { recipes ->
                _state.update { it.copy(recipes = recipes, isLoading = false) }
            }
        }
    }

    private fun observeUserPaints() {
        viewModelScope.launch {
            userPaintRepository.observeUserPaints().collect { userPaints ->
                val ownedStableIds = userPaints.filter { it.isOwned }.map { it.stableId }.toSet()
                _state.update { it.copy(ownedPaintStableIds = ownedStableIds) }
            }
        }
    }

    fun onCreateNewRecipe() {
        _state.update { it.copy(showCreateDialog = true) }
    }

    fun onDismissCreateDialog() {
        _state.update { it.copy(showCreateDialog = false, newRecipeName = "") }
    }

    fun onNewRecipeNameChanged(name: String) {
        _state.update { it.copy(newRecipeName = name) }
    }

    fun onConfirmCreateRecipe() {
        val name = _state.value.newRecipeName

        // Validate recipe name
        val validation = InputValidator.validateRecipeName(name)
        if (validation is ValidationResult.Invalid) {
            // Could emit error event here if needed
            return
        }

        viewModelScope.launch {
            val newRecipe = Recipe(
                id = UUID.randomUUID().toString(),
                name = InputValidator.sanitizeUserInput(name),
                dateCreated = Instant.now(),
                dateModified = Instant.now(),
                isFavorite = false,
                notes = null,
                referenceImageUri = null,
                tags = emptyList(),
                steps = emptyList()
            )

            recipeRepository.createRecipe(newRecipe)
            _state.update {
                it.copy(
                    showCreateDialog = false,
                    newRecipeName = "",
                    selectedRecipeId = newRecipe.id // Navigate to edit
                )
            }
        }
    }

    fun onRecipeSelected(recipeId: String) {
        _state.update { it.copy(selectedRecipeId = recipeId) }
    }

    fun onBackFromDetail() {
        _state.update { it.copy(selectedRecipeId = null) }
    }

    fun onToggleFavorite(recipeId: String) {
        viewModelScope.launch {
            val recipe = recipeRepository.getRecipe(recipeId) ?: return@launch
            val updated = recipe.copy(
                isFavorite = !recipe.isFavorite,
                dateModified = Instant.now()
            )
            recipeRepository.updateRecipe(updated)
        }
    }

    fun onDeleteRecipe(recipeId: String) {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipeId)
        }
    }
}

data class PalettesUiState(
    val recipes: List<Recipe> = emptyList(),
    val ownedPaintStableIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val newRecipeName: String = "",
    val selectedRecipeId: String? = null
)
