package io.brushforge.brushforge.feature.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.brushforge.brushforge.domain.model.CatalogPaint
import io.brushforge.brushforge.domain.model.MatchingAlgorithm
import io.brushforge.brushforge.domain.model.PaintMatch
import io.brushforge.brushforge.domain.model.PaintMixRecipe
import io.brushforge.brushforge.domain.model.PaintType
import io.brushforge.brushforge.domain.repository.CatalogPaintRepository
import io.brushforge.brushforge.domain.usecase.FindMixingRecipesUseCase
import io.brushforge.brushforge.domain.usecase.FindSimilarPaintsUseCase
import io.brushforge.brushforge.domain.util.InputValidator
import io.brushforge.brushforge.domain.util.ValidationResult
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val catalogPaintRepository: CatalogPaintRepository,
    private val userPaintRepository: io.brushforge.brushforge.domain.repository.UserPaintRepository,
    private val findSimilarPaintsUseCase: FindSimilarPaintsUseCase,
    private val findMixingRecipesUseCase: FindMixingRecipesUseCase,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ConverterUiState())
    val state: StateFlow<ConverterUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ConverterEvent>()
    val events = _events.asSharedFlow()

    private var hasLoadedFromNavigation = false

    init {
        loadBrands()
        loadAllPaints()
        observeUserPaints()
    }

    private fun observeUserPaints() {
        viewModelScope.launch {
            userPaintRepository.observeUserPaints().collect { userPaints ->
                _state.update { it.copy(userPaints = userPaints) }
            }
        }
    }

    private fun loadBrands() {
        viewModelScope.launch {
            try {
                val brands = catalogPaintRepository.getAllBrands()
                _state.update { it.copy(availableBrands = brands) }
            } catch (e: Exception) {
                _events.emit(ConverterEvent.ShowError("Failed to load brands"))
            }
        }
    }

    private fun loadAllPaints() {
        viewModelScope.launch {
            try {
                val paints = catalogPaintRepository.getAllPaints()
                _state.update {
                    it.copy(
                        allPaints = paints,
                        searchResults = paints.take(100), // Show first 100 initially
                        showSearchResults = true // Show paints immediately on load
                    )
                }
            } catch (e: Exception) {
                _events.emit(ConverterEvent.ShowError("Failed to load paints"))
            }
        }
    }

    // ============================================================================
    // Paint Search - Query and filters for finding paints
    // ============================================================================

    fun onSearchQueryChanged(query: String) {
        // Validate and sanitize search query
        val validation = InputValidator.validateSearchQuery(query)
        val sanitized = if (validation is ValidationResult.Invalid) {
            // Silently truncate instead of showing error for better UX
            query.take(InputValidator.MAX_SEARCH_QUERY_LENGTH)
        } else {
            InputValidator.sanitizeUserInput(query)
        }

        _state.update { it.copy(searchQuery = sanitized) }
        // Filter results as user types
        filterSearchResults(sanitized)
    }

    private fun filterSearchResults(query: String) {
        val trimmedQuery = query.trim()
        val brandFilter = _state.value.searchBrandFilter
        val typeFilter = _state.value.searchTypeFilter
        val colorFilter = _state.value.searchColorFilter
        val ownedFilter = _state.value.searchOwnedFilter

        // Build sets of stableIds for owned/wishlist filtering
        val ownedStableIds = _state.value.userPaints.filter { it.isOwned }.map { it.stableId }.toSet()
        val wishlistStableIds = _state.value.userPaints.filter { it.isWishlist }.map { it.stableId }.toSet()

        val filtered = _state.value.allPaints
            .filter { paint ->
                // Multi-keyword text search filter
                val matchesText = if (trimmedQuery.isEmpty()) {
                    true
                } else {
                    // Split query into keywords and check if ALL keywords match
                    val keywords = trimmedQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }

                    keywords.all { keyword ->
                        // Create a searchable string containing all paint fields
                        val searchableText = buildString {
                            append(paint.name)
                            append(" ")
                            append(paint.brand)
                            append(" ")
                            paint.line?.let {
                                append(it)
                                append(" ")
                            }
                            paint.code?.let {
                                append(it)
                                append(" ")
                            }
                            append(paint.type.name)
                            append(" ")
                            append(paint.finish.name)
                        }

                        searchableText.contains(keyword, ignoreCase = true)
                    }
                }

                // Brand filter
                val matchesBrand = brandFilter == null || paint.brand == brandFilter

                // Type filter
                val matchesType = typeFilter == null || paint.type == typeFilter

                // Color filter
                val matchesColor = if (colorFilter.isEmpty()) {
                    true
                } else {
                    val paintColorFamily = io.brushforge.brushforge.domain.util.determineColorFamily(paint.hex)
                    colorFilter.contains(paintColorFamily)
                }

                // Owned/Wishlist filter
                val matchesOwnedFilter = when (ownedFilter) {
                    SearchOwnedFilter.All -> true
                    SearchOwnedFilter.Owned -> paint.stableId in ownedStableIds
                    SearchOwnedFilter.Wishlist -> paint.stableId in wishlistStableIds
                }

                matchesText && matchesBrand && matchesType && matchesColor && matchesOwnedFilter
            }
            .take(100)

        _state.update { it.copy(searchResults = filtered) }
    }

    fun onSearchBrandFilterChanged(brand: String?) {
        _state.update { it.copy(searchBrandFilter = brand) }
        filterSearchResults(_state.value.searchQuery)
    }

    fun onSearchTypeFilterChanged(type: PaintType?) {
        _state.update { it.copy(searchTypeFilter = type) }
        filterSearchResults(_state.value.searchQuery)
    }

    fun onSearchColorFilterToggle(colorFamily: io.brushforge.brushforge.domain.util.ColorFamily) {
        _state.update { currentState ->
            val newColorFilter = if (currentState.searchColorFilter.contains(colorFamily)) {
                currentState.searchColorFilter - colorFamily
            } else {
                currentState.searchColorFilter + colorFamily
            }
            currentState.copy(searchColorFilter = newColorFilter)
        }
        filterSearchResults(_state.value.searchQuery)
    }

    fun onSearchFieldFocused() {
        // Show search results when field is focused
        _state.update { it.copy(showSearchResults = true) }
    }

    // ============================================================================
    // Source Paint Selection - Select/clear the paint to convert from
    // ============================================================================

    fun onSelectSourcePaint(paint: CatalogPaint) {
        _state.update {
            it.copy(
                selectedSourcePaint = paint,
                showSearchResults = false,
                searchQuery = ""
            )
        }
    }

    fun onClearSourcePaint() {
        _state.update {
            it.copy(
                selectedSourcePaint = null,
                matches = emptyList(),
                mixRecipes = emptyList(),
                selectedMatch = null
            )
        }
    }

    // ============================================================================
    // Brand Filters - Toggle target brand filters for matching
    // ============================================================================

    fun onToggleBrandFilter(brand: String) {
        _state.update { state ->
            val newBrands = if (state.selectedTargetBrands.contains(brand)) {
                state.selectedTargetBrands - brand
            } else {
                state.selectedTargetBrands + brand
            }
            state.copy(selectedTargetBrands = newBrands)
        }
    }

    fun onSelectAllBrands() {
        _state.update { it.copy(selectedTargetBrands = it.availableBrands.toSet()) }
    }

    fun onClearAllBrands() {
        _state.update { it.copy(selectedTargetBrands = emptySet()) }
    }

    fun onRequireSameTypeChanged(require: Boolean) {
        _state.update { it.copy(requireSameType = require) }
    }

    // ============================================================================
    // Matching & Recipe Finding - Find similar paints and mix recipes
    // ============================================================================

    fun onFindMatches() {
        val sourcePaint = _state.value.selectedSourcePaint
        if (sourcePaint == null) {
            viewModelScope.launch {
                _events.emit(ConverterEvent.ShowError("Please select a source paint first"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val targetBrands = _state.value.selectedTargetBrands.takeIf { it.isNotEmpty() }
                val matches = findSimilarPaintsUseCase(
                    sourcePaint = sourcePaint,
                    targetBrands = targetBrands,
                    algorithm = MatchingAlgorithm.DELTA_E_2000, // Always use the best algorithm
                    requireSameType = _state.value.requireSameType,
                    requireSameFinish = false,
                    limit = 50
                )

                _state.update {
                    it.copy(
                        isLoading = false,
                        matches = matches,
                        currentView = ConverterView.Results
                    )
                }

                if (matches.isEmpty()) {
                    _events.emit(ConverterEvent.ShowError("No matches found with current filters"))
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _events.emit(ConverterEvent.ShowError("Failed to find matches: ${e.message}"))
            }
        }
    }

    fun onFindMixRecipes() {
        val sourcePaint = _state.value.selectedSourcePaint
        if (sourcePaint == null) {
            viewModelScope.launch {
                _events.emit(ConverterEvent.ShowError("Please select a source paint first"))
            }
            return
        }

        viewModelScope.launch {
            // Switch to MixResults view first, then start loading
            _state.update {
                it.copy(
                    isLoading = true,
                    currentView = ConverterView.MixResults,
                    mixRecipes = emptyList() // Clear old recipes
                )
            }

            try {
                val targetBrands = _state.value.selectedTargetBrands.takeIf { it.isNotEmpty() }
                val recipes = withContext(Dispatchers.Default) {
                    findMixingRecipesUseCase(
                        targetPaint = sourcePaint,
                        sourceBrands = targetBrands,
                        algorithm = MatchingAlgorithm.DELTA_E_2000, // Always use the best algorithm
                        requireSameType = _state.value.requireSameType,
                        maxComponents = 3,
                        minPercentage = 5.0,
                        maxResults = 6
                    )
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        mixRecipes = recipes
                    )
                }

                if (recipes.isEmpty()) {
                    _events.emit(ConverterEvent.ShowError("No mix recipes found with current filters"))
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _events.emit(ConverterEvent.ShowError("Failed to find recipes: ${e.message}"))
            }
        }
    }

    // ============================================================================
    // Navigation & Selection - Match/recipe selection and screen navigation
    // ============================================================================

    fun onMatchSelected(match: PaintMatch) {
        _state.update {
            it.copy(
                selectedMatch = match,
                currentView = ConverterView.Detail
            )
        }
    }

    fun onRecipeSelected(recipe: PaintMixRecipe) {
        _state.update {
            it.copy(
                selectedRecipe = recipe,
                currentView = ConverterView.MixDetail
            )
        }
    }

    fun onBackPressed() {
        _state.update { state ->
            when (state.currentView) {
                ConverterView.Search -> state // Already at root
                ConverterView.Results, ConverterView.MixResults -> state.copy(
                    currentView = ConverterView.Search,
                    matches = emptyList(),
                    mixRecipes = emptyList()
                )
                ConverterView.Detail -> state.copy(
                    currentView = ConverterView.Results,
                    selectedMatch = null
                )
                ConverterView.MixDetail -> state.copy(
                    currentView = ConverterView.MixResults,
                    selectedRecipe = null
                )
            }
        }
    }

    // ============================================================================
    // Result Filtering & Sorting - Sort options and quality filters
    // ============================================================================

    fun onSortOptionChanged(option: MatchSortOption) {
        _state.update { state ->
            val sortedMatches = when (option) {
                MatchSortOption.ByDistance -> state.matches.sortedBy { it.distance }
                MatchSortOption.ByConfidence -> state.matches.sortedByDescending { it.confidence }
                MatchSortOption.ByBrand -> state.matches.sortedBy { it.paint.brand }
            }
            state.copy(
                sortOption = option,
                matches = sortedMatches
            )
        }
    }

    fun onFilterQualityChanged(showExcellentOnly: Boolean, showGoodOnly: Boolean) {
        _state.update {
            it.copy(
                showExcellentOnly = showExcellentOnly,
                showGoodOnly = showGoodOnly
            )
        }
    }

    fun onToggleOwnedOnly() {
        _state.update { it.copy(showOwnedOnly = !it.showOwnedOnly) }
    }

    // ============================================================================
    // UI Dialog Management - Filter sheet and info dialog
    // ============================================================================

    fun onOpenFilterSheet() {
        _state.update { it.copy(showFilterSheet = true) }
    }

    fun onCloseFilterSheet() {
        _state.update { it.copy(showFilterSheet = false) }
    }

    fun onClearSearchFilters() {
        _state.update {
            it.copy(
                searchBrandFilter = null,
                searchTypeFilter = null,
                searchOwnedFilter = SearchOwnedFilter.All
            )
        }
        filterSearchResults(_state.value.searchQuery)
    }

    fun onSearchOwnedFilterChanged(filter: SearchOwnedFilter) {
        _state.update { it.copy(searchOwnedFilter = filter) }
        filterSearchResults(_state.value.searchQuery)
    }

    fun onOpenInfoDialog() {
        _state.update { it.copy(showInfoDialog = true) }
    }

    fun onCloseInfoDialog() {
        _state.update { it.copy(showInfoDialog = false) }
    }

    fun loadPaintFromNavigationIfNeeded() {
        if (hasLoadedFromNavigation) return
        hasLoadedFromNavigation = true

        val stableId: String? = savedStateHandle.get<String>("stableId")
        android.util.Log.d("ConverterViewModel", "loadPaintFromNavigationIfNeeded: stableId=$stableId")
        if (stableId != null && stableId != "null") {
            viewModelScope.launch {
                try {
                    val paint = catalogPaintRepository.findByStableId(stableId)
                    android.util.Log.d("ConverterViewModel", "Found paint: ${paint?.name}")
                    if (paint != null) {
                        onSelectSourcePaint(paint)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ConverterViewModel", "Error loading paint", e)
                    _events.emit(ConverterEvent.ShowError("Failed to load paint: ${e.message}"))
                }
            }
        }
    }
}

data class ConverterUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val allPaints: List<CatalogPaint> = emptyList(),
    val searchResults: List<CatalogPaint> = emptyList(),
    val showSearchResults: Boolean = false,
    val selectedSourcePaint: CatalogPaint? = null,
    val availableBrands: List<String> = emptyList(),
    val selectedTargetBrands: Set<String> = emptySet(),
    val searchBrandFilter: String? = null,
    val searchTypeFilter: PaintType? = null,
    val searchColorFilter: Set<io.brushforge.brushforge.domain.util.ColorFamily> = emptySet(),
    val searchOwnedFilter: SearchOwnedFilter = SearchOwnedFilter.All,
    val requireSameType: Boolean = true,
    val isLoading: Boolean = false,
    val matches: List<PaintMatch> = emptyList(),
    val mixRecipes: List<PaintMixRecipe> = emptyList(),
    val selectedMatch: PaintMatch? = null,
    val selectedRecipe: PaintMixRecipe? = null,
    val currentView: ConverterView = ConverterView.Search,
    val sortOption: MatchSortOption = MatchSortOption.ByDistance,
    val showExcellentOnly: Boolean = false,
    val showGoodOnly: Boolean = false,
    val showOwnedOnly: Boolean = false,
    val userPaints: List<io.brushforge.brushforge.domain.model.UserPaint> = emptyList(),
    val showFilterSheet: Boolean = false,
    val showInfoDialog: Boolean = false
) {
    val filteredMatches: List<PaintMatch>
        get() {
            val ownedStableIds = if (showOwnedOnly) {
                userPaints.map { it.stableId }.toSet()
            } else {
                null
            }

            return matches.filter { match ->
                val qualityMatch = when {
                    showExcellentOnly -> match.isExcellentMatch
                    showGoodOnly -> match.isGoodMatch
                    else -> true
                }
                val ownedMatch = if (ownedStableIds != null) {
                    match.paint.stableId in ownedStableIds
                } else {
                    true
                }
                qualityMatch && ownedMatch
            }
        }

    val activeFilterCount: Int
        get() = (if (searchBrandFilter != null) 1 else 0) +
                (if (searchTypeFilter != null) 1 else 0) +
                (if (searchOwnedFilter != SearchOwnedFilter.All) 1 else 0)
}

enum class ConverterView {
    Search,
    Results,
    MixResults,
    Detail,
    MixDetail
}

enum class MatchSortOption(val displayName: String) {
    ByDistance("Best Match"),
    ByConfidence("Confidence"),
    ByBrand("Brand")
}

enum class SearchOwnedFilter(val displayName: String) {
    All("All Paints"),
    Owned("Owned Only"),
    Wishlist("Wishlist Only")
}

sealed class ConverterEvent {
    data class ShowError(val message: String) : ConverterEvent()
    data class ShowMessage(val message: String) : ConverterEvent()
}
