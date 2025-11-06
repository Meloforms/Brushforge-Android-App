package io.brushforge.brushforge.feature.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.graphics.toColorInt
import io.brushforge.brushforge.domain.model.CatalogPaint
import io.brushforge.brushforge.domain.model.PaintType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    modifier: Modifier = Modifier,
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ConverterEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is ConverterEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.showFilterSheet) {
        if (state.showFilterSheet) {
            if (!sheetState.isVisible) {
                sheetState.show()
            }
        } else {
            if (sheetState.isVisible) {
                sheetState.hide()
            }
        }
    }

    if (state.showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onCloseFilterSheet() },
            sheetState = sheetState
        ) {
            SearchFilterSheet(
                state = state,
                onBrandFilterChanged = viewModel::onSearchBrandFilterChanged,
                onTypeFilterChanged = viewModel::onSearchTypeFilterChanged,
                onClearAll = viewModel::onClearSearchFilters,
                onClose = viewModel::onCloseFilterSheet
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (state.currentView) {
                            ConverterView.Search -> "Paint Converter"
                            ConverterView.Results -> "Similar Paints"
                            ConverterView.MixResults -> "Mix Recipes"
                            ConverterView.Detail -> "Match Details"
                            ConverterView.MixDetail -> "Recipe Details"
                        }
                    )
                },
                navigationIcon = {
                    if (state.currentView != ConverterView.Search) {
                        IconButton(onClick = { viewModel.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state.currentView) {
                ConverterView.Search -> SearchView(
                    state = state,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onSearchFieldFocused = viewModel::onSearchFieldFocused,
                    onOpenFilterSheet = viewModel::onOpenFilterSheet,
                    onSelectSourcePaint = viewModel::onSelectSourcePaint,
                    onClearSourcePaint = viewModel::onClearSourcePaint,
                    onToggleBrandFilter = viewModel::onToggleBrandFilter,
                    onSelectAllBrands = viewModel::onSelectAllBrands,
                    onClearAllBrands = viewModel::onClearAllBrands,
                    onRequireSameTypeChanged = viewModel::onRequireSameTypeChanged,
                    onFindMatches = viewModel::onFindMatches,
                    onFindMixRecipes = viewModel::onFindMixRecipes
                )
                ConverterView.Results -> ResultsView(
                    state = state,
                    onMatchSelected = viewModel::onMatchSelected,
                    onSortOptionChanged = viewModel::onSortOptionChanged
                )
                ConverterView.MixResults -> MixResultsView(
                    state = state,
                    onRecipeSelected = viewModel::onRecipeSelected
                )
                ConverterView.Detail -> DetailView(
                    state = state
                )
                ConverterView.MixDetail -> MixDetailView(
                    state = state
                )
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchView(
    state: ConverterUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSearchFieldFocused: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    onSelectSourcePaint: (CatalogPaint) -> Unit,
    onClearSourcePaint: () -> Unit,
    onToggleBrandFilter: (String) -> Unit,
    onSelectAllBrands: () -> Unit,
    onClearAllBrands: () -> Unit,
    onRequireSameTypeChanged: (Boolean) -> Unit,
    onFindMatches: () -> Unit,
    onFindMixRecipes: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Section
        item {
            Text(
                text = "1. Select Source Paint",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            if (state.selectedSourcePaint == null) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onSearchFieldFocused()
                            }
                        },
                    placeholder = { Text("Tap to browse or type to search...") },
                    leadingIcon = {
                        Box(
                            modifier = Modifier.clickable { onOpenFilterSheet() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.activeFilterCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(
                                                text = state.activeFilterCount.toString(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = "Filters"
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = "Filters"
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
            } else {
                SelectedPaintCard(
                    paint = state.selectedSourcePaint,
                    onClear = onClearSourcePaint
                )
            }
        }

        // Search Results
        if (state.showSearchResults && state.searchResults.isNotEmpty()) {
            items(state.searchResults) { paint ->
                PaintSearchResultItem(
                    paint = paint,
                    onClick = { onSelectSourcePaint(paint) }
                )
            }
        }

        if (state.selectedSourcePaint != null) {
            // Brand Selection
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "2. Target Brands (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = onSelectAllBrands) {
                                Text("Select All")
                            }
                            TextButton(onClick = onClearAllBrands) {
                                Text("Clear All")
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.availableBrands.forEach { brand ->
                                FilterChip(
                                    selected = state.selectedTargetBrands.contains(brand),
                                    onClick = { onToggleBrandFilter(brand) },
                                    label = { Text(brand) }
                                )
                            }
                        }
                    }
                }
            }

            // Options
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "3. Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRequireSameTypeChanged(!state.requireSameType) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Require Same Type")
                            Text(
                                text = "Base/Layer types are interchangeable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.requireSameType,
                            onCheckedChange = onRequireSameTypeChanged
                        )
                    }
                }
            }

            // Action Buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onFindMatches,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    ) {
                        Text("Find Similar Paints")
                    }

                    OutlinedButton(
                        onClick = onFindMixRecipes,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading
                    ) {
                        Text("Find Mix Recipes")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedPaintCard(
    paint: CatalogPaint,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(paint.hex.toColorInt()))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                Column {
                    Text(
                        text = paint.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${paint.brand} ${paint.line ?: ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
        }
    }
}

@Composable
private fun PaintSearchResultItem(
    paint: CatalogPaint,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(paint.hex.toColorInt()))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
            )
            Column {
                Text(
                    text = paint.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${paint.brand} ${paint.line ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ResultsView(
    state: ConverterUiState,
    onMatchSelected: (io.brushforge.brushforge.domain.model.PaintMatch) -> Unit,
    onSortOptionChanged: (MatchSortOption) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter/Sort Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sort by:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MatchSortOption.entries.forEach { option ->
                        FilterChip(
                            selected = state.sortOption == option,
                            onClick = { onSortOptionChanged(option) },
                            label = { Text(option.displayName) }
                        )
                    }
                }
            }
        }

        // Results
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.filteredMatches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matches found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.filteredMatches) { match ->
                    MatchResultItem(
                        match = match,
                        onClick = { onMatchSelected(match) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchResultItem(
    match: io.brushforge.brushforge.domain.model.PaintMatch,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color swatch - slightly larger and more prominent
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(match.paint.hex.toColorInt()))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            // Paint info in the middle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = match.paint.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = match.paint.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = match.qualityDescription,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        match.isExcellentMatch -> Color(0xFF4CAF50)
                        match.isGoodMatch -> Color(0xFF2196F3)
                        else -> Color(0xFFFF9800)
                    }
                )
            }

            // Confidence score - prominent on the right
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "%.0f%%".format(match.confidence * 100),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        match.isExcellentMatch -> Color(0xFF4CAF50)
                        match.isGoodMatch -> Color(0xFF2196F3)
                        else -> Color(0xFFFF9800)
                    }
                )
                Text(
                    text = "ΔE %.2f".format(match.distance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MixResultsView(
    state: ConverterUiState,
    onRecipeSelected: (io.brushforge.brushforge.domain.model.PaintMixRecipe) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.mixRecipes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No mix recipes found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.mixRecipes) { recipe ->
                MixRecipeItem(
                    recipe = recipe,
                    onClick = { onRecipeSelected(recipe) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MixRecipeItem(
    recipe: io.brushforge.brushforge.domain.model.PaintMixRecipe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = recipe.componentDescription,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Confidence: %.1f%%".format(recipe.confidence * 100),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            recipe.components.forEach { component ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(component.paint.hex.toColorInt()))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(
                        text = component.paint.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = component.percentageFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailView(state: ConverterUiState) {
    val match = state.selectedMatch ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Match Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Matched Paint", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(match.paint.hex.toColorInt()))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text(match.paint.name, fontWeight = FontWeight.Bold)
                            Text(match.paint.brand)
                            Text("Type: ${match.paint.type.rawValue}")
                            Text("Finish: ${match.paint.finish.rawValue}")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Match Quality", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Quality: ${match.qualityDescription}")
                    Text("Distance: %.2f".format(match.distance))
                    Text("Confidence: %.1f%%".format(match.confidence * 100))
                    Text("Algorithm: ${match.algorithm.displayName}")
                }
            }
        }
    }
}

@Composable
private fun MixDetailView(state: ConverterUiState) {
    val recipe = state.selectedRecipe ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Mix Recipe",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Components", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    recipe.components.forEach { component ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(component.paint.hex.toColorInt()))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(component.paint.name, fontWeight = FontWeight.Bold)
                                Text(component.paint.brand, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = component.percentageFormatted,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mix Quality", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Confidence: %.1f%%".format(recipe.confidence * 100))
                    Text("Practical: ${if (recipe.isPractical) "Yes" else "No (contains very small amounts)"}")
                    Text("Result Color: ${recipe.resultingHex}")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(recipe.resultingHex.toColorInt()))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterSheet(
    state: ConverterUiState,
    onBrandFilterChanged: (String?) -> Unit,
    onTypeFilterChanged: (PaintType?) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Filters", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClose) {
                Text("Done")
            }
        }

        HorizontalDivider()

        // Brand Filter
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Brand", style = MaterialTheme.typography.titleSmall)
            if (state.availableBrands.isEmpty()) {
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
                    FilterChip(
                        selected = state.searchBrandFilter == null,
                        onClick = { onBrandFilterChanged(null) },
                        label = { Text("All Brands") }
                    )
                    state.availableBrands.forEach { brand ->
                        FilterChip(
                            selected = state.searchBrandFilter == brand,
                            onClick = { onBrandFilterChanged(brand) },
                            label = { Text(brand) }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        // Type Filter
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Paint Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.searchTypeFilter == null,
                    onClick = { onTypeFilterChanged(null) },
                    label = { Text("All Types") }
                )
                PaintType.entries
                    .filter { it != PaintType.Unknown }
                    .forEach { type ->
                        FilterChip(
                            selected = state.searchTypeFilter == type,
                            onClick = { onTypeFilterChanged(type) },
                            label = { Text(type.rawValue) }
                        )
                    }
            }
        }

        HorizontalDivider()

        // Clear all button
        if (state.activeFilterCount > 0) {
            TextButton(onClick = onClearAll) {
                Text("Clear all filters")
            }
        }
    }
}
