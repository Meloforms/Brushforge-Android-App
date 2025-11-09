@file:OptIn(ExperimentalMaterial3Api::class)

package io.brushforge.brushforge.feature.mypaints

import android.graphics.Color.parseColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import io.brushforge.brushforge.core.ui.components.ToggleFilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.brushforge.brushforge.domain.model.ColorRecommendations
import io.brushforge.brushforge.domain.model.PaintType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun PaintDetailScreen(
    onNavigateUp: () -> Unit,
    onShowPaint: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaintDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PaintDetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
                state.paint == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.errorMessage ?: "Paint not found.")
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onNavigateUp) {
                                Text("Go back")
                            }
                        }
                    }
                }
                else -> {
                    PaintDetailContent(
                        state = state,
                        onOwnedToggle = viewModel::onOwnedToggled,
                        onWishlistToggle = viewModel::onWishlistToggled,
                        onAlmostEmptyToggle = viewModel::onAlmostEmptyToggled,
                        onNotesChanged = viewModel::onNotesChanged,
                        onSaveNotes = viewModel::onSaveNotes,
                        onSimilarPaintSelected = onShowPaint,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            // Floating back button
            IconButton(
                onClick = onNavigateUp,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private val TWO_PI = (2 * PI).toFloat()
private fun String?.cleanLabel(): String? {
    return this
        ?.takeIf { it.isNotBlank() }
        ?.takeUnless { it.equals("null", ignoreCase = true) }
}

@Composable
private fun PaintDetailTopBar(
    title: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}

@Composable
private fun PaintDetailContent(
    state: PaintDetailUiState,
    onOwnedToggle: (Boolean) -> Unit,
    onWishlistToggle: (Boolean) -> Unit,
    onAlmostEmptyToggle: (Boolean) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSaveNotes: () -> Unit,
    onSimilarPaintSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val paint = state.paint ?: return
    var selectedTabIndex by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    // Reset scroll position when paint changes
    LaunchedEffect(paint.stableId) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Paint header with animation
        PaintHeaderCard(paint = paint)

        // Status card
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            StatusCard(
                state = state,
                onOwnedToggle = onOwnedToggle,
                onWishlistToggle = onWishlistToggle,
                onAlmostEmptyToggle = onAlmostEmptyToggle
            )
        }

        // Tab row
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Info") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Color") }
            )
        }

        // Tab content
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // Info tab
                    PaintInfoCard(paint = paint)
                    NotesCard(
                        notes = state.notesInput,
                        canSave = state.canSaveNotes && !state.isSavingNotes,
                        isSaving = state.isSavingNotes,
                        onNotesChanged = onNotesChanged,
                        onSaveNotes = onSaveNotes
                    )
                    SimilarPaintsSection(
                        paints = state.similarPaints,
                        onSimilarPaintSelected = onSimilarPaintSelected
                    )
                }
                1 -> {
                    // Color tab
                    ColorRecommendationsSection(
                        colorRecommendations = state.colorRecommendations,
                        onPaintSelected = onSimilarPaintSelected
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CompactPaintHeader(
    paint: PaintDetailItem,
    state: PaintDetailUiState,
    onOwnedToggle: (Boolean) -> Unit,
    onWishlistToggle: (Boolean) -> Unit,
    onAlmostEmptyToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(top = 40.dp),  // Space for back button
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact color swatch
            val fallbackColor = MaterialTheme.colorScheme.primary
            val color = remember(paint.hex) {
                runCatching { Color(parseColor(paint.hex)) }
                    .getOrElse { fallbackColor }
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            // Paint info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = paint.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = paint.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Compact status toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleFilterChip(
                label = "Owned",
                selected = state.isOwned,
                onClick = { onOwnedToggle(!state.isOwned) },
                selectedIcon = Icons.Filled.CheckCircle,
                unselectedIcon = Icons.Outlined.CheckCircle
            )
            ToggleFilterChip(
                label = "Wishlist",
                selected = state.isWishlist,
                onClick = { onWishlistToggle(!state.isWishlist) },
                selectedIcon = Icons.Filled.Favorite,
                unselectedIcon = Icons.Outlined.FavoriteBorder
            )
            if (state.isOwned) {
                ToggleFilterChip(
                    label = "Low",
                    selected = state.isAlmostEmpty,
                    onClick = { onAlmostEmptyToggle(!state.isAlmostEmpty) },
                    selectedIcon = Icons.Filled.WaterDrop,
                    unselectedIcon = Icons.Outlined.WaterDrop
                )
            }
        }
    }
}

