package com.example.mail.util.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import net.sqlcipher.database.SupportFactory
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Owns the SQLCipher master key for the Room database.
 *
 * The key is a 256-bit AES key generated once and held exclusively in the
 * Android Keystore (hardware-backed where available). It is deliberately NOT
 * bound to user authentication ([KeyGenParameterSpec.Builder.setUserAuthenticationRequired]
 * left as false) so that adding/removing a fingerprint or switching the
 * device credential never invalidates the key and silently bricks the inbox.
 * The biometric gate in MainActivity is a separate UI layer on top of the
 * encrypted store.
 */
object CryptoManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "mail_db_master_key"
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val KEY_SIZE_BITS = 256

    private const val TAG = "CryptoManager"

    /**
     * Returns a [SupportFactory] for Room, creating the Keystore key on first
     * use. If the key was invalidated or the database cannot be opened, the
     * corrupted database file is deleted so a fresh (re-synced) database can
     * be recreated on the next open instead of crashing on every launch.
     */
    fun getOrCreateSupportFactory(context: Context): SupportFactory {
        val passphrase = getOrCreateKey().encoded
        return try {
            SupportFactory(passphrase)
        } catch (e: Exception) {
            Log.w(TAG, "DB open failed; deleting DB and re-creating", e)
            deleteDatabase(context)
            SupportFactory(getOrCreateKey().encoded)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE_BITS)
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .build()
        return KeyGenerator.getInstance(ALGORITHM, KEYSTORE_PROVIDER)
            .apply { init(spec) }
            .generateKey()
    }

    private fun deleteDatabase(context: Context) {
        val dbFile = File(context.applicationContext.getDatabasePath("mail.db").path)
        if (dbFile.exists()) dbFile.delete()
        // Remove SQLCipher sidecar files (journal, wal, shm).
        for (suffix in listOf("-journal", "-wal", "-shm")) {
            val sidecar = File(dbFile.path + suffix)
            if (sidecar.exists()) sidecar.delete()
        }
    }
}