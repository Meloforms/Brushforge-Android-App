# Palettes/Recipes Feature - Code Review & Recommendations

## Executive Summary

The Palettes/Recipes feature is well-implemented with solid architecture. However, there are several areas where adherence to best practices, design system usage, and code organization can be improved to support future features and maintainability.

---

## ✅ What's Working Well

### 1. **Architecture & Structure**
- ✅ Clean MVVM architecture with proper separation of concerns
- ✅ Unidirectional data flow with StateFlow
- ✅ Proper use of Hilt dependency injection
- ✅ Repository pattern correctly implemented
- ✅ Domain models are clean and well-defined

### 2. **State Management**
- ✅ Single source of truth with UI state data classes
- ✅ Immutable state updates using `.update { }`
- ✅ Proper Flow usage for reactive data
- ✅ Loading and error states handled

### 3. **Code Quality**
- ✅ Good naming conventions
- ✅ Functions are reasonably sized
- ✅ Null safety properly handled
- ✅ ViewModelScope used correctly for coroutines

---

## ⚠️ Issues & Recommendations

### 1. **Design System Usage - CRITICAL**

**Issue**: Hardcoded colors and typography instead of using the design system.

**Current Code (RecipeDetailScreen.kt)**:
```kotlin
// ❌ Hardcoded colors
Box(
    modifier = Modifier
        .size(10.dp)
        .background(
            if (isOwned) Color(0xFF4CAF50)  // ❌ Hardcoded green
            else Color(0xFFF44336)           // ❌ Hardcoded red
        )
)

// ❌ Direct color usage
Text(
    "Delete",
    color = Color(0xFFF44336)  // ❌ Hardcoded red
)

// ❌ Hardcoded font weight
Text(
    text = step.paintName,
    fontWeight = FontWeight.SemiBold  // ❌ Not from typography system
)
```

**Recommended Fix**:
```kotlin
// ✅ Create semantic color tokens in BrushforgeTheme.kt
private val BrushforgeDarkColorScheme = darkColorScheme(
    // ... existing colors ...
    error = Color(0xFFFFB4AB),  // Already exists

    // Add new semantic colors:
    // For ownership indicators:
    tertiary = Color(0xFF4CAF50),      // Success/Owned
    onTertiary = Color(0xFFFFFFFF),

    // Consider adding custom color scheme extensions:
)

// ✅ Use theme colors
Box(
    modifier = Modifier
        .size(10.dp)
        .background(
            if (isOwned) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.error
        )
)

Text(
    "Delete",
    color = MaterialTheme.colorScheme.error
)

// ✅ Define typography variants in Typography.kt
val BrushforgeTypography = Typography(
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Add custom variant for emphasized body text:
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
)

// Then use it:
Text(
    text = step.paintName,
    style = MaterialTheme.typography.labelLarge  // ✅ From design system
)
```

**Impact**: CRITICAL - Inconsistent theming, difficult to maintain, can't support light mode properly.

---

### 2. **File Organization - HIGH PRIORITY**

**Issue**: RecipeDetailScreen.kt is 1083 lines - too large for maintainability.

**Current Structure**:
```
feature/palettes/
├── RecipeDetailScreen.kt (1083 lines) ❌ Too large
├── RecipeDetailViewModel.kt (514 lines)
├── PalettesScreen.kt
├── PalettesViewModel.kt
├── PaintPickerDialog.kt
└── SampleRecipeHelper.kt
```

**Recommended Structure**:
```
feature/palettes/
├── screens/
│   ├── list/
│   │   ├── PalettesScreen.kt
│   │   └── PalettesViewModel.kt
│   └── detail/
│       ├── RecipeDetailScreen.kt (main composable only)
│       ├── RecipeDetailViewModel.kt
│       └── components/
│           ├── RecipeHeader.kt
│           ├── RecipeTagsSection.kt
│           ├── RecipeNotesSection.kt
│           ├── RecipeReferenceImageSection.kt
│           ├── RecipeStepCard.kt
│           └── RecipeStepsList.kt
├── components/
│   └── PaintPickerDialog.kt
└── util/
    └── SampleRecipeHelper.kt
```

