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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.graphics.toColorInt
import io.brushforge.brushforge.domain.model.CatalogPaint
import io.brushforge.brushforge.domain.model.PaintType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    modifier: Modifier = Modifier,
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load paint from stableId if provided via navigation
    LaunchedEffect(Unit) {
        viewModel.loadPaintFromNavigationIfNeeded()
    }

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
                onOwnedFilterChanged = viewModel::onSearchOwnedFilterChanged,
                onClearAll = viewModel::onClearSearchFilters,
                onClose = viewModel::onCloseFilterSheet
            )
        }
    }

    if (state.showInfoDialog) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onCloseInfoDialog() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            MatchingInfoSheet(onDismiss = { viewModel.onCloseInfoDialog() })
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
                actions = {
                    IconButton(onClick = { viewModel.onOpenInfoDialog() }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "How matching works"
                        )
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
                    onFindMixRecipes = viewModel::onFindMixRecipes,
                    onSearchColorFilterToggle = viewModel::onSearchColorFilterToggle
                )
                ConverterView.Results -> ResultsView(
                    state = state,
                    onMatchSelected = viewModel::onMatchSelected,
                    onSortOptionChanged = viewModel::onSortOptionChanged,
                    onToggleOwnedOnly = viewModel::onToggleOwnedOnly
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
    onFindMixRecipes: () -> Unit,
    onSearchColorFilterToggle: (io.brushforge.brushforge.domain.util.ColorFamily) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed Search Bar at Top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            // Color filter chips
            ColorFamilyFilterRow(
                selectedFamilies = state.searchColorFilter,
                onColorFamilyToggle = onSearchColorFilterToggle
            )
        }

        // Scrollable Content Below
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        // Search Results
        if (state.showSearchResults && state.searchResults.isNotEmpty()) {
            // Show subtitle only if no search query (initial state)
            if (state.searchQuery.isEmpty() && state.selectedSourcePaint == null) {
                item {
                    Text(
                        text = "Tap any paint to find similar alternatives",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(
                items = state.searchResults,
                key = { it.stableId }
            ) { paint ->
                PaintSearchResultItem(
                    paint = paint,
                    onClick = { onSelectSourcePaint(paint) }
                )
            }
        }

        if (state.selectedSourcePaint != null) {
            // Brand Selection (no title needed)
            item {
                Spacer(modifier = Modifier.height(8.dp))
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
        } // End LazyColumn
    } // End Column
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
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Larger, more prominent color swatch
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(paint.hex.toColorInt()))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paint.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${paint.brand}${if (!paint.line.isNullOrBlank()) " • ${paint.line}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ResultsView(
    state: ConverterUiState,
    onMatchSelected: (io.brushforge.brushforge.domain.model.PaintMatch) -> Unit,
    onSortOptionChanged: (MatchSortOption) -> Unit,
    onToggleOwnedOnly: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter/Sort Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sort section
                Column {
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

                HorizontalDivider()

                // Filter section
                Column {
                    Text("Filter:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.showOwnedOnly,
                            onClick = onToggleOwnedOnly,
                            label = { Text("Owned only") }
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
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Finding similar paints...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ranking candidates by ΔE and confidence",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (state.filteredMatches.isEmpty()) {
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
    val loadingSteps = remember {
        listOf(
            "Preparing candidate paints",
            "Testing 2-paint combinations",
            "Exploring 3-paint combinations",
            "Scoring mixes for Delta E accuracy",
            "Selecting the most practical recipes"
        )
    }
    var loadingMessageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            loadingMessageIndex = 0
            while (isActive) {
                delay(1500)
                loadingMessageIndex = (loadingMessageIndex + 1) % loadingSteps.size
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.selectedSourcePaint?.let { targetPaint ->
            item {
                TargetMixHeader(paint = targetPaint)
            }
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Finding mix recipes...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = loadingSteps[loadingMessageIndex % loadingSteps.size],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (state.mixRecipes.isEmpty()) {
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

@Composable
private fun TargetMixHeader(paint: CatalogPaint) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(paint.hex.toColorInt()))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Target paint",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = paint.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${paint.brand}${paint.line?.let { " · $it" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailView(state: ConverterUiState) {
    val match = state.selectedMatch ?: return
    val sourcePaint = state.selectedSourcePaint ?: return

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Analysis")

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card - Visual Comparison
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Three-column layout: Source | Quality | Match
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Source column
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "SOURCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(sourcePaint.hex.toColorInt()))
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                )
                                Text(
                                    text = sourcePaint.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = sourcePaint.brand,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                sourcePaint.code?.let { code ->
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Comparison indicator column
                            Column(
                                modifier = Modifier.weight(0.4f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Comparison",
                                    modifier = Modifier.size(32.dp),
                                    tint = when {
                                        match.isExcellentMatch -> Color(0xFF4CAF50)
                                        match.isGoodMatch -> Color(0xFF2196F3)
                                        else -> Color(0xFFFF9800)
                                    }
                                )
                            }

                            // Match column
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "MATCH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(match.paint.hex.toColorInt()))
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                )
                                Text(
                                    text = match.paint.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = match.paint.brand,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                match.paint.code?.let { code ->
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Color comparison strip with labels
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Direct Comparison",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Quality badge
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            match.isExcellentMatch -> Color(0xFF4CAF50)
                                            match.isGoodMatch -> Color(0xFF2196F3)
                                            else -> Color(0xFFFF9800)
                                        }.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(
                                        text = match.qualityDescription,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            match.isExcellentMatch -> Color(0xFF4CAF50)
                                            match.isGoodMatch -> Color(0xFF2196F3)
                                            else -> Color(0xFFFF9800)
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color(sourcePaint.hex.toColorInt()))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surface)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color(match.paint.hex.toColorInt()))
                                )
                            }
                        }

                        // Quick info badges
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Type comparison
                            DetailBadge(
                                text = if (sourcePaint.type == match.paint.type) {
                                    "Same type"
                                } else {
                                    "Type: ${sourcePaint.type.rawValue} → ${match.paint.type.rawValue}"
                                },
                                color = if (sourcePaint.type == match.paint.type) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )

                            // Finish comparison
                            DetailBadge(
                                text = if (sourcePaint.finish == match.paint.finish) {
                                    "Same finish"
                                } else {
                                    "Finish: ${sourcePaint.finish.rawValue} → ${match.paint.finish.rawValue}"
                                },
                                color = if (sourcePaint.finish == match.paint.finish) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        }

                        // Confidence and Delta E
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Confidence pill
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        match.isExcellentMatch -> Color(0xFF4CAF50)
                                        match.isGoodMatch -> Color(0xFF2196F3)
                                        else -> Color(0xFFFF9800)
                                    }.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "%.0f%%".format(match.confidence * 100),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            match.isExcellentMatch -> Color(0xFF4CAF50)
                                            match.isGoodMatch -> Color(0xFF2196F3)
                                            else -> Color(0xFFFF9800)
                                        }
                                    )
                                    Text(
                                        text = "Confidence",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Delta E
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "ΔE %.2f".format(match.distance),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Color Distance",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab selector
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
                            .padding(4.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            FilterChip(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                label = { Text(title) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Tab content
            when (selectedTab) {
                0 -> {
                    // Overview Tab
                    item {
                        DetailOverviewTab(sourcePaint, match)
                    }
                }
                1 -> {
                    // Analysis Tab
                    item {
                        DetailAnalysisTab(sourcePaint, match)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBadge(text: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Returns a triple of (icon, color, tip text) based on confidence level.
 * Tips borrow from widely shared, research-backed hobby guidance:
 * - Age of Miniatures - "10 miniature painting tips (stuff I learned the hard way)", updated 2021-02-02.
 * - wikiHow - "How to Paint Warhammer Figures", rev. 2024-08-18.
 */
private fun getMiniaturePaintingTip(
    confidence: Double,
    sourcePaint: CatalogPaint,
    matchPaint: CatalogPaint
): Triple<String, Color, String> {
    // Use hashCode for consistent but varied selection within same confidence range
    val variationSeed = (sourcePaint.name + matchPaint.name).hashCode()

    return when {
        // 90%+ - Virtually identical, professional level match
        confidence >= 0.90 -> {
            val tips = listOf(
                Triple("✨", Color(0xFF4CAF50), "Perfect twin. Keep following the two-thin-coats rule so micro detail stays sharp while you swap brands mid-project."),
                Triple("🎯", Color(0xFF4CAF50), "Drop-in substitute. Use the time you saved to push basing or faces—the areas viewers notice first on finished armies."),
                Triple("⭐", Color(0xFF4CAF50), "Match is so tight that you can freehand, glaze, or wet blend without recalculating mixes. Let each layer cure fully before oil or enamel steps."),
                Triple("🧴", Color(0xFF4CAF50), "Because coverage is identical, concentrate on brush care and clean water/mediums so the finish stays consistent across the project."),
                Triple("🖌️", Color(0xFF4CAF50), "Use a larger brush for fast basecoats, then drop to a detail tip for highlights—the hue will stay locked in no matter the brushwork.")
            )
            tips[variationSeed.mod(tips.size)]
        }

        // 85-89% - Excellent match, suitable for all practical purposes
        confidence >= 0.85 -> {
            val tips = listOf(
                Triple("💎", Color(0xFF4CAF50), "Close enough for uninterrupted layering. Keep a wet palette handy so blends stay consistent if you need to feather edges."),
                Triple("✅", Color(0xFF4CAF50), "Excellent stand-in. Spray or zenithal prime with the same color family to hide the minor variance before you start glazing."),
                Triple("🎨", Color(0xFF4CAF50), "Great for army batches. Test on a spare shoulder pad, then lock it in with a unifying wash to erase any remaining shift."),
                Triple("🧪", Color(0xFF4CAF50), "If you are touching up older work, glaze both sections with a thin medium mix to marry the sheen."),
                Triple("🏁", Color(0xFF4CAF50), "Keep your brush tip conditioned—good tools make these near matches indistinguishable even under bright display lights.")
            )
            tips[variationSeed.mod(tips.size)]
        }

        // 75-84% - Good match with minor considerations
        confidence >= 0.75 -> {
            val tips = listOf(
                Triple("👍", Color(0xFF2196F3), "Good alternative. Spray a neutral/white primer if you need vibrant reds or yellows—lighter primers make close colors settle faster."),
                Triple("🔍", Color(0xFF2196F3), "Lay down a thin test stripe on poster-tack mounted spare bits so you can rotate them under different lights before committing."),
                Triple("⚡", Color(0xFF2196F3), "Blend the join with two or three translucent glazes (1:1 paint to medium) to nudge saturation without repainting whole panels."),
                Triple("🪄", Color(0xFF2196F3), "Push color theory: if the swap feels cooler/warmer, mix a drop of its complement to neutralize it instead of overpainting."),
                Triple("🧊", Color(0xFF2196F3), "Keep paint on a lidded wet palette overnight—age-of-miniatures style—to avoid remixing and drifting away from the original tone.")
            )
            tips[variationSeed.mod(tips.size)]
        }

        // 65-74% - Noticeable difference, requires technique adjustments
        confidence >= 0.65 -> {
            val tips = listOf(
                Triple("⚠️", Color(0xFFFF9800), "Noticeable shift. Zenithal prime or pre-shade to steer value before you apply this paint, then glaze toward the target hue."),
                Triple("🎭", Color(0xFFFF9800), "Use it for underpainting or modulation, then layer a thin filter of the original color to tie everything back together."),
                Triple("🔧", Color(0xFFFF9800), "Mix in tiny amounts of neutral grey/skin tones to desaturate, rather than piling on black or white which can chalk the finish."),
                Triple("🧷", Color(0xFFFF9800), "Mount minis on cork or use sticky tack handles while testing—easy handling keeps experimental blends smooth."),
                Triple("🌗", Color(0xFFFF9800), "If you must match legacy work, feather the transition with wet blending or stippling instead of a hard edge.")
            )
            tips[variationSeed.mod(tips.size)]
        }

        // 50-64% - Significant difference, use with caution
        confidence >= 0.50 -> {
            val tips = listOf(
                Triple("❗", Color(0xFFFF5722), "Major shift—treat it as a new scheme. Block in fresh armor panels and tie things together later with the same weathering powders or oils."),
                Triple("🚫", Color(0xFFFF5722), "Not a substitute for repairs, but great for spot colors (plasma, lenses, cloth) if you keep the rest of the palette consistent."),
                Triple("💡", Color(0xFFFF5722), "Consider using it on bases or terrain where variation sells realism rather than clashes with previous coats."),
                Triple("🧱", Color(0xFFFF5722), "If you really need the old hue, mix custom colors with measured drops and note the ratios in your project log."),
                Triple("🧼", Color(0xFFFF5722), "Prime a spare sprue tree and make swatches of both paints. Clear-coat them so you can compare drying shifts before reworking a model.")
            )
            tips[variationSeed.mod(tips.size)]
        }

        // Below 50% - Poor match, different color territory
        else -> {
            val tips = listOf(
                Triple("⛔", Color(0xFFF44336), "Fundamentally different color. Use it for narrative elements (OSL, freehand, hazard stripes) but hunt for another match before committing."),
                Triple("❌", Color(0xFFF44336), "If you love the tone, build a fresh recipe around it: re-plan primers, shades, and highlights so the model looks intentional."),
                Triple("🔴", Color(0xFFF44336), "Great candidate for basing powders, diorama elements, or experiment pieces—just not a substitute for the listed paint."),
                Triple("📓", Color(0xFFF44336), "Log this mismatch in your paint journal so you remember it’s in a totally different color family when you revisit the project.")
            )
            tips[variationSeed.mod(tips.size)]
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailOverviewTab(
    sourcePaint: CatalogPaint,
    match: io.brushforge.brushforge.domain.model.PaintMatch
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Quick Facts section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Facts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Generate contextual badges
                    if (match.distance < 5.0) {
                        DetailBadge("Excellent color match", Color(0xFF4CAF50))
                    }
                    if (sourcePaint.type == match.paint.type) {
                        DetailBadge("Same coverage type", Color(0xFF4CAF50))
                    }
                    if (sourcePaint.finish == match.paint.finish) {
                        DetailBadge("Matching finish", Color(0xFF4CAF50))
                    }
                    if (match.confidence >= 0.85) {
                        DetailBadge("High confidence", Color(0xFF2196F3))
                    }
                }
            }
        }

        // Summary section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Type comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paint Type",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (sourcePaint.type == match.paint.type) {
                            "Match"
                        } else {
                            "${sourcePaint.type.rawValue} → ${match.paint.type.rawValue}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (sourcePaint.type == match.paint.type) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }

                HorizontalDivider()

                // Finish comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Finish",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (sourcePaint.finish == match.paint.finish) {
                            "Match"
                        } else {
                            "${sourcePaint.finish.rawValue} → ${match.paint.finish.rawValue}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (sourcePaint.finish == match.paint.finish) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
        }

        // Pro Tip section
        val (tipIcon, tipColor, tipText) = getMiniaturePaintingTip(match.confidence, sourcePaint, match.paint)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = tipColor.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$tipIcon Pro Tip",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tipColor
                )
                Text(
                    text = tipText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DetailAnalysisTab(
    sourcePaint: CatalogPaint,
    match: io.brushforge.brushforge.domain.model.PaintMatch
) {
    val sourceLab = sourcePaint.labColor
    val matchLab = match.paint.labColor

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Color Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Lightness comparison with bar
                AnalysisBarItem(
                    icon = "☀",
                    iconColor = Color(0xFFFFEB3B),
                    label = "Lightness",
                    sourceValue = sourceLab.l,
                    matchValue = matchLab.l,
                    minValue = 0.0,
                    maxValue = 100.0,
                    getLabelForValue = { value ->
                        when {
                            value < 25 -> "Darker"
                            value > 75 -> "Lighter"
                            else -> ""
                        }
                    },
                    getComparisonText = { source, match ->
                        val diff = match - source
                        when {
                            kotlin.math.abs(diff) < 5 -> "Similar lightness"
                            diff > 0 -> "Slightly Lighter"
                            else -> "Slightly Darker"
                        }
                    }
                )

                HorizontalDivider()

                // Chroma/Saturation comparison with bar
                val sourceChroma = kotlin.math.sqrt(sourceLab.a * sourceLab.a + sourceLab.b * sourceLab.b)
                val matchChroma = kotlin.math.sqrt(matchLab.a * matchLab.a + matchLab.b * matchLab.b)

                AnalysisBarItem(
                    icon = "💧",
                    iconColor = Color(0xFFE91E63),
                    label = "Chroma",
                    sourceValue = sourceChroma,
                    matchValue = matchChroma,
                    minValue = 0.0,
                    maxValue = 100.0,
                    getLabelForValue = { value ->
                        when {
                            value < 20 -> "Less saturated"
                            value > 60 -> "More saturated"
                            else -> ""
                        }
                    },
                    getComparisonText = { source, match ->
                        val diff = match - source
                        when {
                            kotlin.math.abs(diff) < 5 -> "Similar saturation"
                            diff > 0 -> "Noticeably More saturated"
                            else -> "Noticeably Less saturated"
                        }
                    }
                )

                HorizontalDivider()

                // Hue comparison with bar (using a* and b* to calculate hue angle)
                val sourceHue = kotlin.math.atan2(sourceLab.b, sourceLab.a) * 180 / kotlin.math.PI
                val matchHue = kotlin.math.atan2(matchLab.b, matchLab.a) * 180 / kotlin.math.PI
                val normalizedSourceHue = if (sourceHue < 0) sourceHue + 360 else sourceHue
                val normalizedMatchHue = if (matchHue < 0) matchHue + 360 else matchHue

                AnalysisBarItem(
                    icon = "🎨",
                    iconColor = Color(0xFF2196F3),
                    label = "Hue",
                    sourceValue = normalizedSourceHue,
                    matchValue = normalizedMatchHue,
                    minValue = 0.0,
                    maxValue = 360.0,
                    getLabelForValue = { value ->
                        when {
                            value < 30 -> "Red"
                            value < 60 -> "Orange"
                            value < 120 -> "Yellow"
                            value < 180 -> "Green"
                            value < 240 -> "Cyan"
                            value < 300 -> "Blue"
                            value < 330 -> "Purple"
                            else -> "Red"
                        }
                    },
                    getComparisonText = { source, match ->
                        // Calculate shortest distance around the hue circle
                        val rawDiff = match - source
                        val normalizedDiff = when {
                            rawDiff > 180 -> rawDiff - 360
                            rawDiff < -180 -> rawDiff + 360
                            else -> rawDiff
                        }

                        val absDiff = kotlin.math.abs(normalizedDiff)
                        val direction = when {
                            absDiff < 5 -> return@AnalysisBarItem "Same hue"
                            normalizedDiff > 0 -> {
                                // Moving clockwise on color wheel
                                when {
                                    match < 30 || match >= 330 -> "toward Red"
                                    match < 60 -> "toward Orange"
                                    match < 120 -> "toward Yellow"
                                    match < 180 -> "toward Green"
                                    match < 240 -> "toward Cyan"
                                    match < 300 -> "toward Blue"
                                    else -> "toward Purple"
                                }
                            }
                            else -> {
                                // Moving counter-clockwise on color wheel
                                when {
                                    match < 30 || match >= 330 -> "toward Red"
                                    match < 60 -> "toward Orange"
                                    match < 120 -> "toward Yellow"
                                    match < 180 -> "toward Green"
                                    match < 240 -> "toward Cyan"
                                    match < 300 -> "toward Blue"
                                    else -> "toward Purple"
                                }
                            }
                        }

                        when {
                            absDiff < 15 -> "Slightly shifts $direction"
                            else -> "Noticeably shifts $direction"
                        }
                    }
                )
            }
        }

        // Delta E and Confidence scores
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delta E Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ΔE Value",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "%.2f".format(match.distance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            match.distance < 3.0 -> Color(0xFF4CAF50)
                            match.distance < 6.0 -> Color(0xFF2196F3)
                            else -> Color(0xFFFF9800)
                        }
                    )
                }

                HorizontalDivider()

                // Confidence Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confidence Score",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "%.0f%%".format(match.confidence * 100),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            match.confidence >= 0.85 -> Color(0xFF4CAF50)
                            match.confidence >= 0.65 -> Color(0xFF2196F3)
                            else -> Color(0xFFFF9800)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisBarItem(
    icon: String,
    iconColor: Color,
    label: String,
    sourceValue: Double,
    matchValue: Double,
    minValue: Double,
    maxValue: Double,
    getLabelForValue: (Double) -> String,
    getComparisonText: (Double, Double) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header with icon and label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = getComparisonText(sourceValue, matchValue),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    kotlin.math.abs(matchValue - sourceValue) < 5 -> Color(0xFF4CAF50)
                    kotlin.math.abs(matchValue - sourceValue) < 15 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )
        }

        // Bar visualization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            // Source marker (triangle at top)
            val sourcePosition = ((sourceValue - minValue) / (maxValue - minValue)).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(sourcePosition)
                    .align(Alignment.CenterStart)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Match marker (triangle at bottom)
            val matchPosition = ((matchValue - minValue) / (maxValue - minValue)).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(matchPosition)
                    .align(Alignment.CenterStart)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(iconColor)
                    )
                    Text(
                        text = "▲",
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor
                    )
                }
            }

            // Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = getLabelForValue(minValue + 25),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getLabelForValue(maxValue - 25),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Value indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Source: %.1f".format(sourceValue),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Match: %.1f".format(matchValue),
                style = MaterialTheme.typography.labelSmall,
                color = iconColor
            )
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

