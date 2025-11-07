package io.brushforge.brushforge.feature.palettes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import io.brushforge.brushforge.domain.model.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalettesScreen(
    viewModel: PalettesViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    // Handle navigation to detail
    state.selectedRecipeId?.let { recipeId ->
        onNavigateToDetail(recipeId)
        viewModel.onBackFromDetail()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Paint Recipes") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onCreateNewRecipe
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Recipe")
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.recipes.isEmpty() -> {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onCreateRecipe = viewModel::onCreateNewRecipe
                )
            }
            else -> {
                RecipesList(
                    recipes = state.recipes,
                    ownedPaintIds = state.ownedPaintStableIds,
                    onRecipeClick = viewModel::onRecipeSelected,
                    onToggleFavorite = viewModel::onToggleFavorite,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    if (state.showCreateDialog) {
        CreateRecipeDialog(
            recipeName = state.newRecipeName,
            onNameChange = viewModel::onNewRecipeNameChanged,
            onDismiss = viewModel::onDismissCreateDialog,
            onConfirm = viewModel::onConfirmCreateRecipe
        )
    }
}

@Composable
private fun RecipesList(
    recipes: List<Recipe>,
    ownedPaintIds: Set<String>,
    onRecipeClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recipes, key = { it.id }) { recipe ->
            RecipeCard(
                recipe = recipe,
                ownedPaintIds = ownedPaintIds,
                onClick = { onRecipeClick(recipe.id) },
                onToggleFavorite = { onToggleFavorite(recipe.id) }
            )
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    ownedPaintIds: Set<String>,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val ownedCount = recipe.getOwnedStepCount(ownedPaintIds)
    val hasAllPaints = recipe.hasAllPaints(ownedPaintIds)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with favorite button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (recipe.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (recipe.isFavorite) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Paint color swatches (first 6 steps)
            if (recipe.steps.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    recipe.steps.take(6).forEach { step ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(step.paintHex.toColorInt()))
                        )
                    }
                    // Fill remaining space if less than 6 steps
                    repeat((6 - recipe.steps.take(6).size).coerceAtLeast(0)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step count and ownership info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${recipe.stepCount} ${if (recipe.stepCount == 1) "step" else "steps"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (recipe.stepCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasAllPaints) Color(0xFF4CAF50)
                                    else if (ownedCount > 0) Color(0xFFFF9800)
                                    else Color(0xFFF44336)
                                )
                        )
                        Text(
                            text = "$ownedCount/${recipe.stepCount} owned",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasAllPaints) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (hasAllPaints) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "\uD83C\uDFA8",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "No Recipes Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create your first paint recipe to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FloatingActionButton(onClick = onCreateRecipe) {
                Icon(Icons.Default.Add, contentDescription = "Create Recipe")
            }
        }
    }
}

@Composable
private fun CreateRecipeDialog(
    recipeName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Recipe") },
        text = {
            Column {
                Text(
                    text = "Enter a name for your recipe",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = recipeName,
                    onValueChange = onNameChange,
                    label = { Text("Recipe Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = recipeName.trim().isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