**Refactoring Example**:
```kotlin
// ✅ RecipeHeader.kt
@Composable
internal fun RecipeHeader(
    recipeName: String,
    isFavorite: Boolean,
    stepCount: Int,
    ownedCount: Int,
    ownedStableIds: Set<String>,
    isEditMode: Boolean,
    onToggleFavorite: () -> Unit,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Component implementation
}

// ✅ RecipeStepCard.kt
@Composable
internal fun RecipeStepCard(
    step: RecipeStep,
    stepNumber: Int,
    isOwned: Boolean,
    isEditMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    onFindSubstitutes: () -> Unit,
    onReplacePaint: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Component implementation
}
```

**Impact**: HIGH - Improves maintainability, testability, and makes future feature additions easier.

---

### 3. **Magic Numbers & Constants - MEDIUM PRIORITY**

**Issue**: Hardcoded sizes and spacings scattered throughout code.

**Current Code**:
```kotlin
// ❌ Magic numbers
Box(modifier = Modifier.size(56.dp))           // What does 56dp represent?
Box(modifier = Modifier.size(32.dp))           // What does 32dp represent?
Box(modifier = Modifier.size(10.dp))           // What does 10dp represent?
RoundedCornerShape(12.dp)                      // Why 12dp?
.padding(horizontal = 16.dp, vertical = 24.dp) // Why these values?
```

**Recommended Fix**:
```kotlin
// ✅ Create a dimensions object
// core/ui/theme/Dimensions.kt
object BrushforgeDimensions {
    // Spacing
    val spacingXSmall = 4.dp
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val spacingLarge = 24.dp
    val spacingXLarge = 32.dp

    // Component sizes
    val paintSwatchSmall = 48.dp
    val paintSwatchMedium = 56.dp
    val paintSwatchLarge = 64.dp

    val stepNumberBadgeSize = 32.dp
    val ownershipIndicatorSize = 10.dp

    // Border radius
    val cornerRadiusSmall = 8.dp
    val cornerRadiusMedium = 12.dp
    val cornerRadiusLarge = 16.dp
    val cornerRadiusCircle = 50.dp
}

// ✅ Usage
Box(
    modifier = Modifier.size(BrushforgeDimensions.paintSwatchMedium)
)

Box(
    modifier = Modifier.size(BrushforgeDimensions.stepNumberBadgeSize)
)

RoundedCornerShape(BrushforgeDimensions.cornerRadiusMedium)
```

**Impact**: MEDIUM - Improves consistency and makes design adjustments easier.

---

### 4. **Composable Visibility & Reusability - MEDIUM PRIORITY**

**Issue**: All composables in RecipeDetailScreen.kt are `private`, limiting reusability.

**Current Code**:
```kotlin
@Composable
private fun StepCard(...) { }  // ❌ Can't be reused elsewhere

@Composable
private fun TagsSection(...) { }  // ❌ Can't be reused elsewhere
```

**Recommended Fix**:
```kotlin
// ✅ Use internal for feature-module visibility
@Composable
internal fun RecipeStepCard(...) { }  // ✅ Can be used in other screens

@Composable
internal fun RecipeTagsSection(...) { }  // ✅ Can be used in other screens

// ✅ Keep only truly private helpers as private
@Composable
private fun StepMenuDropdown(...) { }  // ✅ Internal implementation detail
```

**Impact**: MEDIUM - Enables component reuse and easier testing.

---

### 5. **Error Handling - MEDIUM PRIORITY**

**Issue**: Limited error handling and no user feedback for failures.

**Current Code (RecipeDetailViewModel.kt)**:
```kotlin
// ❌ Silent failure
private suspend fun resolveCatalogPaintForStep(step: RecipeStep): CatalogPaint? {
    try {
        // ... resolution logic ...
    } catch (t: Throwable) {
        android.util.Log.e("RecipeDetailViewModel", "Failed...", t)
        // ❌ User doesn't know what happened
        return null
    }
}
```

