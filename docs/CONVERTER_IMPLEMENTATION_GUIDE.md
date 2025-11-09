# Quick Implementation Guide - Converter Sorting & Filtering

## File Locations (Absolute Paths)

- **ViewModel:** `/Users/basinert/Desktop/Brushforge-Android-Project/Brushforge/feature/converter/src/main/java/io/brushforge/brushforge/feature/converter/ConverterViewModel.kt`
- **Screen:** `/Users/basinert/Desktop/Brushforge-Android-Project/Brushforge/feature/converter/src/main/java/io/brushforge/brushforge/feature/converter/ConverterScreen.kt`
- **Domain Model:** `/Users/basinert/Desktop/Brushforge-Android-Project/Brushforge/domain/src/main/kotlin/io/brushforge/brushforge/domain/model/PaintMatch.kt`
- **UseCase:** `/Users/basinert/Desktop/Brushforge-Android-Project/Brushforge/domain/src/main/kotlin/io/brushforge/brushforge/domain/usecase/FindSimilarPaintsUseCase.kt`

---

## Issue #1: "Best Match" vs "Confidence" Show Identical Results

### Root Cause
In `FindSimilarPaintsUseCase.kt` lines 101-110, confidence is calculated as:
```kotlin
confidence = (1.0 - (distance / 50.0)).coerceIn(0.0, 1.0)
```

This means:
- Lower distance = Higher confidence (inverse relationship)
- Sorting by distance ascending = Sorting by confidence descending
- **They produce identical sort order**

### Solution

**Option A: Use Different Sorting Criteria**
Rename "Confidence" to something else that represents a different calculation:
- "Best Match" = lowest distance (current ByDistance)
- "Most Available" = owned paints first, then by distance
- "Brand First" = current ByBrand
- "Quality" = filter instead of sort

**Option B: Multi-Factor Confidence (Recommended)**
Replace the confidence calculation in `FindSimilarPaintsUseCase.kt` to include:
1. Color distance (40%)
2. Type match (25%)
3. Finish match (15%)
4. User owns it (15%)
5. Brand preference (5%)

---

## Issue #2: Add "Owned Only" Filter

### Step 1: Update ConverterViewModel.kt

Add to `ConverterUiState` data class (around line 366):
```kotlin
val userOwnedPaintIds: Set<String> = emptySet(),  // New
val showOwnedOnly: Boolean = false,  // New
```

Add function after `onFilterQualityChanged()` (after line 315):
```kotlin
fun onFilterOwnedOnlyChanged(showOwnedOnly: Boolean) {
    _state.update {
        it.copy(showOwnedOnly = showOwnedOnly)
    }
}
```

Update the `filteredMatches` computed property (lines 368-375):
```kotlin
val filteredMatches: List<PaintMatch>
    get() = matches.filter { match ->
        // Quality filters
        val qualityMatch = when {
            showExcellentOnly -> match.isExcellentMatch
            showGoodOnly -> match.isGoodMatch
            else -> true
        }
        
        // Owned filter
        val ownedMatch = if (showOwnedOnly) {
            match.paint.stableId in userOwnedPaintIds
        } else {
            true
        }
        
        qualityMatch && ownedMatch
    }
```

### Step 2: Load User Paints in ConverterViewModel

Inject repository (in constructor, around line 26):
```kotlin
@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val catalogPaintRepository: CatalogPaintRepository,
    private val findSimilarPaintsUseCase: FindSimilarPaintsUseCase,
    private val findMixingRecipesUseCase: FindMixingRecipesUseCase,
    private val userPaintRepository: UserPaintRepository  // ADD THIS
) : ViewModel() {
```

Call in init block (after line 40):
```kotlin
init {
    loadBrands()
    loadAllPaints()
    loadUserPaints()  // ADD THIS
}

private fun loadUserPaints() {
    viewModelScope.launch {
        try {
            val userPaints = userPaintRepository.getAllPaints()
            _state.update { state ->
                state.copy(
                    userOwnedPaintIds = userPaints.map { it.catalogPaintId }.toSet()
                )
            }
        } catch (e: Exception) {
            // Log error or emit event
        }
    }
}
```

### Step 3: Add UI Control in ConverterScreen.kt

**Option A: In ResultsView (with sort chips)**

Add to the sort controls section (replace lines 495-503):
```kotlin
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
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = state.showOwnedOnly,
            onClick = { /* onFilterOwnedOnlyChanged(true/false) */ },
            label = { Text("Owned Only") }
        )
    }
}
```

**Option B: In SearchView (before Find button)**

Add after the brand selection card (after line 356):
```kotlin
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Owned paints only")
            Switch(
                checked = state.showOwnedOnly,
                onCheckedChange = { /* onFilterOwnedOnlyChanged(it) */ }
            )
        }
    }
}
```

---

## Quick Reference: Current Filtering Architecture

### 1. Source Paint Search Filters (SearchView)
- `searchBrandFilter` - Filter search results by brand
- `searchTypeFilter` - Filter search results by paint type
- Applied in: `filterSearchResults()` function

### 2. Target Paint Filters (SearchView)
- `selectedTargetBrands` - Which brands to search for matches in
- Applied in: `FindSimilarPaintsUseCase.invoke()`

### 3. Match Results Filters (ResultsView)
- `showExcellentOnly` - Keep only excellent matches
- `showGoodOnly` - Keep only good matches
- Applied in: `filteredMatches` computed property

### 4. New: Owned Paint Filter (ResultsView)
- `showOwnedOnly` - Keep only owned paints
- Applied in: `filteredMatches` computed property

---

## Testing Checklist

After implementing "Owned Only" filter:

- [ ] Filter state loads correctly from repository
- [ ] Switching "Owned Only" on/off updates results
- [ ] Filter works with other quality filters
- [ ] Filter works with different sort options
- [ ] Filter still works after navigating back and forth
- [ ] No performance issues with large paint libraries
- [ ] UI shows clear feedback of filter status

---

## Related Issues to Consider

1. **Confidence is poorly named** - Consider renaming to:
   - "Closest Color" (ByDistance is clear)
   - "Quality Tier" (Excellent/Good/Fair/Poor)
   - Remove "Confidence" sort entirely if calculation isn't improved

2. **No visual feedback for owned paints in search**
   - Add badge/checkmark to already-owned paints
   - Consider pre-filtering to show owned paints first

3. **No bulk filter application**
   - Currently each filter needs separate UI
   - Consider collapsing all filters into a bottom sheet
   - Add "Clear all filters" button

4. **No saved filter preferences**
   - User has to reset filters each session
   - Consider remembering last filter state in SharedPreferences or DataStore

---

## Code Snippet References

### Check if Paint is Owned
```kotlin
match.paint.stableId in state.userOwnedPaintIds
```

### Composite Filter Example
```kotlin
val filteredMatches: List<PaintMatch>
    get() = matches.filter { match ->
        val qualityMatch = when {
            showExcellentOnly -> match.isExcellentMatch
            showGoodOnly -> match.isGoodMatch
            else -> true
        }
        
        val ownedMatch = if (showOwnedOnly) {
            match.paint.stableId in userOwnedPaintIds
        } else {
            true
        }
        
        val typeMatch = if (requireSameType) {
            match.paint.type == selectedSourcePaint?.type
        } else {
            true
        }
        
        qualityMatch && ownedMatch && typeMatch
    }
```

---

## Documentation for Reference

See these files in the project root:
- `sorting_filtering_analysis.md` - Full technical analysis
- `data_flow_diagram.txt` - Visual data flow and architecture
