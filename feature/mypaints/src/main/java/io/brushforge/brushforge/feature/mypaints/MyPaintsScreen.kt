package io.brushforge.brushforge.feature.mypaints

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import io.brushforge.brushforge.domain.util.ColorFamily
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
            when (state.activeSheet) {
                BottomSheetContent.Menu -> AddPaintMenu(
                    onAddCustom = viewModel::onAddCustomSelected,
                    onMixPaints = viewModel::onAddMixSelected,
                    onDismiss = viewModel::onDismissSheet
                )
                BottomSheetContent.Custom -> AddCustomPaintSheet(
                    form = state.customForm,
                    onNameChanged = viewModel::onCustomNameChanged,
                    onBrandChanged = viewModel::onCustomBrandChanged,
                    onHexChanged = viewModel::onCustomHexChanged,
                    onTypeSelected = viewModel::onCustomTypeSelected,
                    onFinishSelected = viewModel::onCustomFinishSelected,
                    onNotesChanged = viewModel::onCustomNotesChanged,
                    onCancel = viewModel::onDismissSheet,
                    onSubmit = viewModel::onSubmitCustomPaint
                )
                BottomSheetContent.Mix -> AddMixPaintSheet(
                    form = state.mixForm,
                    options = state.paintOptions,
                    onNameChanged = viewModel::onMixNameChanged,
                    onBrandChanged = viewModel::onMixBrandChanged,
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
                null -> Unit
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
            FloatingActionButton(onClick = viewModel::onAddButtonClicked) {
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
                onToggleAlmostEmpty = viewModel::onAlmostEmptyToggled,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onCollectionFilterSelected = viewModel::onCollectionFilterSelected,
                onColorFamilyToggle = viewModel::onColorFamilyToggled,
                onClearColorFilters = viewModel::onClearColorFilters,
                onPaintSelected = onPaintSelected,
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
    onToggleAlmostEmpty: (String, Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCollectionFilterSelected: (CollectionFilter) -> Unit,
    onColorFamilyToggle: (ColorFamily) -> Unit,
    onClearColorFilters: () -> Unit,
    onPaintSelected: (String) -> Unit,
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

        Spacer(modifier = Modifier.height(8.dp))

        // Compact inline inventory indicator
        CompactInventorySummary(
            inventoryCount = state.inventoryCount,
            inventoryLimit = state.inventoryLimit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.userItems.isNotEmpty()) {
                item {
                    CompactSectionHeader(
                        title = "Your Paints",
                        icon = Icons.Filled.ColorLens
                    )
                }
                items(state.userItems, key = { it.stableId }) { item ->
                    PaintCard(
                        item = item,
                        onClick = { onPaintSelected(item.stableId) },
                        onToggleOwned = onToggleOwned,
                        onToggleWishlist = onToggleWishlist,
                        onToggleAlmostEmpty = onToggleAlmostEmpty
                    )
                }
            }
            if (state.catalogItems.isNotEmpty()) {
                item {
                    CompactSectionHeader(
                        title = "Catalog",
                        icon = Icons.Filled.Collections
                    )
                }
                items(state.catalogItems, key = { it.stableId }) { item ->
                    PaintCard(
                        item = item,
                        onClick = { onPaintSelected(item.stableId) },
                        onToggleOwned = onToggleOwned,
                        onToggleWishlist = onToggleWishlist,
                        onToggleAlmostEmpty = onToggleAlmostEmpty
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
        // Compact search bar with reduced padding
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search paints") },
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Inline collection tabs and filter/sort buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Collection filter chips (more compact than segmented buttons)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                val filters = CollectionFilter.values()
                filters.forEachIndexed { index, filter ->
                    val selected = state.selectedCollection == filter
                    SegmentedButton(
                        selected = selected,
                        onClick = { onCollectionFilterSelected(filter) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size)
                    ) {
                        Text(
                            text = when (filter) {
                                CollectionFilter.All -> "All"
                                CollectionFilter.Owned -> "Owned"
                                CollectionFilter.Wishlist -> "Wishlist"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Compact filter/sort row
        CompactFilterSortRow(
            filterState = state.filterState,
            sortOption = state.sortOption,
            onOpenFilters = onOpenFilters,
            onSortOptionSelected = onSortOptionSelected
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

        // Color filters - only show if any are selected (collapsible)
        if (state.selectedColorFamilies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ColorFilterRow(
                availableFamilies = state.availableColorFamilies,
                selectedFamilies = state.selectedColorFamilies,
                onColorFamilyToggle = onColorFamilyToggle,
                onClearColorFilters = onClearColorFilters
            )
        }
    }
}

@Composable
private fun CompactFilterSortRow(
    filterState: PaintFilterState,
    sortOption: PaintSortOption,
    onOpenFilters: () -> Unit,
    onSortOptionSelected: (PaintSortOption) -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onOpenFilters,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val label = if (filterState.activeCount > 0) {
                "Filters (${filterState.activeCount})"
            } else {
                "Filters"
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
        Box(modifier = Modifier.weight(1f)) {
            FilledTonalButton(
                onClick = { sortExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = sortOption.label, style = MaterialTheme.typography.labelLarge)
            }
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false }
            ) {
                PaintSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSortOptionSelected(option)
                            sortExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorFilterRow(
    availableFamilies: List<ColorFamily>,
    selectedFamilies: Set<ColorFamily>,
    onColorFamilyToggle: (ColorFamily) -> Unit,
    onClearColorFilters: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Colors", style = MaterialTheme.typography.labelMedium)
            TextButton(
                onClick = onClearColorFilters,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Clear", style = MaterialTheme.typography.labelSmall)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(availableFamilies) { family ->
                val selected = selectedFamilies.contains(family)
                FilterChip(
                    selected = selected,
                    onClick = { onColorFamilyToggle(family) },
                    label = { Text(colorFamilyLabel(family), style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color = colorFamilyColor(family), shape = MaterialTheme.shapes.small)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterSortRow(
    filterState: PaintFilterState,
    sortOption: PaintSortOption,
    onOpenFilters: () -> Unit,
    onSortOptionSelected: (PaintSortOption) -> Unit
) {
    var sortExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = onOpenFilters,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = Icons.Outlined.Tune, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            val label = if (filterState.activeCount > 0) {
                "Filters (${filterState.activeCount})"
            } else {
                "Filters"
            }
            Text(text = label)
        }
        Box(modifier = Modifier.weight(1f)) {
            FilledTonalButton(
                onClick = { sortExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Outlined.Sort, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Sort: ${sortOption.label}")
            }
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false }
            ) {
                PaintSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSortOptionSelected(option)
                            sortExpanded = false
                        }
                    )
                }
            }
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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Add Paint", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAddCustom, modifier = Modifier.fillMaxWidth()) {
            Text("Add Custom Paint")
        }
        Button(onClick = onMixPaints, modifier = Modifier.fillMaxWidth()) {
            Text("Mix Paints")
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text("Cancel")
        }
    }
}

@Composable
private fun AddCustomPaintSheet(
    form: CustomPaintFormState,
    onNameChanged: (String) -> Unit,
    onBrandChanged: (String) -> Unit,
    onHexChanged: (String) -> Unit,
    onTypeSelected: (PaintType?) -> Unit,
    onFinishSelected: (PaintFinish?) -> Unit,
    onNotesChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Add Custom Paint", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChanged,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.brand,
            onValueChange = onBrandChanged,
            label = { Text("Brand") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.hex,
            onValueChange = onHexChanged,
            label = { Text("Hex color") },
            modifier = Modifier.fillMaxWidth(),
            supportingText = form.hexError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            isError = form.hexError != null
        )
        TypeDropdown(
            label = "Paint type",
            options = PaintType.entries.toList(),
            selected = form.type,
            onSelected = onTypeSelected
        )
        TypeDropdown(
            label = "Finish",
            options = PaintFinish.entries.toList(),
            selected = form.finish,
            onSelected = onFinishSelected
        )
        OutlinedTextField(
            value = form.notes,
            onValueChange = onNotesChanged,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onSubmit, enabled = form.canSubmit && !form.isSubmitting) {
                Text(if (form.isSubmitting) "Saving…" else "Save")
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
    onBrandChanged: (String) -> Unit,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Mix Paints", style = MaterialTheme.typography.titleMedium)
        if (options.isEmpty()) {
            Text(text = "No paints available to mix yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onCancel, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            return
        }

        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChanged,
            label = { Text("Mix name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = form.brand,
            onValueChange = onBrandChanged,
            label = { Text("Brand") },
            modifier = Modifier.fillMaxWidth()
        )

        TypeDropdown(
            label = "Paint type",
            options = PaintType.entries.toList(),
            selected = form.type,
            onSelected = onTypeSelected
        )

        TypeDropdown(
            label = "Finish",
            options = PaintFinish.entries.toList(),
            selected = form.finish,
            onSelected = onFinishSelected
        )

        Text(text = "Components", style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            form.components.forEach { component ->
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
                TextButton(onClick = onAddComponent) {
                    Text("Add component")
                }
            }
        }

        val totalPercentage = form.components.sumOf { it.percentage.toDoubleOrNull() ?: 0.0 }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            form.previewHex?.let {
                ColorSwatch(hex = it, modifier = Modifier.size(32.dp))
            }
            Text(text = "Total percentage: ${"%.1f".format(totalPercentage)}", style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedTextField(
            value = form.notes,
            onValueChange = onNotesChanged,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        form.validationError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var expanded by remember { mutableStateOf(false) }
        val selectedOption = options.firstOrNull { it.stableId == component.paintStableId }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedOption?.displayName ?: "Select paint",
                onValueChange = {},
                readOnly = true,
                label = { Text("Paint") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("${option.displayName} (${option.brand})") },
                        onClick = {
                            onPaintSelected(option.stableId)
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = component.percentage,
            onValueChange = onPercentageChanged,
            label = { Text("Percentage") },
            suffix = { Text("%") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            isError = component.percentageError != null,
            supportingText = component.percentageError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        if (canRemove) {
            TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) {
                Text("Remove component")
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
                .menuAnchor()
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
private fun CompactInventorySummary(
    inventoryCount: Int,
    inventoryLimit: Int,
    modifier: Modifier = Modifier
) {
    val progress = (inventoryCount.toFloat() / inventoryLimit.toFloat()).coerceIn(0f, 1f)
    val isNearLimit = inventoryCount >= (inventoryLimit * 0.8)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Inventory,
                contentDescription = null,
                tint = if (isNearLimit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$inventoryCount / $inventoryLimit",
                style = MaterialTheme.typography.labelMedium,
                color = if (isNearLimit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InventorySummary(
    inventoryCount: Int,
    inventoryLimit: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Inventory",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$inventoryCount of $inventoryLimit paints tracked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun PaintCard(
    item: PaintListItemUiModel,
    onClick: (PaintListItemUiModel) -> Unit,
    onToggleOwned: (String, Boolean) -> Unit,
    onToggleWishlist: (String, Boolean) -> Unit,
    onToggleAlmostEmpty: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.clickable { onClick(item) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorSwatch(hex = item.hex, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = item.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    if (item.isUserPaint) {
                        val flag = when {
                            item.isCustom -> "Custom"
                            item.isMixed -> "Mixed"
                            else -> "User"
                        }
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
                IconToggleButton(
                    checked = item.isOwned,
                    onCheckedChange = { onToggleOwned(item.stableId, it) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (item.isOwned) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = "Owned",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconToggleButton(
                    checked = item.isWishlist,
                    onCheckedChange = { onToggleWishlist(item.stableId, it) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (item.isWishlist) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconToggleButton(
                    checked = item.isAlmostEmpty,
                    onCheckedChange = { onToggleAlmostEmpty(item.stableId, it) },
                    enabled = item.isOwned,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Inventory,
                        contentDescription = "Almost empty",
                        tint = if (item.isAlmostEmpty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, modifier: Modifier = Modifier) {
    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { MaterialTheme.colorScheme.primary }
    Box(
        modifier = modifier
            .background(color = color, shape = MaterialTheme.shapes.small)
    )
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