@Composable
private fun MatchingInfoSheet(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Header (fixed, not scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Color Difference (ΔE)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onDismiss) {
                Text("Done", color = MaterialTheme.colorScheme.primary)
            }
        }

        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // What is Color Difference section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "What is Color Difference (ΔE)?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ΔE (Delta E) measures how different two colors are to the human eye. The smaller the number, the closer the match.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Delta E ranges with icons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeltaERangeItem(
                        icon = "✓",
                        iconColor = Color(0xFF4CAF50),
                        range = "0 – Identical",
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                    DeltaERangeItem(
                        icon = "✓",
                        iconColor = Color(0xFF4CAF50),
                        range = "1–2 – Nearly identical",
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                    DeltaERangeItem(
                        icon = "👍",
                        iconColor = Color(0xFF4CAF50),
                        range = "2–5 – Close match",
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                    DeltaERangeItem(
                        icon = "⚠",
                        iconColor = Color(0xFFFF9800),
                        range = "6–10 – Noticeable difference",
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                    DeltaERangeItem(
                        icon = "✕",
                        iconColor = Color(0xFFF44336),
                        range = "10+ – Different color",
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Tips section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tips",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Aim for ΔE < 5 for solid matches when possible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• AI matches may emphasize how the paint behaves (coverage, hue bias, use case) in addition to color.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DeltaERangeItem(
    icon: String,
    iconColor: Color,
    range: String,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(iconColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelMedium,
                color = iconColor,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = range,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterSheet(
    state: ConverterUiState,
    onBrandFilterChanged: (String?) -> Unit,
    onTypeFilterChanged: (PaintType?) -> Unit,
    onOwnedFilterChanged: (SearchOwnedFilter) -> Unit,
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

        // Owned/Wishlist Filter
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Collection", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchOwnedFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.searchOwnedFilter == filter,
                        onClick = { onOwnedFilterChanged(filter) },
                        label = { Text(filter.displayName) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorFamilyFilterRow(
    selectedFamilies: Set<io.brushforge.brushforge.domain.util.ColorFamily>,
    onColorFamilyToggle: (io.brushforge.brushforge.domain.util.ColorFamily) -> Unit
) {
    val availableFamilies = listOf(
        io.brushforge.brushforge.domain.util.ColorFamily.Red,
        io.brushforge.brushforge.domain.util.ColorFamily.Orange,
        io.brushforge.brushforge.domain.util.ColorFamily.Yellow,
        io.brushforge.brushforge.domain.util.ColorFamily.Green,
        io.brushforge.brushforge.domain.util.ColorFamily.Blue,
        io.brushforge.brushforge.domain.util.ColorFamily.Purple,
        io.brushforge.brushforge.domain.util.ColorFamily.Pink,
        io.brushforge.brushforge.domain.util.ColorFamily.Brown,
        io.brushforge.brushforge.domain.util.ColorFamily.Grey,
        io.brushforge.brushforge.domain.util.ColorFamily.Black,
        io.brushforge.brushforge.domain.util.ColorFamily.White
    )

    val haptic = LocalHapticFeedback.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

private fun colorFamilyLabel(family: io.brushforge.brushforge.domain.util.ColorFamily): String = when (family) {
    io.brushforge.brushforge.domain.util.ColorFamily.Red -> "Red"
    io.brushforge.brushforge.domain.util.ColorFamily.Orange -> "Orange"
    io.brushforge.brushforge.domain.util.ColorFamily.Yellow -> "Yellow"
    io.brushforge.brushforge.domain.util.ColorFamily.Green -> "Green"
    io.brushforge.brushforge.domain.util.ColorFamily.Blue -> "Blue"
    io.brushforge.brushforge.domain.util.ColorFamily.Purple -> "Purple"
    io.brushforge.brushforge.domain.util.ColorFamily.Pink -> "Pink"
    io.brushforge.brushforge.domain.util.ColorFamily.Brown -> "Brown"
    io.brushforge.brushforge.domain.util.ColorFamily.Grey -> "Grey"
    io.brushforge.brushforge.domain.util.ColorFamily.Black -> "Black"
    io.brushforge.brushforge.domain.util.ColorFamily.White -> "White"
}

private fun colorFamilyColor(family: io.brushforge.brushforge.domain.util.ColorFamily): Color = when (family) {
    io.brushforge.brushforge.domain.util.ColorFamily.Red -> Color(0xFFEF5350)
    io.brushforge.brushforge.domain.util.ColorFamily.Orange -> Color(0xFFFFA726)
    io.brushforge.brushforge.domain.util.ColorFamily.Yellow -> Color(0xFFFFEB3B)
    io.brushforge.brushforge.domain.util.ColorFamily.Green -> Color(0xFF66BB6A)
    io.brushforge.brushforge.domain.util.ColorFamily.Blue -> Color(0xFF42A5F5)
    io.brushforge.brushforge.domain.util.ColorFamily.Purple -> Color(0xFFAB47BC)
    io.brushforge.brushforge.domain.util.ColorFamily.Pink -> Color(0xFFF06292)
    io.brushforge.brushforge.domain.util.ColorFamily.Brown -> Color(0xFF8D6E63)
    io.brushforge.brushforge.domain.util.ColorFamily.Grey -> Color(0xFFB0BEC5)
    io.brushforge.brushforge.domain.util.ColorFamily.Black -> Color(0xFF424242)
    io.brushforge.brushforge.domain.util.ColorFamily.White -> Color(0xFFFFFFFF)
}
