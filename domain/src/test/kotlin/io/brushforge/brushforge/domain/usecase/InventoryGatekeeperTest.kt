package io.brushforge.brushforge.domain.usecase

import io.brushforge.brushforge.domain.repository.UserPaintRepository
import io.brushforge.brushforge.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for InventoryGatekeeper to ensure inventory limits are enforced correctly.
 *
 * Critical business logic tests:
 * - Inventory limit enforcement prevents database bloat
 * - Proper capacity checking before adding paints
 * - Correct status reporting for UI feedback
 */
class InventoryGatekeeperTest {

    private lateinit var gatekeeper: InventoryGatekeeper
    private lateinit var mockPaintRepository: UserPaintRepository
    private lateinit var mockPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        mockPaintRepository = mockk()
        mockPreferencesRepository = mockk()
        gatekeeper = InventoryGatekeeper(mockPaintRepository, mockPreferencesRepository)
    }

    // ============================================================================
    // Allowed Cases
    // ============================================================================

    @Test
    fun `verifyCapacity returns Allowed when under limit`() = runTest {
        // Given: 50 paints owned, limit is 100
        coEvery { mockPaintRepository.getInventoryCount() } returns 50
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        val result = gatekeeper.verifyCapacity()

        // Then
        assertTrue(result is InventoryGatekeeper.Result.Allowed)
        assertEquals(100, result.limit)
        assertEquals(50, result.current)
    }

    @Test
    fun `verifyCapacity returns Allowed when exactly one below limit`() = runTest {
        // Given: 99 paints owned, limit is 100
        coEvery { mockPaintRepository.getInventoryCount() } returns 99
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        val result = gatekeeper.verifyCapacity()

        // Then
        assertTrue(result is InventoryGatekeeper.Result.Allowed)
        assertEquals(100, result.limit)
        assertEquals(99, result.current)
    }

    @Test
    fun `verifyCapacity returns Allowed when inventory is empty`() = runTest {
        // Given: 0 paints owned, limit is 100
        coEvery { mockPaintRepository.getInventoryCount() } returns 0
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        val result = gatekeeper.verifyCapacity()

        // Then
        assertTrue(result is InventoryGatekeeper.Result.Allowed)
        assertEquals(100, result.limit)
        assertEquals(0, result.current)
    }

    @Test
    fun `verifyCapacity returns Allowed with small limit`() = runTest {
        // Given: 5 paints owned, limit is 10
        coEvery { mockPaintRepository.getInventoryCount() } returns 5
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 10

        // When
        val result = gatekeeper.verifyCapacity()

        // Then
        assertTrue(result is InventoryGatekeeper.Result.Allowed)
        assertEquals(10, result.limit)
        assertEquals(5, result.current)
    }

    // ============================================================================
    // LimitReached Cases
    // ============================================================================

    @Test
    fun `verifyCapacity returns LimitReached when at limit`() = runTest {
        // Given: 100 paints owned, limit is 100
        coEvery { mockPaintRepository.getInventoryCount() } returns 100
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        val result = gatekeeper.verifyCapacity()

        // Then
        assertTrue(result is InventoryGatekeeper.Result.LimitReached)
        assertEquals(100, result.limit)
        assertEquals(100, result.current)
    }

    @Test
    fun `verifyCapacity returns LimitReached when over limit`() = runTest {
        // Given: 150 paints owned, limit is 100 (can happen if limit was reduced)
        coEvery { mockPaintRepository.getInventoryCount() } returns 150
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        val result = gatekeeper.verifyCapacity()

        // Then
        assertTrue(result is InventoryGatekeeper.Result.LimitReached)
        assertEquals(100, result.limit)
        assertEquals(150, result.current)
    }

    @Test
    fun `verifyCapacity handles limit of zero`() = runTest {
        // Given: 0 paints owned, limit is 0 (edge case)
        coEvery { mockPaintRepository.getInventoryCount() } returns 0
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 0

        // When
        val result = gatekeeper.verifyCapacity()

        // Then
        assertTrue(result is InventoryGatekeeper.Result.LimitReached)
        assertEquals(0, result.limit)
        assertEquals(0, result.current)
    }

    @Test
    fun `verifyCapacity handles exactly one paint at limit of one`() = runTest {
        // Given: 1 paint owned, limit is 1
        coEvery { mockPaintRepository.getInventoryCount() } returns 1
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 1

        // When
        val result = gatekeeper.verifyCapacity()

        // Then
        assertTrue(result is InventoryGatekeeper.Result.LimitReached)
        assertEquals(1, result.limit)
        assertEquals(1, result.current)
    }

    // ============================================================================
    // Edge Cases
    // ============================================================================

    @Test
    fun `verifyCapacity handles large numbers`() = runTest {
        // Given: 9999 paints owned, limit is 10000
        coEvery { mockPaintRepository.getInventoryCount() } returns 9999
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 10000

        // When
        val result = gatekeeper.verifyCapacity()

        // Then
        assertTrue(result is InventoryGatekeeper.Result.Allowed)
        assertEquals(10000, result.limit)
        assertEquals(9999, result.current)
    }

    @Test
    fun `verifyCapacity Result interface provides limit and current`() = runTest {
        // Given
        coEvery { mockPaintRepository.getInventoryCount() } returns 75
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        val result: InventoryGatekeeper.Result = gatekeeper.verifyCapacity()

        // Then: Can access limit and current through interface
        assertEquals(100, result.limit)
        assertEquals(75, result.current)
    }

    @Test
    fun `verifyCapacity calls repository methods once`() = runTest {
        // Given
        coEvery { mockPaintRepository.getInventoryCount() } returns 50
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        gatekeeper.verifyCapacity()

        // Then
        coVerify(exactly = 1) { mockPaintRepository.getInventoryCount() }
        coVerify(exactly = 1) { mockPreferencesRepository.getMaxInventorySize() }
    }

    // ============================================================================
    // Real-World Scenarios
    // ============================================================================

    @Test
    fun `scenario - user near limit can still add`() = runTest {
        // Given: User has 95 paints, limit is 100
        coEvery { mockPaintRepository.getInventoryCount() } returns 95
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        val result = gatekeeper.verifyCapacity()

        // Then: Should allow adding more paints
        assertTrue(result is InventoryGatekeeper.Result.Allowed)
        // User has 5 slots remaining
        assertEquals(5, result.limit - result.current)
    }

    @Test
    fun `scenario - user reduced limit but already over`() = runTest {
        // Given: User had 150 paints, then reduced limit to 100
        coEvery { mockPaintRepository.getInventoryCount() } returns 150
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        val result = gatekeeper.verifyCapacity()

        // Then: Should be blocked from adding more
        assertTrue(result is InventoryGatekeeper.Result.LimitReached)
        // Over by 50
        assertEquals(50, result.current - result.limit)
    }

    @Test
    fun `scenario - fresh install with default limit`() = runTest {
        // Given: New user, 0 paints, default limit of 100
        coEvery { mockPaintRepository.getInventoryCount() } returns 0
        coEvery { mockPreferencesRepository.getMaxInventorySize() } returns 100

        // When
        val result = gatekeeper.verifyCapacity()

        // Then: Should allow adding paints
        assertTrue(result is InventoryGatekeeper.Result.Allowed)
        assertEquals(100, result.limit)
        assertEquals(0, result.current)
    }
}
