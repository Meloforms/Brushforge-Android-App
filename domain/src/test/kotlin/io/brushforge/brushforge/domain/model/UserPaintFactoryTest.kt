package io.brushforge.brushforge.domain.model

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for UserPaintFactory to ensure paint creation logic is correct.
 *
 * These tests verify:
 * - Catalog paints are correctly converted to user paints
 * - Custom paints are created with proper flags
 * - Mixed paints compute correct hex color from components
 * - Stable IDs are properly generated
 * - Default values are set correctly
 */
class UserPaintFactoryTest {

    private lateinit var factory: UserPaintFactory
    private lateinit var mockUuid: UUID
    private lateinit var mockUuidProvider: UUIDStringProvider

    @Before
    fun setup() {
        factory = UserPaintFactory()
        mockUuid = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        mockUuidProvider = UUIDStringProvider { mockUuid }
    }

    // ============================================================================
    // createFromCatalog Tests
    // ============================================================================

    @Test
    fun `createFromCatalog creates owned paint from catalog`() {
        val catalogPaint = CatalogPaint(
            name = "Abaddon Black",
            code = "62-02",
            brand = "Citadel",
            line = "Base",
            lineVariant = null,
            hex = "#000000",
            red = 0,
            green = 0,
            blue = 0,
            type = PaintType.Base,
            finish = PaintFinish.Matte,
            tags = emptySet()
        )

        val result = factory.createFromCatalog(
            paint = catalogPaint,
            isOwned = true,
            isWishlist = false,
            notes = "Test notes",
            uuidProvider = mockUuidProvider
        )

        assertEquals(mockUuid, result.id)
        assertEquals(catalogPaint.stableId, result.stableId)
        assertEquals("Abaddon Black", result.name)
        assertEquals("Citadel", result.brand)
        assertEquals("Base", result.line)
        assertEquals("#000000", result.hex)
        assertTrue(result.isOwned)
        assertFalse(result.isWishlist)
        assertEquals("Test notes", result.notes)
        assertFalse(result.isAlmostEmpty)
        assertFalse(result.isCustom)
        assertFalse(result.isMixed)
        assertEquals(PaintType.Base, result.type)
        assertEquals(PaintFinish.Matte, result.finish)
        assertTrue(result.mixComponents.isEmpty())
    }

    @Test
    fun `createFromCatalog creates wishlist paint from catalog`() {
        val catalogPaint = CatalogPaint(
            name = "Magic Blue",
            code = "72.021",
            brand = "Vallejo",
            line = "Game Color",
            lineVariant = null,
            hex = "#0080FF",
            red = 0,
            green = 128,
            blue = 255,
            type = PaintType.Base,
            finish = PaintFinish.Matte,
            tags = emptySet()
        )

        val result = factory.createFromCatalog(
            paint = catalogPaint,
            isOwned = false,
            isWishlist = true,
            uuidProvider = mockUuidProvider
        )

        assertFalse(result.isOwned)
        assertTrue(result.isWishlist)
        assertNull(result.notes)
    }

    @Test
    fun `createFromCatalog preserves paint attributes`() {
        val catalogPaint = CatalogPaint(
            name = "Metallic Gold",
            code = "TEST-001",
            brand = "Test Brand",
            line = "Metallics",
            lineVariant = "Premium",
            hex = "#FFD700",
            red = 255,
            green = 215,
            blue = 0,
            type = PaintType.Metallic,
            finish = PaintFinish.Metallic,
            tags = setOf("gold", "metallic")
        )

        val result = factory.createFromCatalog(
            paint = catalogPaint,
            isOwned = true,
            isWishlist = false,
            uuidProvider = mockUuidProvider
        )

        assertEquals(PaintType.Metallic, result.type)
        assertEquals(PaintFinish.Metallic, result.finish)
        assertEquals("Metallics", result.line)
    }

    // ============================================================================
    // createCustom Tests
    // ============================================================================

    @Test
    fun `createCustom creates custom paint with correct flags`() {
        val result = factory.createCustom(
            name = "My Custom Red",
            brand = "HomeBrew",
            hex = "#FF0000",
            type = PaintType.Base,
            finish = PaintFinish.Matte,
            notes = "Custom mix",
            uuidProvider = mockUuidProvider
        )

        assertEquals(mockUuid, result.id)
        assertTrue(result.stableId.startsWith(PaintStableIds.CUSTOM_PREFIX))
        assertEquals("user_custom_12345678-1234-1234-1234-123456789abc", result.stableId)
        assertEquals("My Custom Red", result.name)
        assertEquals("HomeBrew", result.brand)
        assertNull(result.line)
        assertEquals("#FF0000", result.hex)
        assertTrue(result.isOwned)
        assertFalse(result.isWishlist)
        assertEquals("Custom mix", result.notes)
        assertFalse(result.isAlmostEmpty)
        assertTrue(result.isCustom)
        assertFalse(result.isMixed)
        assertEquals(PaintType.Base, result.type)
        assertEquals(PaintFinish.Matte, result.finish)
        assertTrue(result.mixComponents.isEmpty())
    }

    @Test
    fun `createCustom allows null type and finish`() {
        val result = factory.createCustom(
            name = "Unknown Type Paint",
            brand = "Generic",
            hex = "#123456",
            type = null,
            finish = null,
            notes = null,
            uuidProvider = mockUuidProvider
        )

        assertNull(result.type)
        assertNull(result.finish)
        assertNull(result.notes)
    }

