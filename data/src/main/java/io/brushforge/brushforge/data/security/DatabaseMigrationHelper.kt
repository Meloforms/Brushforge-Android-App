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
     * 1. Check if unencrypted DB exists
     * 2. If yes, encrypt it using the provided passphrase
     * 3. Delete unencrypted DB
     * 4. Rename encrypted DB to original name
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
        val unencryptedDb = context.getDatabasePath(databaseName)

        // If unencrypted database doesn't exist, no migration needed
        if (!unencryptedDb.exists()) {
            Log.d(TAG, "No unencrypted database found, skipping migration")
            return false
        }

        try {
            Log.i(TAG, "Starting database encryption migration")

            val encryptedDbPath = File(context.getDatabasePath(databaseName).parent, "${databaseName}-encrypted")

            // Open the unencrypted database
            val db = SQLiteDatabase.openOrCreateDatabase(
                unencryptedDb.absolutePath,
                "",  // Empty passphrase for unencrypted
                null,
                null
            )

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
            if (unencryptedDb.delete()) {
                Log.d(TAG, "Deleted unencrypted database")
            }

            // Also delete WAL and SHM files if they exist
            File(context.getDatabasePath("$databaseName-wal").absolutePath).delete()
            File(context.getDatabasePath("$databaseName-shm").absolutePath).delete()

            // Rename encrypted database to original name
            if (encryptedDbPath.renameTo(unencryptedDb)) {
                Log.i(TAG, "Database encryption migration completed successfully")
                return true
            } else {
                Log.e(TAG, "Failed to rename encrypted database")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during database encryption migration", e)
            // On error, keep the unencrypted database so user data isn't lost
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
