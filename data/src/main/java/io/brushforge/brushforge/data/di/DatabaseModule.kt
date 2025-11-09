package io.brushforge.brushforge.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.brushforge.brushforge.data.security.DatabaseKeyProvider
import io.brushforge.brushforge.data.security.DatabaseMigrationHelper
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.brushforge.brushforge.data.database.BrushforgeDatabase
import io.brushforge.brushforge.data.database.dao.CatalogPaintDao
import io.brushforge.brushforge.data.database.dao.UserPaintDao
import io.brushforge.brushforge.data.preferences.UserPreferencesRepositoryImpl
import io.brushforge.brushforge.data.catalog.AssetCatalogProvider
import io.brushforge.brushforge.data.repository.CatalogPaintRepositoryImpl
import io.brushforge.brushforge.data.repository.UserPaintRepositoryImpl
import io.brushforge.brushforge.domain.repository.CatalogAssetProvider
import io.brushforge.brushforge.domain.repository.CatalogPaintRepository
import io.brushforge.brushforge.domain.repository.UserPaintRepository
import io.brushforge.brushforge.domain.repository.UserPreferencesRepository
import javax.inject.Singleton

private const val DATABASE_NAME = "brushforge.db"
private const val DATA_STORE_NAME = "brushforge_prefs"

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add indices for better query performance
        // UserPaintEntity indices
        db.execSQL("CREATE INDEX IF NOT EXISTS index_user_paints_stable_id ON user_paints(stable_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_user_paints_is_owned ON user_paints(is_owned)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_user_paints_is_wishlist ON user_paints(is_wishlist)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_user_paints_brand ON user_paints(brand)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_user_paints_type ON user_paints(type)")

        // CatalogPaintEntity indices
        db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_paints_brand ON catalog_paints(brand)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_paints_type ON catalog_paints(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_paints_finish ON catalog_paints(finish)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_paints_name ON catalog_paints(name)")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop and recreate catalog_paints table with new schema including line_variant
        // This is safe because catalog data is replaceable and will be re-seeded from assets
        // User data (user_paints table) is preserved
        db.execSQL("DROP TABLE IF EXISTS catalog_paints")

        db.execSQL("""
            CREATE TABLE catalog_paints (
                stable_id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                code TEXT,
                brand TEXT NOT NULL,
                line TEXT,
                line_variant TEXT,
                hex TEXT NOT NULL,
                red INTEGER NOT NULL,
                green INTEGER NOT NULL,
                blue INTEGER NOT NULL,
                type TEXT NOT NULL,
                finish TEXT NOT NULL,
                tags TEXT NOT NULL,
                lab_l REAL NOT NULL,
                lab_a REAL NOT NULL,
                lab_b REAL NOT NULL
            )
        """.trimIndent())

        // Recreate indices
        db.execSQL("CREATE INDEX index_catalog_paints_brand ON catalog_paints(brand)")
        db.execSQL("CREATE INDEX index_catalog_paints_type ON catalog_paints(type)")
        db.execSQL("CREATE INDEX index_catalog_paints_finish ON catalog_paints(finish)")
        db.execSQL("CREATE INDEX index_catalog_paints_name ON catalog_paints(name)")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create recipes table
        db.execSQL("""
            CREATE TABLE recipes (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                dateCreated INTEGER NOT NULL,
                dateModified INTEGER NOT NULL,
                isFavorite INTEGER NOT NULL DEFAULT 0,
                notes TEXT,
                referenceImageUri TEXT,
                tags TEXT NOT NULL DEFAULT '[]'
            )
        """.trimIndent())

        // Create recipe_steps table with foreign key cascade
        db.execSQL("""
            CREATE TABLE recipe_steps (
                id TEXT PRIMARY KEY NOT NULL,
                recipeId TEXT NOT NULL,
                stepIndex INTEGER NOT NULL,
                paintName TEXT NOT NULL,
                paintBrand TEXT NOT NULL,
                paintHex TEXT NOT NULL,
                paintStableId TEXT,
                notes TEXT,
                FOREIGN KEY (recipeId) REFERENCES recipes(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create indices for recipe_steps for efficient queries
        db.execSQL("CREATE INDEX index_recipe_steps_recipeId ON recipe_steps(recipeId)")
        db.execSQL("CREATE INDEX index_recipe_steps_stepIndex ON recipe_steps(stepIndex)")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add line column to user_paints table to preserve product line information
        // when catalog paints are added to user's collection
        db.execSQL("ALTER TABLE user_paints ADD COLUMN line TEXT")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider
    ): BrushforgeDatabase {
        // Initialize SQLCipher
        SQLiteDatabase.loadLibs(context)

        // Get the encryption passphrase
        val passphrase = keyProvider.getDatabasePassphrase()

        // Migrate existing unencrypted database to encrypted format (one-time operation)
        DatabaseMigrationHelper.migrateToEncrypted(context, DATABASE_NAME, passphrase)

        // Create encrypted database factory
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            BrushforgeDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideCatalogDao(database: BrushforgeDatabase): CatalogPaintDao = database.catalogPaintDao()

    @Provides
    fun provideUserPaintDao(database: BrushforgeDatabase): UserPaintDao = database.userPaintDao()

    @Provides
    fun provideRecipeDao(database: BrushforgeDatabase): io.brushforge.brushforge.data.database.dao.RecipeDao = database.recipeDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(DATA_STORE_NAME)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindCatalogPaintRepository(
        impl: CatalogPaintRepositoryImpl
    ): CatalogPaintRepository

    @Binds
    abstract fun bindCatalogAssetProvider(
        impl: AssetCatalogProvider
    ): CatalogAssetProvider

    @Binds
    abstract fun bindUserPaintRepository(
        impl: UserPaintRepositoryImpl
    ): UserPaintRepository

    @Binds
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository

    @Binds
    abstract fun bindRecipeRepository(
        impl: io.brushforge.brushforge.data.repository.RecipeRepositoryImpl
    ): io.brushforge.brushforge.domain.repository.RecipeRepository
}