@Composable
private fun PaintHeaderCard(paint: PaintDetailItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),  // Space for back button
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val fallbackColor = MaterialTheme.colorScheme.primary
        val color = remember(paint.hex) {
            runCatching { Color(parseColor(paint.hex)) }
                .getOrElse { fallbackColor }
        }

        // Start animation from 0
        var startAnimation by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            startAnimation = true
        }

        // Animated fill percentage (0f to 1f)
        val fillAnimation = androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 2000,
                delayMillis = 200,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            label = "fillAnimation"
        )

        var primaryWavePhase by remember { mutableStateOf(0f) }
        var secondaryWavePhase by remember { mutableStateOf(0f) }
        var highlightShift by remember { mutableStateOf(0f) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val isSettled = fillAnimation.value >= 0.995f

        LaunchedEffect(startAnimation, isSettled) {
            if (!startAnimation || isSettled) return@LaunchedEffect
            var lastFrameNanos = 0L
            while (isActive) {
                withFrameNanos { frameTime ->
                    if (lastFrameNanos != 0L) {
                        val deltaSeconds = (frameTime - lastFrameNanos) / 1_000_000_000f
                        primaryWavePhase = ((primaryWavePhase + deltaSeconds * 1.4f) % TWO_PI)
                        secondaryWavePhase = ((secondaryWavePhase + deltaSeconds * 1.9f) % TWO_PI)
                        highlightShift = ((highlightShift + deltaSeconds * 0.18f) % 1f)
                    }
                    lastFrameNanos = frameTime
                }
            }
        }

        // Large circular color preview with fill animation
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.CircleShape)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val radius = size.minDimension / 2f

                // Background (empty circle)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = radius,
                    center = center
                )

                if (fillAnimation.value > 0f) {
                    val fillHeight = canvasHeight * fillAnimation.value
                    val fillTop = canvasHeight - fillHeight

                    val waveEnergy = (1f - fillAnimation.value).coerceIn(0f, 1f)
                    val baseAmplitude = with(density) { 4.dp.toPx() }
                    val dynamicAmplitude = baseAmplitude * waveEnergy
                    val waveFrequency = 1.8f

                    val paintPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, canvasHeight)
                        lineTo(0f, fillTop)

                        val steps = 120
                        for (i in 0..steps) {
                            val x = (i / steps.toFloat()) * canvasWidth
                            val normalizedX = x / canvasWidth
                            val primaryWave = sin(normalizedX * waveFrequency * TWO_PI + primaryWavePhase) * dynamicAmplitude
                            val secondaryWave =
                                sin(normalizedX * (waveFrequency * 1.15f) * TWO_PI - secondaryWavePhase * 0.75f) * (dynamicAmplitude * 0.45f)
                            val y = (fillTop + primaryWave + secondaryWave).coerceIn(0f, canvasHeight)
                            lineTo(x, y)
                        }

                        lineTo(canvasWidth, canvasHeight)
                        close()
                    }

                    drawPath(
                        path = paintPath,
                        color = color
                    )

                    if (waveEnergy > 0f) {
                        // Subtle sheen inside the liquid that fades as the jar settles
                        clipPath(paintPath) {
                            val sheenStrength = waveEnergy.coerceIn(0f, 1f)
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.12f * sheenStrength),
                                        Color.Transparent
                                    ),
                                    startY = fillTop,
                                    endY = fillTop + canvasHeight * 0.3f
                                ),
                                size = size
                            )

                            val highlightTop = fillTop - canvasHeight * 0.16f
                            val highlightBottom = fillTop + canvasHeight * 0.04f
                            val highlightPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(canvasWidth * 0.12f, highlightBottom)
                                quadraticTo(
                                    canvasWidth * 0.5f,
                                    highlightTop,
                                    canvasWidth * 0.88f,
                                    highlightBottom
                                )
                                lineTo(canvasWidth * 0.88f, highlightTop - canvasHeight * 0.08f)
                                lineTo(canvasWidth * 0.12f, highlightTop - canvasHeight * 0.06f)
                                close()
                            }
                            val shift = highlightShift
                            drawPath(
                                path = highlightPath,
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.18f * sheenStrength),
                                        Color.White.copy(alpha = 0.04f * sheenStrength)
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(canvasWidth * (0.12f + shift * 0.08f), highlightTop),
                                    end = androidx.compose.ui.geometry.Offset(canvasWidth * (0.7f + shift * 0.12f), highlightBottom)
                                )
                            )
                        }
                    }

                    // Glass rim and glow
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = radius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.06f),
                        center = center
                    )
                    drawCircle(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(
                                canvasWidth * 0.35f,
                                canvasHeight * 0.32f
                            ),
                            radius = radius * 1.05f
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }

            // Add finish overlay effect on top
            if (paint.finish != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(androidx.compose.foundation.shape.CircleShape)
                ) {
                    when (paint.finish) {
                        io.brushforge.brushforge.domain.model.PaintFinish.Metallic -> {
                            val gradient = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.0f)
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                            )
                            drawRect(brush = gradient)
                        }
                        io.brushforge.brushforge.domain.model.PaintFinish.Gloss -> {
                            val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.0f)
                                ),
                                startY = 0f,
                                endY = size.height * 0.55f
                            )
                            drawRect(brush = gradient)
                        }
                        else -> {}
                    }
                }
            }
        }

        // Paint name and brand
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = paint.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Display brand and line with different colors
            Text(
                text = buildAnnotatedString {
                    val line = paint.line.cleanLabel()
                    val variant = paint.lineVariant.cleanLabel()

                    withStyle(style = SpanStyle(color = Color(0xFF64B5F6))) {
                        append(paint.brand)
                    }
                    line?.let {
                        append(" ")
                        withStyle(style = SpanStyle(color = Color(0xFF90CAF9))) {
                            append(it)
                        }
                    }
                    variant?.let {
                        append(" • ")
                        withStyle(style = SpanStyle(color = Color(0xFFBBDEFB))) {
                            append(it)
                        }
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Type, Finish and flags chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (paint.isCustom) {
                AssistChip(
                    onClick = {},
                    label = { Text("Custom") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }
            if (paint.isMixed) {
                AssistChip(
                    onClick = {},
                    label = { Text("Mix") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaintInfoCard(paint: PaintDetailItem) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Paint properties section
        Text(
            text = "Paint Properties",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // 2x2 grid of info cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                icon = Icons.Outlined.Brush,
                label = "Type",
                value = paint.type?.rawValue ?: "Unknown",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = Icons.Outlined.AutoAwesome,
                label = "Finish",
                value = paint.finish?.rawValue ?: "Unknown",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                icon = Icons.Outlined.Palette,
                label = "Color Family",
                value = paint.colorFamily.name,
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = Icons.Outlined.FormatColorFill,
                label = "RGB",
                value = "${paint.red}, ${paint.green}, ${paint.blue}",
                modifier = Modifier.weight(1f)
            )
        }

        // Tags section
        if (paint.tags.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paint.tags.sorted().forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }

        // Mix components section
        if (paint.isMixed && paint.mixComponents.isNotEmpty()) {
            MixComponentsCard(components = paint.mixComponents)
        }
    }
}

@Composable
private fun ColorValueChip(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MixComponentsCard(components: List<MixComponentUiModel>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Mix Recipe",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            components.forEach { component ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Color swatch
                    val fallbackColor = MaterialTheme.colorScheme.primary
                    val color = remember(component.hex) {
                        runCatching { Color(parseColor(component.hex)) }
                            .getOrElse { fallbackColor }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                    )

                    // Paint info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = component.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = component.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    // Percentage
                    Text(
                        text = "${component.percentage.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

private fun getUseCasesForType(type: PaintType): List<String> {
    return when (type) {
        PaintType.Base -> listOf(
            "Foundation colors for miniatures",
            "Large area coverage",
            "First coat after primer"
        )
        PaintType.Layer -> listOf(
            "Building up color gradually",
            "Fine detail work",
            "Smooth thin coats"
        )
        PaintType.Wash -> listOf(
            "Adding depth to recesses",
            "Instant shading",
            "Weathering effects"
        )
        PaintType.Shade -> listOf(
            "Darkening recessed areas",
            "Creating shadows",
            "Adding definition"
        )
        PaintType.Highlight -> listOf(
            "Raised surfaces",
            "Edge highlighting",
            "Creating volume"
        )
        PaintType.Metallic -> listOf(
            "Armor and weapons",
            "Jewelry and coins",
            "Mechanical parts"
        )
        PaintType.Technical -> listOf(
            "Special effects",
            "Blood/corrosion/verdigris",
            "Texture creation"
        )
        PaintType.Primer -> listOf(
            "Surface preparation",
            "Paint adhesion",
            "First layer on bare plastic/metal"
        )
        PaintType.Contrast -> listOf(
            "One-coat painting",
            "Speed painting",
            "Automatic shading"
        )
        PaintType.Dry -> listOf(
            "Drybrushing highlights",
            "Texture emphasis",
            "Quick edge work"
        )
        PaintType.Glaze -> listOf(
            "Subtle color transitions",
            "Tinting",
            "Smooth blending"
        )
        PaintType.Ink -> listOf(
            "Fine line work",
            "Intense color in recesses",
            "Transparent layers"
        )
        PaintType.Air -> listOf(
            "Airbrushing",
            "Smooth gradients",
            "Large surface coverage"
        )
        PaintType.Spray -> listOf(
            "Base coating",
            "Priming",
            "Quick coverage"
        )
        PaintType.Speed -> listOf(
            "Fast batch painting",
            "Tabletop quality",
            "Army painting"
        )
        else -> emptyList()
    }
}

@Composable
private fun InfoCard(
    icon: Any,  // Can be String or ImageVector
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (icon) {
                is androidx.compose.ui.graphics.vector.ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                is String -> {
                    // Check if icon is text (like "RGB") or emoji
                    if (icon.length <= 3 && icon.all { it.isLetterOrDigit() }) {
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusCard(
    state: PaintDetailUiState,
    onOwnedToggle: (Boolean) -> Unit,
    onWishlistToggle: (Boolean) -> Unit,
    onAlmostEmptyToggle: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Collection Status",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Big toggle buttons for Owned and Wishlist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOwnedToggle(!state.isOwned) },
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isOwned)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (state.isOwned) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (state.isOwned)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Owned",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (state.isOwned) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onWishlistToggle(!state.isWishlist) },
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isWishlist)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (state.isWishlist) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (state.isWishlist)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Wishlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (state.isWishlist) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Almost Empty toggle (only show for owned paints)
        if (state.isOwned) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isAlmostEmpty) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (state.isAlmostEmpty)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column {
                            Text(
                                text = "Almost empty",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Keep track of low stock bottles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = state.isAlmostEmpty,
                        onCheckedChange = { onAlmostEmptyToggle(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    canSave: Boolean,
    isSaving: Boolean,
    onNotesChanged: (String) -> Unit,
    onSaveNotes: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Personal Notes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Add techniques, thinning ratios or recipe tweaks.") },
                    shape = MaterialTheme.shapes.medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onSaveNotes,
                        enabled = canSave && !isSaving
                    ) {
                        if (isSaving) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = if (isSaving) "Saving…" else "Save notes")
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarPaintsSection(
    paints: List<SimilarPaintUiModel>,
    onSimilarPaintSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Similar Paints",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (paints.isEmpty()) {
            Text(
                text = "No close matches found in the catalog.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(paints, key = { it.stableId }) { item ->
                    SimilarPaintCard(
                        item = item,
                        onClick = { onSimilarPaintSelected(item.stableId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SimilarPaintCard(
    item: SimilarPaintUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val fallbackColor = MaterialTheme.colorScheme.primary
            val color = remember(item.hex) {
                runCatching { Color(parseColor(item.hex)) }
                    .getOrElse { fallbackColor }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(color)
            )
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${item.confidenceLabel} • ΔE ${"%.2f".format(item.deltaE)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${item.typeLabel} • ${item.finishLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorRecommendationsSection(
    colorRecommendations: ColorRecommendations?,
    onPaintSelected: (String) -> Unit
) {
    if (colorRecommendations == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Color recommendations not available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "This feature is only available for catalog paints",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Highlight & Shadow Section
        HighlightShadowSection(
            highlight = colorRecommendations.highlight,
            shadow = colorRecommendations.shadow,
            onPaintSelected = onPaintSelected
        )

        // Complementary colors
        if (colorRecommendations.complementary.isNotEmpty()) {
            ColorHarmonySection(
                title = "Complementary Colors",
                description = "Opposite on the color wheel - great for contrast",
                paints = colorRecommendations.complementary,
                onPaintSelected = onPaintSelected
            )
        }

        // Analogous colors
        if (colorRecommendations.analogous.isNotEmpty()) {
            ColorHarmonySection(
                title = "Analogous Colors",
                description = "Adjacent on the color wheel - create harmony",
                paints = colorRecommendations.analogous,
                onPaintSelected = onPaintSelected
            )
        }

        // Info card if no owned paints for harmony
        if (colorRecommendations.complementary.isEmpty() && colorRecommendations.analogous.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Add more paints to your collection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Mark paints as owned to see complementary and analogous color recommendations from your collection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightShadowSection(
    highlight: io.brushforge.brushforge.domain.model.ColorRecommendation?,
    shadow: io.brushforge.brushforge.domain.model.ColorRecommendation?,
    onPaintSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Highlight & Shadow",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Recommended paints for adding depth to your miniature. Highlights add brightness, shadows add depth.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Highlight
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Brush,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Highlight",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (highlight != null) {
                    ColorRecommendationCard(
                        paint = highlight.paint,
                        score = highlight.score,
                        onClick = { onPaintSelected(highlight.paint.stableId) }
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No match found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Shadow
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Shadow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (shadow != null) {
                    ColorRecommendationCard(
                        paint = shadow.paint,
                        score = shadow.score,
                        onClick = { onPaintSelected(shadow.paint.stableId) }
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No match found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorHarmonySection(
    title: String,
    description: String,
    paints: List<io.brushforge.brushforge.domain.model.ColorRecommendation>,
    onPaintSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Grid layout for color swatches
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            paints.forEach { recommendation ->
                CompactColorCard(
                    paint = recommendation.paint,
                    score = recommendation.score,
                    onClick = { onPaintSelected(recommendation.paint.stableId) }
                )
            }
        }
    }
}

@Composable
private fun CompactColorCard(
    paint: io.brushforge.brushforge.domain.model.CatalogPaint,
    score: Float,
    onClick: () -> Unit
) {
    val fallbackColor = MaterialTheme.colorScheme.primary
    val color = remember(paint.hex) {
        runCatching { Color(parseColor(paint.hex)) }
            .getOrElse { fallbackColor }
    }

    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large color swatch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(color)
            )

            // Paint info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = paint.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = paint.brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Match quality badge
                val matchQuality = when {
                    score >= 0.9f -> "Excellent"
                    score >= 0.8f -> "Very Good"
                    score >= 0.7f -> "Good"
                    else -> "Fair"
                }
                val badgeColor = when {
                    score >= 0.9f -> MaterialTheme.colorScheme.primaryContainer
                    score >= 0.8f -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }

                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = matchQuality,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorRecommendationCard(
    paint: io.brushforge.brushforge.domain.model.CatalogPaint,
    score: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val fallbackColor = MaterialTheme.colorScheme.primary
            val color = remember(paint.hex) {
                runCatching { Color(parseColor(paint.hex)) }
                    .getOrElse { fallbackColor }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(color)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = paint.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = paint.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${paint.type.rawValue} • ${paint.finish.rawValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Match quality indicator
                val matchQuality = when {
                    score >= 0.9f -> "Excellent"
                    score >= 0.8f -> "Very Good"
                    score >= 0.7f -> "Good"
                    else -> "Fair"
                }
                Text(
                    text = "Match: $matchQuality",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
