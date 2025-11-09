package io.brushforge.brushforge.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides secure encryption keys for the Room database using Android Keystore.
 *
 * The database passphrase is:
 * 1. Generated once using SecureRandom
 * 2. Encrypted and stored in EncryptedSharedPreferences (backed by Android Keystore)
 * 3. Retrieved on subsequent app launches
 *
 * This ensures:
 * - Keys are hardware-backed when available (TEE/SE)
 * - Keys cannot be extracted from the device
 * - Keys are automatically deleted on app uninstall
 */
@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "brushforge_db_keys"
        private const val KEY_DATABASE_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_LENGTH = 32 // 256-bit key
    }

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false) // Don't require biometric for app launch
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Gets the database passphrase, generating it if it doesn't exist.
     * This should be called on a background thread as it may perform I/O.
     */
    fun getDatabasePassphrase(): ByteArray {
        val existingPassphrase = encryptedPrefs.getString(KEY_DATABASE_PASSPHRASE, null)

        return if (existingPassphrase != null) {
            // Decode existing passphrase from Base64
            android.util.Base64.decode(existingPassphrase, android.util.Base64.NO_WRAP)
        } else {
            // Generate new passphrase
            generateAndStorePassphrase()
        }
    }

    private fun generateAndStorePassphrase(): ByteArray {
        // Generate a cryptographically secure random passphrase
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        SecureRandom().nextBytes(passphrase)

        // Store it encrypted
        val base64Passphrase = android.util.Base64.encodeToString(passphrase, android.util.Base64.NO_WRAP)
        encryptedPrefs.edit()
            .putString(KEY_DATABASE_PASSPHRASE, base64Passphrase)
            .apply()

        return passphrase
    }

    /**
     * Clears the stored passphrase. Use with caution - this will make the existing
     * database unreadable and will require re-initialization.
     */
    fun clearPassphrase() {
        encryptedPrefs.edit()
            .remove(KEY_DATABASE_PASSPHRASE)
            .apply()
    }
}