**Recommended Fix**:
```kotlin
// ✅ Add events for user feedback
sealed class RecipeDetailEvent {
    data class ShowError(val message: String) : RecipeDetailEvent()
    data class ShowMessage(val message: String) : RecipeDetailEvent()
}

// In ViewModel:
private val _events = MutableSharedFlow<RecipeDetailEvent>()
val events = _events.asSharedFlow()

private suspend fun resolveCatalogPaintForStep(step: RecipeStep): CatalogPaint? {
    return try {
        // ... resolution logic ...
    } catch (t: Throwable) {
        android.util.Log.e("RecipeDetailViewModel", "Failed...", t)
        _events.emit(RecipeDetailEvent.ShowError("Unable to find paint substitutes"))
        null
    }
}

// In Screen:
val snackbarHostState = remember { SnackbarHostState() }

LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        when (event) {
            is RecipeDetailEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            is RecipeDetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
        }
    }
}

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
) { ... }
```

**Impact**: MEDIUM - Better user experience and debugging.

---

### 6. **Code Duplication - LOW PRIORITY**

**Issue**: Dialog creation code is repetitive.

**Current Code**:
```kotlin
// ❌ Repeated dialog pattern
if (state.showEditNameDialog) {
    AlertDialog(
        onDismissRequest = viewModel::onDismissEditNameDialog,
        title = { Text("Edit Recipe Name") },
        text = {
            OutlinedTextField(
                value = state.editNameText,
                onValueChange = viewModel::onEditNameTextChanged,
                label = { Text("Name") }
            )
        },
        confirmButton = {
            TextButton(onClick = viewModel::onConfirmEditName) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::onDismissEditNameDialog) {
                Text("Cancel")
            }
        }
    )
}

// Same pattern repeated for tags, notes, step notes...
```

**Recommended Fix**:
```kotlin
// ✅ Create a reusable text input dialog
@Composable
internal fun TextInputDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    label: String = "",
    placeholder: String = "",
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = singleLine,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        modifier = modifier
    )
}

// ✅ Usage
if (state.showEditNameDialog) {
    TextInputDialog(
        title = "Edit Recipe Name",
        value = state.editNameText,
        onValueChange = viewModel::onEditNameTextChanged,
        onConfirm = viewModel::onConfirmEditName,
        onDismiss = viewModel::onDismissEditNameDialog,
        label = "Name"
    )
}
```

**Impact**: LOW - Reduces code duplication, easier to maintain.

---

### 7. **Testing Infrastructure - LOW PRIORITY**

**Issue**: No tests for ViewModels or composables.

**Recommended Setup**:
```kotlin
// test/RecipeDetailViewModelTest.kt
class RecipeDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: RecipeDetailViewModel
    private lateinit var recipeRepository: FakeRecipeRepository
    private lateinit var userPaintRepository: FakeUserPaintRepository

    @Before
    fun setup() {
        recipeRepository = FakeRecipeRepository()
        userPaintRepository = FakeUserPaintRepository()
        viewModel = RecipeDetailViewModel(
            recipeRepository,
            userPaintRepository,
            catalogPaintRepository,
            SavedStateHandle(mapOf("recipeId" to "test-id"))
        )
    }

    @Test
    fun `toggling favorite updates state`() = runTest {
        // Given
        val initialRecipe = createTestRecipe(isFavorite = false)
        recipeRepository.emit(initialRecipe)

        // When
        viewModel.onToggleFavorite()

        // Then
        assertEquals(true, viewModel.state.value.recipe?.isFavorite)
        assertTrue(viewModel.state.value.hasUnsavedChanges)
    }

    @Test
    fun `adding step increments step count`() = runTest {
        // Test implementation
    }
}
```

**Impact**: LOW (for now) - Critical for long-term maintainability.

---

## 📋 Action Items (Prioritized)

