package io.brushforge.brushforge.data.catalog

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.brushforge.brushforge.core.common.CoroutineDispatchers
import io.brushforge.brushforge.domain.model.CatalogPaint
import io.brushforge.brushforge.domain.repository.CatalogAssetProvider
import io.brushforge.brushforge.domain.repository.CatalogLoadResult
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.json.JSONException

@Singleton
class AssetCatalogProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: PaintCatalogParser,
    private val dispatchers: CoroutineDispatchers
) : CatalogAssetProvider {

    override suspend fun loadCatalogPaints(): CatalogLoadResult {
        return withContext(dispatchers.io) {
            val assetManager = context.assets
            val directory = PAINTS_ASSET_DIR

            val files = try {
                assetManager.list(directory)?.filter { it.endsWith(".json") } ?: emptyList()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to list catalog files in directory: $directory", e)
                return@withContext CatalogLoadResult(
                    paints = emptyList(),
                    errors = listOf("Unable to access paint catalog directory. Please reinstall the app.")
                )
            }

            if (files.isEmpty()) {
                Log.w(TAG, "No catalog files found in directory: $directory")
                return@withContext CatalogLoadResult(
                    paints = emptyList(),
                    errors = listOf("No paint catalog files found. Please reinstall the app.")
                )
            }

            val paints = mutableListOf<CatalogPaint>()
            val errors = mutableListOf<String>()

            files.sorted().forEach { fileName ->
                try {
                    val path = "$directory/$fileName"
                    val json = assetManager.open(path).bufferedReader().use { it.readText() }
                    val parsedPaints = parser.parse(json, fileName.removeSuffix(".json"))
                    paints += parsedPaints
                    Log.d(TAG, "Successfully loaded ${parsedPaints.size} paints from $fileName")
                } catch (e: FileNotFoundException) {
                    val errorMsg = "File not found: $fileName"
                    Log.e(TAG, errorMsg, e)
                    errors.add(errorMsg)
                    // Continue loading other files
                } catch (e: IOException) {
                    val errorMsg = "Failed to read file: $fileName"
                    Log.e(TAG, errorMsg, e)
                    errors.add(errorMsg)
                    // Continue loading other files
                } catch (e: JSONException) {
                    val errorMsg = "Failed to parse file: $fileName (corrupted data)"
                    Log.e(TAG, errorMsg, e)
                    errors.add(errorMsg)
                    // Continue loading other files
                } catch (e: Exception) {
                    val errorMsg = "Unexpected error loading file: $fileName"
                    Log.e(TAG, errorMsg, e)
                    errors.add(errorMsg)
                    // Continue loading other files
                }
            }

            if (paints.isEmpty() && errors.isNotEmpty()) {
                Log.e(TAG, "Failed to load any paints. All ${files.size} files failed.")
            } else if (errors.isNotEmpty()) {
                Log.w(TAG, "Loaded ${paints.size} paints with ${errors.size} errors")
            } else {
                Log.i(TAG, "Successfully loaded ${paints.size} paints from ${files.size} files")
            }

            CatalogLoadResult(paints = paints, errors = errors)
        }
    }

    companion object {
        private const val TAG = "AssetCatalogProvider"
        private const val PAINTS_ASSET_DIR = "paints"
    }
}
