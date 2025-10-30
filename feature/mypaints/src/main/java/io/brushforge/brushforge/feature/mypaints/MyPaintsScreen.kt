package io.brushforge.brushforge.feature.mypaints

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPaintsScreen(
    modifier: Modifier = Modifier,
    viewModel: MyPaintsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "My Paints") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: open add paint sheet */ }) {
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        InventorySummary(
            inventoryCount = state.inventoryCount,
            inventoryLimit = state.inventoryLimit,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.userItems.isNotEmpty()) {
                item { SectionHeader(title = "Your Paints", icon = Icons.Filled.ColorLens) }
                items(state.userItems, key = { it.stableId }) { item ->
                    PaintCard(
                        item = item,
                        onToggleOwned = onToggleOwned,
                        onToggleWishlist = onToggleWishlist,
                        onToggleAlmostEmpty = onToggleAlmostEmpty
                    )
                }
            }
            if (state.catalogItems.isNotEmpty()) {
                item { SectionHeader(title = "Catalog", icon = Icons.Filled.Collections) }
                items(state.catalogItems, key = { it.stableId }) { item ->
                    PaintCard(
                        item = item,
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
    onToggleOwned: (String, Boolean) -> Unit,
    onToggleWishlist: (String, Boolean) -> Unit,
    onToggleAlmostEmpty: (String, Boolean) -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorSwatch(hex = item.hex)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.typeLabel} • ${item.finishLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.isUserPaint) {
                    val flag = when {
                        item.isCustom -> "Custom"
                        item.isMixed -> "Mixed"
                        else -> "User"
                    }
                    Text(
                        text = flag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconToggleButton(
                    checked = item.isOwned,
                    onCheckedChange = { onToggleOwned(item.stableId, it) }
                ) {
                    Icon(
                        imageVector = if (item.isOwned) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = "Owned"
                    )
                }
                IconToggleButton(
                    checked = item.isWishlist,
                    onCheckedChange = { onToggleWishlist(item.stableId, it) }
                ) {
                    Icon(
                        imageVector = if (item.isWishlist) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist"
                    )
                }
                IconToggleButton(
                    checked = item.isAlmostEmpty,
                    onCheckedChange = { onToggleAlmostEmpty(item.stableId, it) },
                    enabled = item.isOwned
                ) {
                    Icon(
                        imageVector = Icons.Filled.Inventory,
                        contentDescription = "Almost empty",
                        tint = if (item.isAlmostEmpty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
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
            .size(48.dp)
            .background(color = color, shape = MaterialTheme.shapes.small)
    )
}
