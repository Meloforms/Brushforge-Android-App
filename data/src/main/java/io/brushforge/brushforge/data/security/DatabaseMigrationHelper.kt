package io.brushforge.brushforge.data.security

import android.content.Context
import android.util.Log
import net.sqlcipher.database.SQLiteDatabase
import java.io.File

/**
 * Helper for migrating an unencrypted database to an encrypted one.
 *
 * This handles the one-time migration when encryption is first enabled.
 */
object DatabaseMigrationHelper {
    private const val TAG = "DatabaseMigration"

    /**
     * Migrates an unencrypted database to an encrypted one.
     *
     * Process:
     * 1. Check if database exists
     * 2. Check if it's already encrypted
     * 3. If unencrypted, encrypt it using the provided passphrase
     * 4. Delete unencrypted DB
     * 5. Rename encrypted DB to original name
     *
     * @param context Application context
     * @param databaseName Name of the database file
     * @param passphrase Encryption passphrase
     * @return true if migration was performed, false if not needed
     */
    fun migrateToEncrypted(
        context: Context,
        databaseName: String,
        passphrase: ByteArray
    ): Boolean {
        val dbFile = context.getDatabasePath(databaseName)

        // If database doesn't exist, no migration needed
        if (!dbFile.exists()) {
            Log.d(TAG, "No database found, skipping migration")
            return false
        }

        // Check if database is already encrypted by trying to open with passphrase
        try {
            val testDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                String(passphrase, Charsets.UTF_8).toCharArray(),
                null,
                SQLiteDatabase.OPEN_READONLY,
                null
            )
            testDb.close()
            Log.d(TAG, "Database is already encrypted, skipping migration")
            return false  // Already encrypted, no migration needed
        } catch (e: Exception) {
            // Database is not encrypted or corrupted, attempt migration
            Log.d(TAG, "Database is not encrypted, proceeding with migration")
        }

        try {
            Log.i(TAG, "Starting database encryption migration")

            val encryptedDbPath = File(context.getDatabasePath(databaseName).parent, "${databaseName}-encrypted")

            // Delete any existing encrypted database file from previous failed attempts
            if (encryptedDbPath.exists()) {
                Log.d(TAG, "Deleting existing encrypted database file from previous attempt")
                encryptedDbPath.delete()
            }

            // Try to open the unencrypted database
            val db = try {
                SQLiteDatabase.openOrCreateDatabase(
                    dbFile.absolutePath,
                    "",  // Empty passphrase for unencrypted
                    null,
                    null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Cannot open database - it may be corrupted. Deleting and starting fresh.", e)
                // Delete corrupted database and its files
                dbFile.delete()
                File(context.getDatabasePath("$databaseName-wal").absolutePath).delete()
                File(context.getDatabasePath("$databaseName-shm").absolutePath).delete()
                return false
            }

            // Export to encrypted database
            db.rawExecSQL("ATTACH DATABASE '${encryptedDbPath.absolutePath}' AS encrypted KEY x'${passphrase.toHexString()}';")
            db.rawExecSQL("SELECT sqlcipher_export('encrypted');")
            db.rawExecSQL("DETACH DATABASE encrypted;")
            db.close()

            // Verify the encrypted database works
            val encryptedDb = SQLiteDatabase.openDatabase(
                encryptedDbPath.absolutePath,
                String(passphrase, Charsets.UTF_8).toCharArray(),
                null,
                SQLiteDatabase.OPEN_READONLY,
                null
            )
            encryptedDb.close()

            // Delete the unencrypted database
            if (dbFile.delete()) {
                Log.d(TAG, "Deleted unencrypted database")
            }

            // Also delete WAL and SHM files if they exist
            File(context.getDatabasePath("$databaseName-wal").absolutePath).delete()
            File(context.getDatabasePath("$databaseName-shm").absolutePath).delete()

            // Rename encrypted database to original name
            if (encryptedDbPath.renameTo(dbFile)) {
                Log.i(TAG, "Database encryption migration completed successfully")
                return true
            } else {
                Log.e(TAG, "Failed to rename encrypted database")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during database encryption migration", e)
            // On error, try to clean up and let Room recreate the database
            try {
                dbFile.delete()
                File(context.getDatabasePath("$databaseName-wal").absolutePath).delete()
                File(context.getDatabasePath("$databaseName-shm").absolutePath).delete()
                File(context.getDatabasePath("${databaseName}-encrypted").absolutePath).delete()
                Log.w(TAG, "Cleaned up database files - app will recreate encrypted database on next launch")
            } catch (cleanupException: Exception) {
                Log.e(TAG, "Error cleaning up database files", cleanupException)
            }
            return false
        }
    }

    /**
     * Converts byte array to hex string for SQLCipher
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}