    @Test
    fun `createCustom defaults to owned not wishlist`() {
        val result = factory.createCustom(
            name = "Test",
            brand = "Test",
            hex = "#FFFFFF",
            type = null,
            finish = null,
            notes = null,
            uuidProvider = mockUuidProvider
        )

        assertTrue(result.isOwned)
        assertFalse(result.isWishlist)
    }

    // ============================================================================
    // createMixed Tests
    // ============================================================================

    @Test
    fun `createMixed creates mixed paint with correct flags`() {
        val components = listOf(
            MixComponent(
                id = UUID.randomUUID(),
                stableId = "citadel_mephiston_red",
                name = "Mephiston Red",
                brand = "Citadel",
                hex = "#FF0000",
                percentage = 50.0
            ),
            MixComponent(
                id = UUID.randomUUID(),
                stableId = "citadel_abaddon_black",
                name = "Abaddon Black",
                brand = "Citadel",
                hex = "#000000",
                percentage = 50.0
            )
        )

        val result = factory.createMixed(
            name = "Dark Red Mix",
            brand = "Custom Mix",
            components = components,
            type = PaintType.Base,
            finish = PaintFinish.Matte,
            notes = "50/50 mix",
            uuidProvider = mockUuidProvider
        )

        assertEquals(mockUuid, result.id)
        assertTrue(result.stableId.startsWith(PaintStableIds.MIX_PREFIX))
        assertEquals("user_mix_12345678-1234-1234-1234-123456789abc", result.stableId)
        assertEquals("Dark Red Mix", result.name)
        assertEquals("Custom Mix", result.brand)
        assertNull(result.line)
        assertTrue(result.isOwned)
        assertFalse(result.isWishlist)
        assertEquals("50/50 mix", result.notes)
        assertFalse(result.isAlmostEmpty)
        assertFalse(result.isCustom)
        assertTrue(result.isMixed)
        assertEquals(PaintType.Base, result.type)
        assertEquals(PaintFinish.Matte, result.finish)
        assertEquals(2, result.mixComponents.size)
        assertEquals(components, result.mixComponents)
    }

    @Test
    fun `createMixed computes hex from components`() {
        val components = listOf(
            MixComponent(
                id = UUID.randomUUID(),
                stableId = "white",
                name = "White",
                brand = "Brand",
                hex = "#FFFFFF",
                percentage = 50.0
            ),
            MixComponent(
                id = UUID.randomUUID(),
                stableId = "black",
                name = "Black",
                brand = "Brand",
                hex = "#000000",
                percentage = 50.0
            )
        )

        val result = factory.createMixed(
            name = "Gray Mix",
            brand = "Custom",
            components = components,
            type = null,
            finish = null,
            notes = null,
            uuidProvider = mockUuidProvider
        )

        // Hex should be computed from the mix, not be empty
        assertNotNull(result.hex)
        assertTrue(result.hex.matches(Regex("^#[0-9A-F]{6}$")))
        // Gray should be somewhere between white and black
        assertNotEquals("#FFFFFF", result.hex)
        assertNotEquals("#000000", result.hex)
    }

    @Test
    fun `createMixed handles single component`() {
        val components = listOf(
            MixComponent(
                id = UUID.randomUUID(),
                stableId = "citadel_ultramarine_blue",
                name = "Ultramarine Blue",
                brand = "Citadel",
                hex = "#0033CC",
                percentage = 100.0
            )
        )

        val result = factory.createMixed(
            name = "Pure Blue",
            brand = "Custom",
            components = components,
            type = PaintType.Base,
            finish = PaintFinish.Matte,
            notes = null,
            uuidProvider = mockUuidProvider
        )

        assertEquals(1, result.mixComponents.size)
        // With single component at 100%, hex should match component
        assertEquals("#0033CC", result.hex)
    }

    @Test
    fun `createMixed preserves component percentages`() {
        val components = listOf(
            MixComponent(
                id = UUID.randomUUID(),
                stableId = "red",
                name = "Red",
                brand = "Brand",
                hex = "#FF0000",
                percentage = 70.0
            ),
            MixComponent(
                id = UUID.randomUUID(),
                stableId = "blue",
                name = "Blue",
                brand = "Brand",
                hex = "#0000FF",
                percentage = 30.0
            )
        )

        val result = factory.createMixed(
            name = "Purple Mix",
            brand = "Custom",
            components = components,
            type = null,
            finish = null,
            notes = null,
            uuidProvider = mockUuidProvider
        )

        assertEquals(70.0, result.mixComponents[0].percentage, 0.001)
        assertEquals(30.0, result.mixComponents[1].percentage, 0.001)
    }

    // ============================================================================
    // Edge Cases
    // ============================================================================

    @Test
    fun `factory creates unique IDs for different paints`() {
        val custom1 = factory.createCustom(
            name = "Paint 1",
            brand = "Brand",
            hex = "#111111",
            type = null,
            finish = null,
            notes = null
        )

        val custom2 = factory.createCustom(
            name = "Paint 2",
            brand = "Brand",
            hex = "#222222",
            type = null,
            finish = null,
            notes = null
        )

        assertNotEquals(custom1.id, custom2.id)
        assertNotEquals(custom1.stableId, custom2.stableId)
    }

    @Test
    fun `factory sets dateAdded to current time`() {
        val before = java.time.Instant.now()

        val result = factory.createCustom(
            name = "Test",
            brand = "Test",
            hex = "#000000",
            type = null,
            finish = null,
            notes = null
        )

        val after = java.time.Instant.now()

        assertTrue(result.dateAdded >= before)
        assertTrue(result.dateAdded <= after)
    }
}