### CRITICAL (Do Before Adding More Features)
1. **Extract hardcoded colors to design system**
   - Add semantic color tokens for owned/not-owned states
   - Replace all `Color(0x...)` with `MaterialTheme.colorScheme.*`
   - Add color extensions if needed for custom states

2. **Extract hardcoded typography**
   - Define all font weights in Typography.kt
   - Replace `FontWeight.*` with typography styles
   - Ensure text styles are consistent

### HIGH PRIORITY (Next Sprint)
3. **Refactor RecipeDetailScreen.kt**
   - Extract components into separate files
   - Create components/ directory structure
   - Move to internal visibility for reusability

4. **Add proper error handling**
   - Implement events flow in all ViewModels
   - Add SnackbarHost to all screens
   - Provide user feedback for all errors

### MEDIUM PRIORITY (Soon)
5. **Create Dimensions object**
   - Extract all magic numbers
   - Define semantic sizing constants
   - Update all spacing to use constants

6. **Add comprehensive documentation**
   - KDoc for all public/internal functions
   - Architecture decision records
   - Component usage examples

### LOW PRIORITY (Eventually)
7. **Create reusable dialog components**
   - Extract common dialog patterns
   - Create feature/palettes/components/dialogs/

8. **Add unit tests**
   - ViewModel tests for all user actions
   - Repository tests with fakes
   - Compose UI tests for critical flows

---

## 🎯 Design System Checklist

Before considering the feature "complete", ensure:

- [ ] No hardcoded colors (`Color(0x...)`) - use `MaterialTheme.colorScheme.*`
- [ ] No hardcoded typography weights - use `MaterialTheme.typography.*`
- [ ] No magic numbers for spacing - use constants
- [ ] No magic numbers for sizing - use constants
- [ ] All components use Material3 components where possible
- [ ] Consistent corner radius usage
- [ ] Consistent padding/spacing patterns
- [ ] Theme supports both light and dark modes
- [ ] Accessibility: color contrast ratios meet WCAG AA
- [ ] Accessibility: semantic content descriptions

---

## 🔮 Future-Proofing Recommendations

### For Upcoming Features:

1. **Image Support (Reference Images)**
   - Will need: Image loading library (Coil)
   - Will need: Permissions handling
   - Current structure supports this ✅

2. **Sharing Recipes**
   - Will need: Serialization logic
   - Will need: Deep linking support
   - Consider: Export to different formats

3. **Recipe Import**
   - Will need: Import validation
   - Will need: Conflict resolution UI
   - Will need: Bulk operations

4. **Advanced Filtering/Search**
   - Current flat structure works ✅
   - May need: Search indexing for performance
   - May need: Filter state persistence

5. **Recipe Templates**
   - Current sample structure can be extended ✅
   - Will need: Template categorization
   - Will need: Template preview

---

## 📊 Technical Debt Score

| Category | Score | Notes |
|----------|-------|-------|
| Architecture | 9/10 | Excellent MVVM, minor improvements needed |
| Design System | 5/10 | ⚠️ Major issues with hardcoded values |
| Code Organization | 6/10 | Files too large, needs refactoring |
| Error Handling | 6/10 | Basic handling, needs user feedback |
| Testing | 2/10 | ⚠️ No tests currently |
| Documentation | 7/10 | Good naming, needs KDoc |
| **Overall** | **6.5/10** | **Good foundation, needs polish** |

---

## ✅ Conclusion

The Palettes/Recipes feature has a **solid foundation** with good architecture and state management. The main issues are:

1. **Design system adherence** (hardcoded colors/typography)
2. **File organization** (large files need splitting)
3. **Error handling** (needs user feedback)

These are **fixable** and should be addressed before adding major new features. The current structure will support future features well once these items are resolved.

**Recommended Timeline:**
- **Week 1**: Fix design system usage (colors, typography)
- **Week 2**: Refactor RecipeDetailScreen into components
- **Week 3**: Add error handling and user feedback
- **Week 4**: Extract constants and add documentation

After these improvements, the codebase will be in excellent shape for new features.
