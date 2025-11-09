package io.brushforge.brushforge.domain.usecase

import io.brushforge.brushforge.core.common.CoroutineDispatchers
import io.brushforge.brushforge.domain.repository.CatalogAssetProvider
import io.brushforge.brushforge.domain.repository.CatalogPaintRepository
import javax.inject.Inject
import kotlinx.coroutines.withContext

class SeedCatalogIfNeededUseCase @Inject constructor(
    private val catalogPaintRepository: CatalogPaintRepository,
    private val catalogAssetProvider: CatalogAssetProvider,
    private val dispatchers: CoroutineDispatchers
) {

    suspend operator fun invoke(force: Boolean = false): Result {
        val currentCount = catalogPaintRepository.count()
        if (!force && currentCount > 0) {
            return Result.AlreadySeeded(currentCount)
        }

        val loadResult = catalogAssetProvider.loadCatalogPaints()

        // If completely failed to load, return error with details
        if (loadResult.isCompleteFailure) {
            return Result.LoadFailed(loadResult.errors)
        }

        // If no paints loaded (different from load failure), return no data
        if (loadResult.paints.isEmpty()) {
            return Result.NoCatalogData
        }

        // If there were some errors but we got some paints, proceed with partial success
        // (Logging will be done by the data layer)

        withContext(dispatchers.io) {
            catalogPaintRepository.replaceCatalog(loadResult.paints)
        }

        return if (loadResult.hasErrors) {
            Result.PartiallySeeded(
                inserted = loadResult.paints.size,
                errors = loadResult.errors
            )
        } else {
            Result.Seeded(loadResult.paints.size)
        }
    }

    sealed interface Result {
        data class AlreadySeeded(val count: Int) : Result
        data class Seeded(val inserted: Int) : Result
        data class PartiallySeeded(val inserted: Int, val errors: List<String>) : Result
        data class LoadFailed(val errors: List<String>) : Result
        data object NoCatalogData : Result
    }
}
