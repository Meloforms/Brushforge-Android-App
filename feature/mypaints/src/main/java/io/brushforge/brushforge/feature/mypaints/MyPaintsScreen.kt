package io.brushforge.brushforge.feature.mypaints

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.InputChip
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import io.brushforge.brushforge.domain.util.ColorFamily
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import io.brushforge.brushforge.domain.model.PaintType
import io.brushforge.brushforge.domain.model.PaintFinish

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPaintsScreen(
    modifier: Modifier = Modifier,
    viewModel: MyPaintsViewModel = hiltViewModel(),
    onPaintSelected: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is MyPaintsEvent.InventoryLimitReached -> {
                    snackbarHostState.showSnackbar(
                        message = "Inventory limit reached (${event.limit}). Upgrade to premium to add more."
                    )
                }
                is MyPaintsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.activeSheet) {
        if (state.activeSheet != null) {
            if (!sheetState.isVisible) {
            sheetState.show()
            }
        } else {
            if (sheetState.isVisible) {
                sheetState.hide()
            }
        }
    }

    if (state.activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissSheet() },
            sheetState = sheetState
        ) {
            when (state.activeSheet!!) {
                BottomSheetContent.Menu -> AddPaintMenu(
                    onAddCustom = viewModel::onAddCustomSelected,
                    onMixPaints = viewModel::onAddMixSelected,
                    onDismiss = viewModel::onDismissSheet
                )
                BottomSheetContent.Custom -> AddCustomPaintSheet(
                    form = state.customForm,
                    onNameChanged = viewModel::onCustomNameChanged,
                    onBrandSelected = viewModel::onCustomBrandSelected,
                    onCustomBrandInputChanged = viewModel::onCustomBrandInputChanged,
                    onBrandToggle = viewModel::onCustomBrandToggle,
                    onToggleBrandDropdown = viewModel::onToggleBrandDropdown,
                    onColorRedChanged = viewModel::onColorRedChanged,
                    onColorGreenChanged = viewModel::onColorGreenChanged,
                    onColorBlueChanged = viewModel::onColorBlueChanged,
                    onTypeSelected = viewModel::onCustomTypeSelected,
                    onFinishSelected = viewModel::onCustomFinishSelected,
                    onTagInputChanged = viewModel::onCustomTagInputChanged,
                    onTagAdd = viewModel::onCustomTagAdd,
                    onTagRemove = viewModel::onCustomTagRemove,
                    onNotesChanged = viewModel::onCustomNotesChanged,
                    onCancel = viewModel::onDismissSheet,
                    onSubmit = viewModel::onSubmitCustomPaint
                )
                BottomSheetContent.Mix -> AddMixPaintSheet(
                    form = state.mixForm,
                    options = state.paintOptions,
                    onNameChanged = viewModel::onMixNameChanged,
                    onBrandSelected = viewModel::onMixBrandSelected,
                    onCustomBrandInputChanged = viewModel::onMixCustomBrandInputChanged,
                    onBrandToggle = viewModel::onMixBrandToggle,
                    onToggleBrandDropdown = viewModel::onMixToggleBrandDropdown,
                    onTypeSelected = viewModel::onMixTypeSelected,
                    onFinishSelected = viewModel::onMixFinishSelected,
                    onNotesChanged = viewModel::onMixNotesChanged,
                    onAddComponent = viewModel::onMixAddComponent,
                    onRemoveComponent = viewModel::onMixRemoveComponent,
                    onComponentPaintChanged = viewModel::onMixComponentPaintChanged,
                    onComponentPercentageChanged = viewModel::onMixComponentPercentageChanged,
                    onCancel = viewModel::onDismissSheet,
                    onSubmit = viewModel::onSubmitMixPaint
                )
                BottomSheetContent.Filters -> FilterSortSheet(
                    filters = state.filterState,
                    sortOption = state.sortOption,
                    onBrandToggle = viewModel::onBrandFilterToggled,
                    onClearBrands = viewModel::onClearBrandFilters,
                    onTypeToggle = viewModel::onTypeFilterToggled,
                    onClearTypes = viewModel::onClearTypeFilters,
                    onSortOptionSelected = viewModel::onSortOptionSelected,
                    onClearAll = viewModel::onClearAllFilters,
                    onClose = viewModel::onDismissSheet
                )
                BottomSheetContent.PaintActions -> {
                    state.selectedPaintForActions?.let { paint ->
                        PaintActionsSheet(
                            paint = paint,
                            onToggleOwned = viewModel::onOwnedToggled,
                            onToggleWishlist = viewModel::onWishlistToggled,
                            onToggleAlmostEmpty = viewModel::onAlmostEmptyToggled,
                            onEdit = viewModel::onEditCustomPaint,
                            onDelete = viewModel::onDeleteUserPaint,
                            onDismiss = viewModel::onDismissSheet
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "My Paints") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.onAddButtonClicked()
            }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add paint")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            MyPaintsContent(
                state = state,
                onToggleOwned = viewModel::onOwnedToggled,
                onToggleWishlist = viewModel::onWishlistToggled,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onCollectionFilterSelected = viewModel::onCollectionFilterSelected,
                onColorFamilyToggle = viewModel::onColorFamilyToggled,
                onClearColorFilters = viewModel::onClearColorFilters,
                onPaintSelected = onPaintSelected,
                onPaintLongPressed = viewModel::onPaintLongPressed,
                onOpenFilters = viewModel::onFiltersClicked,
                onSortOptionSelected = viewModel::onSortOptionSelected,
                onBrandFilterToggle = viewModel::onBrandFilterToggled,
                onTypeFilterToggle = viewModel::onTypeFilterToggled,
                onClearAllFilters = viewModel::onClearAllFilters,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun MyPaintsContent(
    state: MyPaintsUiState,
    onToggleOwned: (String, Boolean) -> Unit,
    onToggleWishlist: (String, Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCollectionFilterSelected: (CollectionFilter) -> Unit,
    onColorFamilyToggle: (ColorFamily) -> Unit,
    onClearColorFilters: () -> Unit,
    onPaintSelected: (String) -> Unit,
    onPaintLongPressed: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onSortOptionSelected: (PaintSortOption) -> Unit,
    onBrandFilterToggle: (String) -> Unit,
    onTypeFilterToggle: (PaintType) -> Unit,
    onClearAllFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        SearchAndFilterSection(
            state = state,
            onSearchQueryChange = onSearchQueryChange,
            onCollectionFilterSelected = onCollectionFilterSelected,
            onColorFamilyToggle = onColorFamilyToggle,
            onClearColorFilters = onClearColorFilters,
            onOpenFilters = onOpenFilters,
            onSortOptionSelected = onSortOptionSelected,
            onBrandFilterToggle = onBrandFilterToggle,
            onTypeFilterToggle = onTypeFilterToggle,
            onClearAllFilters = onClearAllFilters
        )

        Spacer(modifier = Modifier.height(12.dp))

        val listItems = state.visibleItems

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Limit reached warning banner (only show when at/near limit)
            if (state.inventoryCount >= state.inventoryLimit) {
                item {
                    LimitReachedBanner(
                        limit = state.inventoryLimit
                    )
                }
            }

            if (listItems.isNotEmpty()) {
                items(listItems, key = { it.stableId }) { item ->
                    PaintCard(
                        item = item,
                        onClick = { onPaintSelected(item.stableId) },
                        onToggleOwned = onToggleOwned,
                        onToggleWishlist = onToggleWishlist,
                        onLongPress = onPaintLongPressed
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterSection(
    state: MyPaintsUiState,
    onSearchQueryChange: (String) -> Unit,
    onCollectionFilterSelected: (CollectionFilter) -> Unit,
    onColorFamilyToggle: (ColorFamily) -> Unit,
    onClearColorFilters: () -> Unit,
    onOpenFilters: () -> Unit,
    onSortOptionSelected: (PaintSortOption) -> Unit,
    onBrandFilterToggle: (String) -> Unit,
    onTypeFilterToggle: (PaintType) -> Unit,
    onClearAllFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar with filter button and clear button
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search paints") },
            shape = MaterialTheme.shapes.medium,
            leadingIcon = {
                Box(
                    modifier = Modifier.clickable { onOpenFilters() },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.filterState.activeCount > 0) {
                        androidx.compose.material3.BadgedBox(
                            badge = {
                                androidx.compose.material3.Badge {
                                    Text(
                                        text = state.filterState.activeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Filter & Sort"
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Filter & Sort"
                        )
                    }
                }
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        modifier = Modifier.clickable { onSearchQueryChange("") }
                    )
                }
            }
        )

        // Collection filter tabs (All/Owned/Wishlist)
        Spacer(modifier = Modifier.height(12.dp))
        CollectionFilterTabs(
            selected = state.selectedCollection,
            ownedCount = state.ownedCount,
            wishlistCount = state.wishlistCount,
            onFilterSelected = onCollectionFilterSelected
        )

        // Color filters - always visible like iOS
        Spacer(modifier = Modifier.height(12.dp))
        ColorFilterRow(
            availableFamilies = state.availableColorFamilies,
            selectedFamilies = state.selectedColorFamilies,
            onColorFamilyToggle = onColorFamilyToggle
        )

        // Active filters - only show if active
        if (state.filterState.activeCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            ActiveFilterChips(
                filterState = state.filterState,
                onBrandChipClick = onBrandFilterToggle,
                onTypeChipClick = onTypeFilterToggle,
                onClearAll = onClearAllFilters
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionFilterTabs(
    selected: CollectionFilter,
    ownedCount: Int,
    wishlistCount: Int,
    onFilterSelected: (CollectionFilter) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedButton(
            selected = selected == CollectionFilter.All,
            onClick = { onFilterSelected(CollectionFilter.All) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            icon = {}
        ) {
            Text("All")
        }
        SegmentedButton(
            selected = selected == CollectionFilter.Owned,
            onClick = { onFilterSelected(CollectionFilter.Owned) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            icon = {}
        ) {
            Text("Owned ($ownedCount)")
        }
        SegmentedButton(
            selected = selected == CollectionFilter.Wishlist,
            onClick = { onFilterSelected(CollectionFilter.Wishlist) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            icon = {}
        ) {
            Text("Wishlist ($wishlistCount)")
        }
    }
}

@Composable
private fun ColorFilterRow(
    availableFamilies: List<ColorFamily>,
    selectedFamilies: Set<ColorFamily>,
    onColorFamilyToggle: (ColorFamily) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(availableFamilies) { family ->
            val selected = selectedFamilies.contains(family)
            FilterChip(
                selected = selected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onColorFamilyToggle(family)
                },
                label = { Text(colorFamilyLabel(family), style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = colorFamilyColor(family),
                                shape = CircleShape
                            )
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterChips(
    filterState: PaintFilterState,
    onBrandChipClick: (String) -> Unit,
    onTypeChipClick: (PaintType) -> Unit,
    onClearAll: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Active filters", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onClearAll) {
                Text("Clear")
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterState.selectedBrands.sorted().forEach { brand ->
                FilterChip(
                    selected = true,
                    onClick = { onBrandChipClick(brand) },
                    label = { Text(brand) },
                    trailingIcon = {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Remove brand")
                    }
                )
            }
            filterState.selectedTypes.sortedBy { it.rawValue }.forEach { type ->
                FilterChip(
                    selected = true,
                    onClick = { onTypeChipClick(type) },
                    label = { Text(type.rawValue) },
                    trailingIcon = {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Remove type")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSortSheet(
    filters: PaintFilterState,
    sortOption: PaintSortOption,
    onBrandToggle: (String) -> Unit,
    onClearBrands: () -> Unit,
    onTypeToggle: (PaintType) -> Unit,
    onClearTypes: () -> Unit,
    onSortOptionSelected: (PaintSortOption) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Filters & Sort", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClose) {
                Text("Done")
            }
        }
            HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Brands", style = MaterialTheme.typography.titleSmall)
            if (filters.availableBrands.isEmpty()) {
                Text(
                    text = "No brands available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.availableBrands.forEach { brand ->
                        val selected = filters.selectedBrands.contains(brand)
                        FilterChip(
                            selected = selected,
                            onClick = { onBrandToggle(brand) },
                            label = { Text(brand) }
                        )
                    }
                }
                if (filters.selectedBrands.isNotEmpty()) {
                    TextButton(onClick = onClearBrands) {
                        Text("Clear brands")
                    }
                }
            }
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Paint types", style = MaterialTheme.typography.titleSmall)
            if (filters.availableTypes.isEmpty()) {
                Text(
                    text = "No types available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.availableTypes.forEach { type ->
                        val selected = filters.selectedTypes.contains(type)
                        FilterChip(
                            selected = selected,
                            onClick = { onTypeToggle(type) },
                            label = { Text(type.rawValue) }
                        )
                    }
                }
                if (filters.selectedTypes.isNotEmpty()) {
                    TextButton(onClick = onClearTypes) {
                        Text("Clear types")
                    }
                }
            }
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Sort by", style = MaterialTheme.typography.titleSmall)
            PaintSortOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sortOption == option,
                        onClick = { onSortOptionSelected(option) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = option.label)
                }
            }
        }
        HorizontalDivider()
        TextButton(onClick = onClearAll) {
            Text("Clear all filters")
        }
    }
}

@Composable
private fun AddPaintMenu(
    onAddCustom: () -> Unit,
    onMixPaints: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add to My Paints",
                style = MaterialTheme.typography.titleLarge
            )
            androidx.compose.material3.IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Paint option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddCustom() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Custom Paint",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Build a paint with your own color, brand, and notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mixed Paint option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMixPaints() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Mixed Paint",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Blend paints you own and reuse the mix in recipes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddCustomPaintSheet(
    form: CustomPaintFormState,
    onNameChanged: (String) -> Unit,
    onBrandSelected: (String?) -> Unit,
    onCustomBrandInputChanged: (String) -> Unit,
    onBrandToggle: () -> Unit,
    onToggleBrandDropdown: () -> Unit,
    onColorRedChanged: (Float) -> Unit,
    onColorGreenChanged: (Float) -> Unit,
    onColorBlueChanged: (Float) -> Unit,
    onTypeSelected: (PaintType?) -> Unit,
    onFinishSelected: (PaintFinish?) -> Unit,
    onTagInputChanged: (String) -> Unit,
    onTagAdd: () -> Unit,
    onTagRemove: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (form.editingPaintId != null) "Edit Custom Paint" else "Add Custom Paint",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Name field
        item {
            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChanged,
                label = { Text("Paint Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Brand selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Brand", style = MaterialTheme.typography.labelLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (form.useCustomBrand) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = form.useCustomBrand,
                            onCheckedChange = { onBrandToggle() }
                        )
                    }
                }

                if (form.useCustomBrand) {
                    OutlinedTextField(
                        value = form.customBrand,
                        onValueChange = onCustomBrandInputChanged,
                        label = { Text("Custom Brand") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = form.showBrandDropdown,
                        onExpandedChange = { onToggleBrandDropdown() }
                    ) {
                        OutlinedTextField(
                            value = form.selectedBrand ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Select Brand") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Dropdown"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = form.showBrandDropdown,
                            onDismissRequest = { onToggleBrandDropdown() }
                        ) {
                            form.availableBrands.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand) },
                                    onClick = { onBrandSelected(brand) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Color picker section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                // Color preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(
                                red = form.colorRed,
                                green = form.colorGreen,
                                blue = form.colorBlue
                            ))
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(
                        text = form.hex,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Red slider
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Red", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${(form.colorRed * 255).toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color(0xFFEF5350)
                        )
                    }
                    Slider(
                        value = form.colorRed,
                        onValueChange = onColorRedChanged,
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Green slider
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Green", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${(form.colorGreen * 255).toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color(0xFF66BB6A)
                        )
                    }
                    Slider(
                        value = form.colorGreen,
                        onValueChange = onColorGreenChanged,
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Blue slider
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Blue", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${(form.colorBlue * 255).toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color(0xFF42A5F5)
                        )
                    }
                    Slider(
                        value = form.colorBlue,
                        onValueChange = onColorBlueChanged,
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Paint type dropdown
        item {
            TypeDropdown(
                label = "Paint Type",
                options = PaintType.entries.toList(),
                selected = form.type,
                onSelected = onTypeSelected
            )
        }

        // Finish dropdown
        item {
            TypeDropdown(
                label = "Finish",
                options = PaintFinish.entries.toList(),
                selected = form.finish,
                onSelected = onFinishSelected
            )
        }

        // Tags section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                // Tag input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = form.tagInput,
                        onValueChange = onTagInputChanged,
                        label = { Text("Add tag") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = onTagAdd,
                        enabled = form.tagInput.trim().isNotBlank()
                    ) {
                        Text("Add")
                    }
                }

                // Display tags
                if (form.tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        form.tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { onTagRemove(tag) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Notes field
        item {
            OutlinedTextField(
                value = form.notes,
                onValueChange = onNotesChanged,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSubmit,
                    enabled = form.canSubmit && !form.isSubmitting
                ) {
                    Text(if (form.isSubmitting) "Saving…" else "Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMixPaintSheet(
    form: MixPaintFormState,
    options: List<PaintOption>,
    onNameChanged: (String) -> Unit,
    onBrandSelected: (String?) -> Unit,
    onCustomBrandInputChanged: (String) -> Unit,
    onBrandToggle: () -> Unit,
    onToggleBrandDropdown: () -> Unit,
    onTypeSelected: (PaintType?) -> Unit,
    onFinishSelected: (PaintFinish?) -> Unit,
    onNotesChanged: (String) -> Unit,
    onAddComponent: () -> Unit,
    onRemoveComponent: (String) -> Unit,
    onComponentPaintChanged: (String, String) -> Unit,
    onComponentPercentageChanged: (String, String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Mix Paints", style = MaterialTheme.typography.titleMedium)
        }

        if (options.isEmpty()) {
            item {
                Text(text = "No paints available to mix yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Button(onCancel, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        } else {

        item {
            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChanged,
                label = { Text("Mix name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Brand selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Brand", style = MaterialTheme.typography.labelLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (form.useCustomBrand) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = form.useCustomBrand,
                            onCheckedChange = { onBrandToggle() }
                        )
                    }
                }

                if (form.useCustomBrand) {
                    OutlinedTextField(
                        value = form.customBrand,
                        onValueChange = onCustomBrandInputChanged,
                        label = { Text("Custom Brand") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = form.showBrandDropdown,
                        onExpandedChange = { onToggleBrandDropdown() }
                    ) {
                        OutlinedTextField(
                            value = form.selectedBrand ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Select Brand") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = form.showBrandDropdown) },
                            modifier = Modifier
                                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = form.showBrandDropdown,
                            onDismissRequest = { onToggleBrandDropdown() }
                        ) {
                            form.availableBrands.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand) },
                                    onClick = { onBrandSelected(brand) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            TypeDropdown(
                label = "Paint type",
                options = PaintType.entries.toList(),
                selected = form.type,
                onSelected = onTypeSelected
            )
        }

        item {
            TypeDropdown(
                label = "Finish",
                options = PaintFinish.entries.toList(),
                selected = form.finish,
                onSelected = onFinishSelected
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text(
                text = "Components",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }

        items(form.components) { component ->
            MixComponentRow(
                component = component,
                options = options,
                onPaintSelected = { onComponentPaintChanged(component.id, it) },
                onPercentageChanged = { onComponentPercentageChanged(component.id, it) },
                onRemove = { onRemoveComponent(component.id) },
                canRemove = form.components.size > 1
            )
        }

        if (form.components.size < 3) {
            item {
                TextButton(onClick = onAddComponent) {
                    Text("Add component")
                }
            }
        }

        item {
            val totalPercentage = form.components.sumOf { it.percentage.toDoubleOrNull() ?: 0.0 }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                form.previewHex?.let {
                    ColorSwatch(hex = it, modifier = Modifier.size(32.dp))
                }
                Text(text = "Total percentage: ${"%.1f".format(totalPercentage)}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            OutlinedTextField(
                value = form.notes,
                onValueChange = onNotesChanged,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        form.validationError?.let { error ->
            item {
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSubmit, enabled = form.canSubmit && !form.isSubmitting) {
                    Text(if (form.isSubmitting) "Saving…" else "Save mix")
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MixComponentRow(
    component: MixComponentDraft,
    options: List<PaintOption>,
    onPaintSelected: (String) -> Unit,
    onPercentageChanged: (String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        var expanded by remember { mutableStateOf(false) }
        val selectedOption = options.firstOrNull { it.stableId == component.paintStableId }

        // Paint selector with color preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color swatch
            selectedOption?.let {
                ColorSwatch(
                    hex = it.hex,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Paint dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedOption?.displayName ?: "Select paint",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Paint") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ColorSwatch(hex = option.hex, modifier = Modifier.size(24.dp))
                                    Text("${option.displayName} (${option.brand})")
                                }
                            },
                            onClick = {
                                onPaintSelected(option.stableId)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Percentage slider
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Percentage", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "${component.percentage.ifEmpty { "0" }}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = component.percentage.toFloatOrNull() ?: 0f,
                onValueChange = { onPercentageChanged(it.toInt().toString()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )

            if (component.percentageError != null) {
                Text(
                    text = component.percentageError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (canRemove) {
            TextButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Remove")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> TypeDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when (selected) {
        is PaintType -> selected.rawValue
        is PaintFinish -> selected.rawValue
        null -> "None"
        else -> selected.toString()
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = {
                onSelected(null)
                expanded = false
            })
            options.forEach { option ->
                val text = when (option) {
                    is PaintType -> option.rawValue
                    is PaintFinish -> option.rawValue
                    else -> option.toString()
                }
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    onSelected(option)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun LimitReachedBanner(
    limit: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Inventory limit reached",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "You've reached your $limit paint limit. Upgrade to Premium for unlimited paints or remove some to add more.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PaintCard(
    item: PaintListItemUiModel,
    onClick: (PaintListItemUiModel) -> Unit,
    onToggleOwned: (String, Boolean) -> Unit,
    onToggleWishlist: (String, Boolean) -> Unit,
    onLongPress: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.combinedClickable(
            onClick = { onClick(item) },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress(item.stableId)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorSwatch(
                hex = item.hex,
                isAlmostEmpty = item.isAlmostEmpty,
                finish = item.paintFinish,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleSmall
                )
                // Display brand and line with different colors
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color(0xFF64B5F6))) {
                            append(item.brand)
                        }
                        if (item.line != null) {
                            append(" ")
                            withStyle(style = SpanStyle(color = Color(0xFF90CAF9))) {
                                append(item.line)
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.typeLabel} • ${item.finishLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Only show flag for custom or mixed paints
                    if (item.isCustom || item.isMixed) {
                        val flag = if (item.isCustom) "Custom" else "Mixed"
                        Text(
                            text = " • $flag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Owned toggle with animation
                val ownedScale by animateFloatAsState(
                    targetValue = if (item.isOwned) 1.1f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "ownedScale"
                )
                IconToggleButton(
                    checked = item.isOwned,
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleOwned(item.stableId, checked)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (item.isOwned) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = "Owned",
                        modifier = Modifier
                            .size(20.dp)
                            .scale(ownedScale)
                    )
                }

                // Wishlist toggle with animation
                val wishlistScale by animateFloatAsState(
                    targetValue = if (item.isWishlist) 1.1f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "wishlistScale"
                )
                IconToggleButton(
                    checked = item.isWishlist,
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleWishlist(item.stableId, checked)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (item.isWishlist) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        modifier = Modifier
                            .size(20.dp)
                            .scale(wishlistScale)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    hex: String,
    isAlmostEmpty: Boolean = false,
    finish: PaintFinish? = null,
    modifier: Modifier = Modifier
) {
    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { MaterialTheme.colorScheme.primary }

    Box(
        modifier = modifier
            .background(
                color = if (isAlmostEmpty) MaterialTheme.colorScheme.surfaceVariant else color,
                shape = MaterialTheme.shapes.small
            )
            .then(
                if (isAlmostEmpty) {
                    Modifier.border(
                        width = 1.dp,
                        color = color,
                        shape = MaterialTheme.shapes.small
                    )
                } else Modifier
            )
    ) {
        // Half-full indicator for almost empty paints
        if (isAlmostEmpty) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
            ) {
                val width = size.width
                val height = size.height
                val waveHeight = height * 0.5f
                val waveAmplitude = height * 0.015f
                val wavelength = width * 0.6f

                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, waveHeight)
                    var x = 0f
                    while (x <= width) {
                        val y = waveHeight + waveAmplitude * kotlin.math.sin((x / wavelength) * 2 * kotlin.math.PI).toFloat()
                        lineTo(x, y)
                        x += 2f
                    }
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = path,
                    color = color
                )
            }
        }

        // Finish overlay effects
        if (!isAlmostEmpty && finish != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
            ) {
                when (finish) {
                    PaintFinish.Metallic -> {
                        // Metallic shine effect - diagonal gradient
                        val gradient = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.0f),
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.0f)
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                        )
                        drawRect(brush = gradient)
                    }
                    PaintFinish.Gloss -> {
                        // Glossy reflection effect - top highlight
                        val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.0f)
                            ),
                            startY = 0f,
                            endY = size.height * 0.5f
                        )
                        drawRect(brush = gradient)
                    }
                    PaintFinish.Satin -> {
                        // Satin subtle sheen - very light reflection
                        val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.0f)
                            ),
                            startY = 0f,
                            endY = size.height * 0.4f
                        )
                        drawRect(brush = gradient)
                    }
                    PaintFinish.Transparent -> {
                        // Checkerboard pattern for transparent
                        val squareSize = size.width / 6
                        for (row in 0..5) {
                            for (col in 0..5) {
                                if ((row + col) % 2 == 0) {
                                    drawRect(
                                        color = Color.White.copy(alpha = 0.2f),
                                        topLeft = androidx.compose.ui.geometry.Offset(
                                            x = col * squareSize,
                                            y = row * squareSize
                                        ),
                                        size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
                                    )
                                }
                            }
                        }
                    }
                    PaintFinish.Matte,
                    PaintFinish.Unknown -> {
                        // No overlay for matte or unknown
                    }
                }
            }
        }
    }
}

@Composable
private fun PaintActionsSheet(
    paint: PaintListItemUiModel,
    onToggleOwned: (String, Boolean) -> Unit,
    onToggleWishlist: (String, Boolean) -> Unit,
    onToggleAlmostEmpty: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Paint?") },
            text = { Text("Are you sure you want to delete \"${paint.displayName}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(paint.stableId)
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Paint preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorSwatch(
                hex = paint.hex,
                isAlmostEmpty = paint.isAlmostEmpty,
                finish = paint.paintFinish,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = paint.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = paint.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Actions
        Column {
            // Toggle Owned (for all paints)
            if (!paint.isOwned) {
                TextButton(
                    onClick = {
                        onToggleOwned(paint.stableId, true)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add to Owned")
                    }
                }
            } else {
                TextButton(
                    onClick = {
                        onToggleOwned(paint.stableId, false)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Remove from Owned")
                    }
                }
            }

            // Toggle Wishlist
            if (!paint.isWishlist) {
                TextButton(
                    onClick = {
                        onToggleWishlist(paint.stableId, true)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add to Wishlist")
                    }
                }
            } else {
                TextButton(
                    onClick = {
                        onToggleWishlist(paint.stableId, false)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Remove from Wishlist")
                    }
                }
            }

            // Toggle Almost Empty (only for owned paints)
            if (paint.isOwned) {
                if (!paint.isAlmostEmpty) {
                    TextButton(
                        onClick = {
                            onToggleAlmostEmpty(paint.stableId, true)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(Icons.Filled.Inventory, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Mark as Almost Empty")
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            onToggleAlmostEmpty(paint.stableId, false)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(Icons.Filled.Inventory, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Unmark as Almost Empty")
                        }
                    }
                }
            }

            // Edit and Delete (only for custom/mix paints)
            if (paint.isCustom || paint.isMixed) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Edit (only for custom paints, not mixed)
                if (paint.isCustom) {
                    TextButton(
                        onClick = {
                            onEdit(paint.stableId)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Edit Paint")
                        }
                    }
                }

                // Delete
                TextButton(
                    onClick = {
                        showDeleteConfirmation = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Delete Paint",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun colorFamilyLabel(family: ColorFamily): String = when (family) {
    ColorFamily.Red -> "Red"
    ColorFamily.Orange -> "Orange"
    ColorFamily.Yellow -> "Yellow"
    ColorFamily.Green -> "Green"
    ColorFamily.Blue -> "Blue"
    ColorFamily.Purple -> "Purple"
    ColorFamily.Pink -> "Pink"
    ColorFamily.Brown -> "Brown"
    ColorFamily.Grey -> "Grey"
    ColorFamily.Black -> "Black"
    ColorFamily.White -> "White"
}

private fun colorFamilyColor(family: ColorFamily): Color = when (family) {
    ColorFamily.Red -> Color(0xFFEF5350)
    ColorFamily.Orange -> Color(0xFFFFA726)
    ColorFamily.Yellow -> Color(0xFFFFEB3B)
    ColorFamily.Green -> Color(0xFF66BB6A)
    ColorFamily.Blue -> Color(0xFF42A5F5)
    ColorFamily.Purple -> Color(0xFFAB47BC)
    ColorFamily.Pink -> Color(0xFFF06292)
    ColorFamily.Brown -> Color(0xFF8D6E63)
    ColorFamily.Grey -> Color(0xFFB0BEC5)
    ColorFamily.Black -> Color(0xFF424242)
    ColorFamily.White -> Color(0xFFFFFFFF)
}
