package io.brushforge.brushforge.domain.repository

import io.brushforge.brushforge.domain.model.CatalogPaint

interface CatalogAssetProvider {
    suspend fun loadCatalogPaints(): CatalogLoadResult
}

/**
 * Result of loading the paint catalog from asset files.
 *
 * @param paints Successfully loaded paints (may be empty if all files failed)
 * @param errors List of error messages for files that failed to load
 */
data class CatalogLoadResult(
    val paints: List<CatalogPaint>,
    val errors: List<String>
) {
    /**
     * Returns true if at least some paints were loaded successfully.
     */
    val hasAnyPaints: Boolean get() = paints.isNotEmpty()

    /**
     * Returns true if there were any errors during loading.
     */
    val hasErrors: Boolean get() = errors.isNotEmpty()

    /**
     * Returns true if loading was completely successful (no errors).
     */
    val isFullSuccess: Boolean get() = paints.isNotEmpty() && errors.isEmpty()

    /**
     * Returns true if loading completely failed (no paints loaded).
     */
    val isCompleteFailure: Boolean get() = paints.isEmpty()
}
