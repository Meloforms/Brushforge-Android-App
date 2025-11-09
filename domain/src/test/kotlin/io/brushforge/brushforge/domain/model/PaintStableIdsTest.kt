package io.brushforge.brushforge.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for PaintStableIds to ensure stable ID generation is correct.
 *
 * Stable IDs are critical for:
 * - Identifying user-created paints across app restarts
 * - Distinguishing custom vs mixed paints
 * - Maintaining paint references in recipes
 */
class PaintStableIdsTest {

    @Test
    fun `custom generates stable ID with correct prefix`() {
        val mockUuid = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val mockProvider = UUIDStringProvider { mockUuid }

        val (stableId, id) = PaintStableIds.custom(mockProvider)

        assertEquals("user_custom_12345678-1234-1234-1234-123456789abc", stableId)
        assertEquals(mockUuid, id)
    }

    @Test
    fun `mix generates stable ID with correct prefix`() {
        val mockUuid = UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890")
        val mockProvider = UUIDStringProvider { mockUuid }

        val (stableId, id) = PaintStableIds.mix(mockProvider)

        assertEquals("user_mix_abcdef12-3456-7890-abcd-ef1234567890", stableId)
        assertEquals(mockUuid, id)
    }

    @Test
    fun `custom generates lowercase stable IDs`() {
        val mockUuid = UUID.fromString("AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE")
        val mockProvider = UUIDStringProvider { mockUuid }

        val (stableId, _) = PaintStableIds.custom(mockProvider)

        // UUID should be lowercase
        assertTrue(stableId.contains("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
        assertFalse(stableId.contains("AAAA"))
    }

    @Test
    fun `mix generates lowercase stable IDs`() {
        val mockUuid = UUID.fromString("FFFFFFFF-AAAA-BBBB-CCCC-DDDDDDDDDDDD")
        val mockProvider = UUIDStringProvider { mockUuid }

        val (stableId, _) = PaintStableIds.mix(mockProvider)

        // Should be lowercase
        assertEquals(stableId, stableId.lowercase())
    }

    @Test
    fun `custom and mix use different prefixes`() {
        val mockUuid = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val mockProvider = UUIDStringProvider { mockUuid }

        val (customStableId, _) = PaintStableIds.custom(mockProvider)
        val (mixStableId, _) = PaintStableIds.mix(mockProvider)

        assertTrue(customStableId.startsWith(PaintStableIds.CUSTOM_PREFIX))
        assertTrue(mixStableId.startsWith(PaintStableIds.MIX_PREFIX))
        assertNotEquals(customStableId, mixStableId)
    }

    @Test
    fun `custom returns consistent pair`() {
        val mockUuid = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val mockProvider = UUIDStringProvider { mockUuid }

        val (stableId, id) = PaintStableIds.custom(mockProvider)

        // Stable ID should contain the UUID
        assertTrue(stableId.contains(id.toString().lowercase()))
    }

    @Test
    fun `mix returns consistent pair`() {
        val mockUuid = UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890")
        val mockProvider = UUIDStringProvider { mockUuid }

        val (stableId, id) = PaintStableIds.mix(mockProvider)

        // Stable ID should contain the UUID
        assertTrue(stableId.contains(id.toString().lowercase()))
    }

    @Test
    fun `UUIDStringProvider default generates random UUIDs`() {
        val provider = UUIDStringProvider()

        val uuid1 = provider.generate()
        val uuid2 = provider.generate()

        assertNotEquals(uuid1, uuid2)
    }

    @Test
    fun `UUIDStringProvider generates valid UUIDs`() {
        val provider = UUIDStringProvider()

        val uuid = provider.generate()

        // Should be a valid UUID
        assertNotNull(uuid)
        // UUID string format is valid
        val uuidString = uuid.toString()
        assertTrue(uuidString.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
    }

    @Test
    fun `different invocations generate unique stable IDs`() {
        val (stableId1, id1) = PaintStableIds.custom()
        val (stableId2, id2) = PaintStableIds.custom()

        assertNotEquals(stableId1, stableId2)
        assertNotEquals(id1, id2)
    }

    @Test
    fun `prefix constants are correct`() {
        assertEquals("user_custom_", PaintStableIds.CUSTOM_PREFIX)
        assertEquals("user_mix_", PaintStableIds.MIX_PREFIX)
    }
}
