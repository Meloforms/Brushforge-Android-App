package io.brushforge.brushforge.feature.mypaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.brushforge.brushforge.domain.model.CatalogPaint
import io.brushforge.brushforge.domain.model.MixComponent
import io.brushforge.brushforge.domain.model.PaintFinish
import io.brushforge.brushforge.domain.model.PaintType
import io.brushforge.brushforge.domain.model.UserPaint
import io.brushforge.brushforge.domain.model.MyPaintsSnapshot
import io.brushforge.brushforge.domain.usecase.AddCustomPaintUseCase
import io.brushforge.brushforge.domain.usecase.AddMixedPaintUseCase
import io.brushforge.brushforge.domain.usecase.DeleteUserPaintUseCase
import io.brushforge.brushforge.domain.usecase.ObserveInventoryLimitUseCase
import io.brushforge.brushforge.domain.usecase.ObserveMyPaintsUseCase
import io.brushforge.brushforge.domain.usecase.SeedCatalogIfNeededUseCase
import io.brushforge.brushforge.domain.usecase.ToggleAlmostEmptyStatusUseCase
import io.brushforge.brushforge.domain.usecase.ToggleOwnedStatusUseCase
import io.brushforge.brushforge.domain.usecase.ToggleWishlistStatusUseCase
import io.brushforge.brushforge.domain.usecase.UpdateUserPaintNotesUseCase
import io.brushforge.brushforge.domain.util.ColorFamily
import io.brushforge.brushforge.domain.util.InputValidator
import io.brushforge.brushforge.domain.util.ValidationResult
import io.brushforge.brushforge.domain.util.determineColorFamily
import io.brushforge.brushforge.domain.util.mixComponentsToHex
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(
    kotlinx.coroutines.FlowPreview::class,
    kotlinx.coroutines.ExperimentalCoroutinesApi::class
)
@HiltViewModel
class MyPaintsViewModel @Inject constructor(
    observeMyPaintsUseCase: ObserveMyPaintsUseCase,
    observeInventoryLimitUseCase: ObserveInventoryLimitUseCase,
    private val seedCatalogIfNeededUseCase: SeedCatalogIfNeededUseCase,
    private val addCustomPaintUseCase: AddCustomPaintUseCase,
    private val addMixedPaintUseCase: AddMixedPaintUseCase,
    private val toggleOwnedStatusUseCase: ToggleOwnedStatusUseCase,
    private val toggleWishlistStatusUseCase: ToggleWishlistStatusUseCase,
    private val toggleAlmostEmptyStatusUseCase: ToggleAlmostEmptyStatusUseCase,
    private val updateUserPaintNotesUseCase: UpdateUserPaintNotesUseCase,
    private val deleteUserPaintUseCase: DeleteUserPaintUseCase,
    private val userPaintRepository: io.brushforge.brushforge.domain.repository.UserPaintRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MyPaintsUiState())
    val state: StateFlow<MyPaintsUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MyPaintsEvent>()
    val events = _events.asSharedFlow()

    private val searchQuery = MutableStateFlow("")
    private val collectionFilter = MutableStateFlow(CollectionFilter.All)
    private val colorFilters = MutableStateFlow<Set<ColorFamily>>(emptySet())
    private val brandFilters = MutableStateFlow<Set<String>>(emptySet())
    private val typeFilters = MutableStateFlow<Set<PaintType>>(emptySet())
    private val sortOption = MutableStateFlow(PaintSortOption.ByType)

    init {
        viewModelScope.launch {
            when (seedCatalogIfNeededUseCase()) {
                is SeedCatalogIfNeededUseCase.Result.Seeded -> Unit
                is SeedCatalogIfNeededUseCase.Result.AlreadySeeded -> Unit
                SeedCatalogIfNeededUseCase.Result.NoCatalogData -> {
                    _events.emit(MyPaintsEvent.ShowMessage("Unable to load paint catalog."))
                }
            }
        }

        viewModelScope.launch {
            val filterSelectionFlow = combine(
                colorFilters,
                brandFilters,
                typeFilters,
                sortOption
            ) { colors, brands, types, sort ->
                MyPaintsFilterSelection(
                    colorFamilies = colors,
                    brands = brands,
                    types = types,
                    sortOption = sort
                )
            }

            // Create a debounced search flow that doesn't delay the initial value
            // StateFlow already has distinctUntilChanged behavior, so we don't need to add it
            val debouncedSearchQuery = searchQuery
                .transformLatest { value ->
                    // Emit immediately for empty string, debounce for actual searches
                    if (value.isEmpty()) {
                        emit(value)
                    } else {
                        kotlinx.coroutines.delay(300)
                        emit(value)
                    }
                }

            combine(
                observeMyPaintsUseCase(),
                observeInventoryLimitUseCase(),
                debouncedSearchQuery,
                collectionFilter,
                filterSelectionFlow
            ) { snapshot: MyPaintsSnapshot, limit: Int, query: String, collection: CollectionFilter, filters: MyPaintsFilterSelection ->
                val currentState = _state.value
                val userByStableId = snapshot.userPaints.associateBy { it.stableId }
                val catalogBase: List<PaintListItemUiModel> = snapshot.catalogPaints.map { paint ->
                    val user = userByStableId[paint.stableId]
                    PaintListItemUiModel(
                        stableId = paint.stableId,
                        displayName = paint.name,
                        brand = paint.brand,
                        line = paint.line,
                        hex = paint.hex,
                        typeLabel = paint.type.rawValue,
                        finishLabel = paint.finish.rawValue,
                        paintType = paint.type,
                        paintFinish = paint.finish,
                        isUserPaint = false,
                        isCustom = false,
                        isMixed = false,
                        isOwned = user?.isOwned ?: false,
                        isWishlist = user?.isWishlist ?: false,
                        isAlmostEmpty = user?.isAlmostEmpty ?: false,
                        colorFamily = determineColorFamily(paint.hex)
                    )
                }
                val userItemsBase: List<PaintListItemUiModel> = snapshot.userPaints.map { user ->
                    PaintListItemUiModel(
                        stableId = user.stableId,
                        displayName = user.name,
                        brand = user.brand,
                        line = user.line, // Preserve product line from catalog paints
                        hex = user.hex,
                        typeLabel = user.type?.rawValue ?: "Unknown",
                        finishLabel = user.finish?.rawValue ?: "Unknown",
                        paintType = user.type,
                        paintFinish = user.finish,
                        isUserPaint = true,
                        isCustom = user.isCustom,
                        isMixed = user.isMixed,
                        isOwned = user.isOwned,
                        isWishlist = user.isWishlist,
                        isAlmostEmpty = user.isAlmostEmpty,
                        colorFamily = determineColorFamily(user.hex)
                    )
                }

                val queryLower = query.trim().lowercase()

                fun matchesSearch(item: PaintListItemUiModel): Boolean {
                    if (queryLower.isBlank()) return true
                    return listOf(
                        item.displayName,
                        item.brand,
                        item.line,
                        item.typeLabel,
                        item.finishLabel
                    ).filterNotNull().any { it.lowercase().contains(queryLower) }
                }

                fun matchesCollection(item: PaintListItemUiModel): Boolean {
                    return when (collection) {
                        CollectionFilter.All -> true
                        CollectionFilter.Owned -> item.isOwned
                        CollectionFilter.Wishlist -> item.isWishlist
                        else -> false
                    }
                }

                fun matchesColor(item: PaintListItemUiModel): Boolean {
                    return filters.colorFamilies.isEmpty() || filters.colorFamilies.contains(item.colorFamily)
                }

                fun matchesBrand(item: PaintListItemUiModel): Boolean {
                    return filters.brands.isEmpty() || filters.brands.contains(item.brand)
                }

                fun matchesType(item: PaintListItemUiModel): Boolean {
                    if (filters.types.isEmpty()) return true
                    val type = item.paintType ?: return false
                    return filters.types.contains(type)
                }

                val comparator = comparatorFor(filters.sortOption)

                // Filter helper to avoid duplicate filter chain
                fun applyFilters(items: List<PaintListItemUiModel>): List<PaintListItemUiModel> {
                    return items.asSequence()
                        .filter { matchesCollection(it) }
                        .filter { matchesColor(it) }
                        .filter { matchesSearch(it) }
                        .filter { matchesBrand(it) }
                        .filter { matchesType(it) }
                        .sortedWith(comparator)
                        .toList()
                }

                val catalogItems = applyFilters(catalogBase)
                val userItems = applyFilters(userItemsBase)
                val userStableIds = userItems.mapTo(mutableSetOf()) { it.stableId }
                val dedupedCatalogItems = catalogItems.filter { it.stableId !in userStableIds }
                val visibleItems = mergeVisibleItems(
                    userItems = userItems,
                    catalogItems = dedupedCatalogItems,
                    comparator = comparator
                )

                val options: List<PaintOption> = (catalogBase.map {
                    PaintOption(
                        stableId = it.stableId,
                        displayName = it.displayName,
                        brand = it.brand,
                        hex = it.hex
                    )
                } + userItemsBase.map {
                    PaintOption(
                        stableId = it.stableId,
                        displayName = it.displayName,
                        brand = it.brand,
                        hex = it.hex
                    )
                }).distinctBy { it.stableId }

                val normalizedMixForm = normalizeMixForm(currentState.mixForm, options)

                // Collect brands and types in fewer iterations
                val brandSet = mutableSetOf<String>()
                val typeSet = mutableSetOf<PaintType>()

                snapshot.catalogPaints.forEach { paint ->
                    paint.brand.takeIf { it.isNotBlank() }?.let { brandSet.add(it) }
                    typeSet.add(paint.type)
                }
                snapshot.userPaints.forEach { user ->
                    user.brand.takeIf { it.isNotBlank() }?.let { brandSet.add(it) }
                    user.type?.let { typeSet.add(it) }
                }

                val availableBrands = brandSet.toList().sortedBy { it.lowercase() }
                val availableTypes = typeSet.toList().sortedBy { it.rawValue }

                // Calculate owned and wishlist counts from unique paints by stableId
                // We need to avoid double-counting catalog paints that are also in user paints
                val allItemsByStableId = (catalogBase + userItemsBase).associateBy { it.stableId }
                val ownedCount = allItemsByStableId.values.count { it.isOwned }
                val wishlistCount = allItemsByStableId.values.count { it.isWishlist }

                MyPaintsUiState(
                    isLoading = false,
                    catalogItems = dedupedCatalogItems,
                    userItems = userItems,
                    visibleItems = visibleItems,
                    allItemsByStableId = allItemsByStableId,
                    inventoryLimit = limit,
                    inventoryCount = snapshot.userPaints.count { it.isOwned || it.isWishlist },
                    ownedCount = ownedCount,
                    wishlistCount = wishlistCount,
                    errorMessage = currentState.errorMessage,
                    searchQuery = query,
                    selectedCollection = collection,
                    selectedColorFamilies = filters.colorFamilies,
                    availableColorFamilies = currentState.availableColorFamilies,
                    paintOptions = options,
                    activeSheet = currentState.activeSheet,
                    customForm = currentState.customForm.copy(availableBrands = availableBrands),
                    mixForm = normalizedMixForm,
                    filterState = PaintFilterState(
                        selectedBrands = filters.brands,
                        availableBrands = availableBrands,
                        selectedTypes = filters.types,
                        availableTypes = availableTypes
                    ),
                    sortOption = filters.sortOption
                )
            }
                .catch { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "") }
                }
                .collect { newState ->
                    _state.value = newState
                }
        }
    }

    fun onOwnedToggled(stableId: String, owned: Boolean) {
        // Persist to database first (with limit check)
        viewModelScope.launch {
            when (val result = toggleOwnedStatusUseCase(stableId, owned)) {
                ToggleOwnedStatusUseCase.Result.NotFound -> {
                    _events.emit(MyPaintsEvent.ShowMessage("Paint not found."))
                }
                is ToggleOwnedStatusUseCase.Result.Success -> {
                    // Optimistic UI update after successful check
                    optimisticallyUpdatePaint(stableId) { item ->
                        item.copy(
                            isOwned = owned,
                            isWishlist = if (owned) false else item.isWishlist,
                            isAlmostEmpty = if (!owned) false else item.isAlmostEmpty
                        )
                    }
                }
                is ToggleOwnedStatusUseCase.Result.InventoryLimitReached -> {
                    _events.emit(MyPaintsEvent.InventoryLimitReached(result.limit))
                }
            }
        }
    }

    fun onWishlistToggled(stableId: String, wishlist: Boolean) {
        // Persist to database first (with limit check)
        viewModelScope.launch {
            when (val result = toggleWishlistStatusUseCase(stableId, wishlist)) {
                ToggleWishlistStatusUseCase.Result.NotFound -> {
                    _events.emit(MyPaintsEvent.ShowMessage("Paint not found."))
                }
                is ToggleWishlistStatusUseCase.Result.Success -> {
                    // Optimistic UI update after successful check
                    optimisticallyUpdatePaint(stableId) { item ->
                        item.copy(isWishlist = wishlist)
                    }
                }
                is ToggleWishlistStatusUseCase.Result.InventoryLimitReached -> {
                    _events.emit(MyPaintsEvent.InventoryLimitReached(result.limit))
                }
            }
        }
    }

    fun onAlmostEmptyToggled(stableId: String, almostEmpty: Boolean) {
        // Optimistic UI update for instant feedback
        optimisticallyUpdatePaint(stableId) { item ->
            item.copy(isAlmostEmpty = almostEmpty)
        }

        // Persist to database (Flow will eventually confirm the change)
        viewModelScope.launch {
            when (val result = toggleAlmostEmptyStatusUseCase(stableId, almostEmpty)) {
                ToggleAlmostEmptyStatusUseCase.Result.NotFound -> _events.emit(MyPaintsEvent.ShowMessage("Paint not found."))
                is ToggleAlmostEmptyStatusUseCase.Result.InvalidState -> _events.emit(MyPaintsEvent.ShowMessage(result.reason))
                is ToggleAlmostEmptyStatusUseCase.Result.Success -> Unit
            }
        }
    }

    /**
     * Optimistically updates a paint item in the UI state for instant user feedback.
     * The database Flow will eventually propagate the real state.
     */
    private fun optimisticallyUpdatePaint(stableId: String, transform: (PaintListItemUiModel) -> PaintListItemUiModel) {
        _state.update { currentState ->
            val updatedUserItems = currentState.userItems.map { item ->
                if (item.stableId == stableId) transform(item) else item
            }
            val updatedCatalogItems = currentState.catalogItems.map { item ->
                if (item.stableId == stableId) transform(item) else item
            }

            // Update cached map of all items (unfiltered) so counts stay accurate
            val updatedAllItems = currentState.allItemsByStableId.toMutableMap()
            val existing = updatedAllItems[stableId]
            if (existing != null) {
                updatedAllItems[stableId] = transform(existing)
            } else {
                val fallback = (updatedUserItems + updatedCatalogItems).firstOrNull { it.stableId == stableId }
                if (fallback != null) {
                    updatedAllItems[stableId] = fallback
                }
            }
            val ownedCount = updatedAllItems.values.count { it.isOwned }
            val wishlistCount = updatedAllItems.values.count { it.isWishlist }
            val comparator = comparatorFor(currentState.sortOption)

            // Deduplicate: remove catalog items that exist in user items
            val userStableIds = updatedUserItems.map { it.stableId }.toSet()
            val dedupedCatalogItems = updatedCatalogItems.filter { it.stableId !in userStableIds }

            val visibleItems = mergeVisibleItems(
                userItems = updatedUserItems,
                catalogItems = dedupedCatalogItems,
                comparator = comparator
            )

            currentState.copy(
                userItems = updatedUserItems,
                catalogItems = updatedCatalogItems,
                ownedCount = ownedCount,
                wishlistCount = wishlistCount,
                visibleItems = visibleItems,
                allItemsByStableId = updatedAllItems
            )
        }
    }

    fun onNotesChanged(stableId: String, notes: String?) {
        viewModelScope.launch {
            when (val result = updateUserPaintNotesUseCase(stableId, notes)) {
                UpdateUserPaintNotesUseCase.Result.NotFound -> _events.emit(MyPaintsEvent.ShowMessage("Paint not found."))
                is UpdateUserPaintNotesUseCase.Result.Success -> Unit
            }
        }
    }

    fun onDeleteUserPaint(stableId: String) {
        viewModelScope.launch {
            deleteUserPaintUseCase(stableId)
        }
    }

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
        // Update state immediately for responsive text input
        _state.update { it.copy(searchQuery = value) }
    }

    fun onCollectionFilterSelected(filter: CollectionFilter) {
        collectionFilter.value = filter
    }

    fun onColorFamilyToggled(family: ColorFamily) {
        val current = colorFilters.value
        colorFilters.value = if (current.contains(family)) current - family else current + family
    }

    fun onClearColorFilters() {
        colorFilters.value = emptySet()
    }

    fun onAddButtonClicked() {
        val options = _state.value.paintOptions
        _state.update {
            it.copy(
                activeSheet = BottomSheetContent.Menu,
                customForm = CustomPaintFormState(),
                mixForm = createDefaultMixForm(options)
            )
        }
    }

    fun onDismissSheet() {
        val options = _state.value.paintOptions
        val currentSheet = _state.value.activeSheet
        _state.update { current ->
            when (currentSheet) {
                BottomSheetContent.Menu, BottomSheetContent.Custom, BottomSheetContent.Mix -> current.copy(
                    activeSheet = null,
                    customForm = CustomPaintFormState(),
                    mixForm = createDefaultMixForm(options)
                )
                BottomSheetContent.Filters -> current.copy(activeSheet = null)
                BottomSheetContent.PaintActions -> current.copy(
                    activeSheet = null,
                    selectedPaintForActions = null
                )
                null -> current
            }
        }
    }

    fun onAddCustomSelected() {
        _state.update { it.copy(activeSheet = BottomSheetContent.Custom, customForm = CustomPaintFormState()) }
    }

    fun onEditCustomPaint(stableId: String) {
        viewModelScope.launch {
            val userPaint = userPaintRepository.findByStableId(stableId)
            if (userPaint != null && userPaint.isCustom) {
                // Parse hex to RGB
                val hex = userPaint.hex.removePrefix("#")
                val r = hex.substring(0, 2).toInt(16) / 255f
                val g = hex.substring(2, 4).toInt(16) / 255f
                val b = hex.substring(4, 6).toInt(16) / 255f

                // Determine if using custom brand
                val availableBrands = _state.value.customForm.availableBrands
                val useCustomBrand = userPaint.brand !in availableBrands

                val form = CustomPaintFormState(
                    editingPaintId = stableId,
                    name = userPaint.name,
                    selectedBrand = if (!useCustomBrand) userPaint.brand else null,
                    customBrand = if (useCustomBrand) userPaint.brand else "",
                    colorRed = r,
                    colorGreen = g,
                    colorBlue = b,
                    type = userPaint.type,
                    finish = userPaint.finish,
                    tags = emptySet(), // Tags are not stored in UserPaint yet, but the UI supports them
                    notes = userPaint.notes ?: "",
                    availableBrands = availableBrands,
                    useCustomBrand = useCustomBrand
                )
                _state.update { it.copy(activeSheet = BottomSheetContent.Custom, customForm = form) }
            }
        }
    }

    fun onAddMixSelected() {
        val options = _state.value.paintOptions
        if (options.isEmpty()) {
            viewModelScope.launch {
                _events.emit(MyPaintsEvent.ShowMessage("Add paints before creating a mix."))
            }
            return
        }
        val defaultForm = createDefaultMixForm(options)
        _state.update { it.copy(activeSheet = BottomSheetContent.Mix, mixForm = defaultForm) }
    }

    fun onFiltersClicked() {
        _state.update { it.copy(activeSheet = BottomSheetContent.Filters) }
    }

    fun onPaintLongPressed(stableId: String) {
        val paint = _state.value.allItemsByStableId[stableId]
        if (paint != null) {
            _state.update {
                it.copy(
                    activeSheet = BottomSheetContent.PaintActions,
                    selectedPaintForActions = paint
                )
            }
        }
    }

    fun onCustomNameChanged(value: String) {
        _state.update { it.copy(customForm = it.customForm.copy(name = value)) }
    }

    fun onCustomBrandSelected(brand: String?) {
        _state.update {
            it.copy(customForm = it.customForm.copy(
                selectedBrand = brand,
                useCustomBrand = false,
                showBrandDropdown = false
            ))
        }
    }

    fun onCustomBrandToggle() {
        _state.update {
            it.copy(customForm = it.customForm.copy(
                useCustomBrand = !it.customForm.useCustomBrand,
                showBrandDropdown = false
            ))
        }
    }

    fun onCustomBrandInputChanged(value: String) {
        _state.update { it.copy(customForm = it.customForm.copy(customBrand = value)) }
    }

    fun onToggleBrandDropdown() {
        _state.update { it.copy(customForm = it.customForm.copy(showBrandDropdown = !it.customForm.showBrandDropdown)) }
    }

    fun onColorRedChanged(value: Float) {
        _state.update { it.copy(customForm = it.customForm.copy(colorRed = value.coerceIn(0f, 1f))) }
    }

    fun onColorGreenChanged(value: Float) {
        _state.update { it.copy(customForm = it.customForm.copy(colorGreen = value.coerceIn(0f, 1f))) }
    }

    fun onColorBlueChanged(value: Float) {
        _state.update { it.copy(customForm = it.customForm.copy(colorBlue = value.coerceIn(0f, 1f))) }
    }

    fun onCustomTypeSelected(type: PaintType?) {
        _state.update { it.copy(customForm = it.customForm.copy(type = type)) }
    }

    fun onCustomFinishSelected(finish: PaintFinish?) {
        _state.update { it.copy(customForm = it.customForm.copy(finish = finish)) }
    }

    fun onCustomTagInputChanged(value: String) {
        _state.update { it.copy(customForm = it.customForm.copy(tagInput = value)) }
    }

    fun onCustomTagAdd() {
        val tag = _state.value.customForm.tagInput.trim()
        if (tag.isNotBlank()) {
            _state.update {
                it.copy(customForm = it.customForm.copy(
                    tags = it.customForm.tags + tag,
                    tagInput = ""
                ))
            }
        }
    }

    fun onCustomTagRemove(tag: String) {
        _state.update {
            it.copy(customForm = it.customForm.copy(tags = it.customForm.tags - tag))
        }
    }

    fun onCustomNotesChanged(value: String) {
        _state.update { it.copy(customForm = it.customForm.copy(notes = value)) }
    }

    fun onMixNameChanged(value: String) {
        mutateMixForm { it.copy(name = value) }
    }

    fun onMixBrandSelected(brand: String?) {
        mutateMixForm { it.copy(selectedBrand = brand, showBrandDropdown = false) }
    }

    fun onMixCustomBrandInputChanged(value: String) {
        mutateMixForm { it.copy(customBrand = value) }
    }

    fun onMixBrandToggle() {
        mutateMixForm { it.copy(useCustomBrand = !it.useCustomBrand) }
    }

    fun onMixToggleBrandDropdown() {
        mutateMixForm { it.copy(showBrandDropdown = !it.showBrandDropdown) }
    }

    fun onMixNotesChanged(value: String) {
        mutateMixForm { it.copy(notes = value) }
    }

    fun onMixTypeSelected(type: PaintType?) {
        mutateMixForm { it.copy(type = type) }
    }

    fun onMixFinishSelected(finish: PaintFinish?) {
        mutateMixForm { it.copy(finish = finish) }
    }

    fun onMixAddComponent() {
        val options = _state.value.paintOptions
        if (options.isEmpty()) return
        mutateMixForm { form ->
            if (form.components.size >= 3) form
            else form.copy(components = form.components + MixComponentDraft(paintStableId = options.first().stableId, percentage = ""))
        }
    }

    fun onMixRemoveComponent(id: String) {
        mutateMixForm { form ->
            form.copy(components = form.components.filterNot { it.id == id })
        }
    }

    fun onMixComponentPaintChanged(id: String, stableId: String) {
        mutateMixForm { form ->
            form.copy(components = form.components.map { draft ->
                if (draft.id == id) draft.copy(paintStableId = stableId) else draft
            })
        }
    }

    fun onMixComponentPercentageChanged(id: String, value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
        mutateMixForm { form ->
            form.copy(components = form.components.map { draft ->
                if (draft.id == id) draft.copy(percentage = sanitized) else draft
            })
        }
    }

    fun onSubmitMixPaint() {
        val current = _state.value
        if (!current.mixForm.canSubmit || current.mixForm.isSubmitting) return
        mutateMixForm { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val latest = _state.value
            val optionsMap = latest.paintOptions.associateBy { it.stableId }
            val components = latest.mixForm.components.mapNotNull { draft ->
                val option = draft.paintStableId?.let(optionsMap::get) ?: return@mapNotNull null
                val percentage = draft.percentage.toDoubleOrNull() ?: return@mapNotNull null
                MixComponent(
                    id = UUID.randomUUID(),
                    stableId = option.stableId,
                    name = option.displayName,
                    brand = option.brand,
                    hex = option.hex,
                    percentage = percentage
                )
            }

            // Validate input before submission
            val nameValidation = InputValidator.validatePaintName(latest.mixForm.name)
            if (nameValidation is ValidationResult.Invalid) {
                mutateMixForm { it.copy(isSubmitting = false) }
                _events.emit(MyPaintsEvent.ShowMessage(nameValidation.errorMessage))
                return@launch
            }

            val brandValidation = InputValidator.validatePaintBrand(latest.mixForm.effectiveBrand)
            if (brandValidation is ValidationResult.Invalid) {
                mutateMixForm { it.copy(isSubmitting = false) }
                _events.emit(MyPaintsEvent.ShowMessage(brandValidation.errorMessage))
                return@launch
            }

            val notesValidation = InputValidator.validateNotes(latest.mixForm.notes)
            if (notesValidation is ValidationResult.Invalid) {
                mutateMixForm { it.copy(isSubmitting = false) }
                _events.emit(MyPaintsEvent.ShowMessage(notesValidation.errorMessage))
                return@launch
            }

            val request = AddMixedPaintUseCase.Request(
                name = InputValidator.sanitizeUserInput(latest.mixForm.name),
                brand = InputValidator.sanitizeUserInput(latest.mixForm.effectiveBrand),
                components = components,
                type = latest.mixForm.type,
                finish = latest.mixForm.finish,
                notes = InputValidator.sanitizeNotes(latest.mixForm.notes).takeIf { it.isNotBlank() }
            )

            when (val result = addMixedPaintUseCase(request)) {
                is AddMixedPaintUseCase.Result.Success -> {
                    val options = _state.value.paintOptions
                    _state.update {
                        it.copy(
                            mixForm = createDefaultMixForm(options),
                            activeSheet = null
                        )
                    }
                    _events.emit(MyPaintsEvent.ShowMessage("Mixed paint added."))
                }
                is AddMixedPaintUseCase.Result.InventoryLimitReached -> {
                    mutateMixForm { it.copy(isSubmitting = false) }
                    _events.emit(MyPaintsEvent.InventoryLimitReached(result.limit))
                }
                is AddMixedPaintUseCase.Result.InvalidComposition -> {
                    mutateMixForm { it.copy(isSubmitting = false) }
                    _events.emit(MyPaintsEvent.ShowMessage(result.message))
                }
            }
        }
    }

    fun onSubmitCustomPaint() {
        val form = _state.value.customForm
        if (!form.canSubmit || form.isSubmitting) return
        viewModelScope.launch {
            // Validate all inputs before submission
            val validation = InputValidator.validateUserPaint(
                name = form.name,
                brand = form.effectiveBrand,
                notes = form.notes,
                hex = form.hex
            )

            if (validation is ValidationResult.Invalid) {
                _events.emit(MyPaintsEvent.ShowMessage(validation.errorMessage))
                return@launch
            }

            _state.update { it.copy(customForm = it.customForm.copy(isSubmitting = true)) }

            if (form.editingPaintId != null) {
                // Update existing paint
                val existingPaint = userPaintRepository.findByStableId(form.editingPaintId)
                if (existingPaint != null && existingPaint.isCustom) {
                    val updatedPaint = existingPaint.copy(
                        name = InputValidator.sanitizeUserInput(form.name),
                        brand = InputValidator.sanitizeUserInput(form.effectiveBrand),
                        hex = form.hex,
                        type = form.type,
                        finish = form.finish,
                        notes = InputValidator.sanitizeNotes(form.notes).takeIf { it.isNotBlank() }
                    )
                    userPaintRepository.upsert(updatedPaint)
                    _state.update { it.copy(customForm = CustomPaintFormState(), activeSheet = null) }
                    _events.emit(MyPaintsEvent.ShowMessage("Custom paint updated."))
                }
            } else {
                // Create new paint
                val request = AddCustomPaintUseCase.Request(
                    name = InputValidator.sanitizeUserInput(form.name),
                    brand = InputValidator.sanitizeUserInput(form.effectiveBrand),
                    hex = form.hex,
                    type = form.type,
                    finish = form.finish,
                    notes = InputValidator.sanitizeNotes(form.notes).takeIf { it.isNotBlank() }
                )
                when (val result = addCustomPaintUseCase(request)) {
                    is AddCustomPaintUseCase.Result.Success -> {
                        _state.update { it.copy(customForm = CustomPaintFormState(), activeSheet = null) }
                        _events.emit(MyPaintsEvent.ShowMessage("Custom paint added."))
                    }
                    is AddCustomPaintUseCase.Result.InventoryLimitReached -> {
                        _events.emit(MyPaintsEvent.InventoryLimitReached(result.limit))
                    }
                }
            }
            _state.update { it.copy(customForm = it.customForm.copy(isSubmitting = false)) }
        }
    }

    fun onErrorMessageShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun onBrandFilterToggled(brand: String) {
        brandFilters.update { current ->
            if (current.contains(brand)) current - brand else current + brand
        }
    }

    fun onClearBrandFilters() {
        brandFilters.value = emptySet()
    }

    fun onTypeFilterToggled(type: PaintType) {
        typeFilters.update { current ->
            if (current.contains(type)) current - type else current + type
        }
    }

    fun onClearTypeFilters() {
        typeFilters.value = emptySet()
    }

    fun onClearAllFilters() {
        brandFilters.value = emptySet()
        typeFilters.value = emptySet()
    }

    fun onSortOptionSelected(option: PaintSortOption) {
        sortOption.value = option
    }

    private fun validateHex(hex: String): String? {
        val regex = Regex("^#[0-9A-Fa-f]{6}$")
        return if (regex.matches(hex)) null else "Invalid hex"
    }

    private fun createDefaultMixForm(options: List<PaintOption>): MixPaintFormState {
        val components = when {
            options.size >= 2 -> listOf(
                MixComponentDraft(paintStableId = options[0].stableId, percentage = "50"),
                MixComponentDraft(paintStableId = options[1].stableId, percentage = "50")
            )
            options.isNotEmpty() -> listOf(MixComponentDraft(paintStableId = options.first().stableId, percentage = "100"))
            else -> emptyList()
        }
        val availableBrands = _state.value.customForm.availableBrands
        return normalizeMixForm(MixPaintFormState(components = components, availableBrands = availableBrands), options)
    }

    private fun mutateMixForm(mutator: (MixPaintFormState) -> MixPaintFormState) {
        val options = _state.value.paintOptions
        val mutated = mutator(_state.value.mixForm)
        val normalized = normalizeMixForm(mutated, options)
        _state.update { it.copy(mixForm = normalized) }
    }

    private fun normalizeMixForm(form: MixPaintFormState, options: List<PaintOption>): MixPaintFormState {
        if (options.isEmpty()) {
            return form.copy(
                components = emptyList(),
                previewHex = null,
                validationError = "No paints available to mix."
            )
        }

        val optionMap = options.associateBy { it.stableId }
        val resolvedComponents = form.components.map { draft ->
            val stableId = draft.paintStableId?.takeIf { optionMap.containsKey(it) } ?: optionMap.keys.first()
            val error = validatePercentage(draft.percentage)
            draft.copy(paintStableId = stableId, percentageError = error)
        }

        val preview = computeMixPreview(resolvedComponents, optionMap)
        val validationError = when {
            resolvedComponents.size < 2 -> "Add at least two components."
            resolvedComponents.any { it.percentageError != null } -> "Fix component percentages."
            preview == null -> "Enter valid percentages."
            else -> null
        }

        // Auto-fill brand from first component if not set
        val updatedForm = if (form.effectiveBrand.isBlank() && !form.useCustomBrand && form.selectedBrand == null) {
            val autoBrand = resolvedComponents.firstOrNull()?.paintStableId?.let { optionMap[it]?.brand }
            if (autoBrand != null) {
                form.copy(selectedBrand = autoBrand)
            } else {
                form
            }
        } else {
            form
        }

        return updatedForm.copy(
            components = resolvedComponents,
            previewHex = preview,
            validationError = validationError
        )
    }

    private fun computeMixPreview(
        components: List<MixComponentDraft>,
        options: Map<String, PaintOption>
    ): String? {
        val valid = components.mapNotNull { draft ->
            val option = draft.paintStableId?.let(options::get) ?: return@mapNotNull null
            val percentage = draft.percentage.toDoubleOrNull() ?: return@mapNotNull null
            if (percentage <= 0.0) return@mapNotNull null
            option to percentage
        }
        if (valid.size < 2) return null
        val mixComponents = valid.map { (option, percentage) ->
            MixComponent(
                id = UUID.randomUUID(),
                stableId = option.stableId,
                name = option.displayName,
                brand = option.brand,
                hex = option.hex,
                percentage = percentage
            )
        }
        return mixComponentsToHex(mixComponents)
    }

    private fun validatePercentage(value: String): String? {
        if (value.isBlank()) return "Required"
        val number = value.toDoubleOrNull() ?: return "Invalid"
        return if (number > 0.0) null else "Must be > 0"
    }

    private fun mergeVisibleItems(
        userItems: List<PaintListItemUiModel>,
        catalogItems: List<PaintListItemUiModel>,
        comparator: Comparator<PaintListItemUiModel>
    ): List<PaintListItemUiModel> {
        return (catalogItems + userItems)
            .associateBy { it.stableId }
            .values
            .sortedWith(comparator)
    }

    private fun comparatorFor(option: PaintSortOption): Comparator<PaintListItemUiModel> {
        return when (option) {
            PaintSortOption.Alphabetical -> Comparator { a, b ->
                a.displayName.compareTo(b.displayName, ignoreCase = true)
            }
            PaintSortOption.ByBrand -> Comparator { a, b ->
                val brandCompare = a.brand.compareTo(b.brand, ignoreCase = true)
                if (brandCompare != 0) {
                    brandCompare
                } else {
                    a.displayName.compareTo(b.displayName, ignoreCase = true)
                }
            }
            PaintSortOption.ByType -> Comparator { a, b ->
                val typeCompare = paintTypeOrder(a.paintType) - paintTypeOrder(b.paintType)
                if (typeCompare != 0) {
                    typeCompare
                } else {
                    val brandCompare = a.brand.compareTo(b.brand, ignoreCase = true)
                    if (brandCompare != 0) {
                        brandCompare
                    } else {
                        a.displayName.compareTo(b.displayName, ignoreCase = true)
                    }
                }
            }
        }
    }

    private fun paintTypeOrder(type: PaintType?): Int {
        return when (type) {
            PaintType.Base -> 0
            PaintType.Layer -> 1
            PaintType.Highlight -> 2
            PaintType.Wash -> 3
            PaintType.Shade -> 4
            PaintType.Contrast -> 5
            PaintType.Metallic -> 6
            PaintType.Primer -> 7
            PaintType.Technical -> 8
            PaintType.Ink -> 9
            PaintType.Air -> 10
            PaintType.Spray -> 11
            PaintType.Dry -> 12
            PaintType.Glaze -> 13
            PaintType.Speed -> 14
            PaintType.Unknown, null -> 15
        }
    }
}

private data class MyPaintsFilterSelection(
    val colorFamilies: Set<ColorFamily>,
    val brands: Set<String>,
    val types: Set<PaintType>,
    val sortOption: PaintSortOption
)
