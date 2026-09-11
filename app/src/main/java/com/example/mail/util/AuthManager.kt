package com.example.mail.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "AuthManager"

    companion object {
        private const val PREFS_NAME = "mail_auth_secure"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCOUNT_NAME = "account_name"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"

        private const val WEB_CLIENT_ID = "808026645220-inmpi32m9s69e8h04ur0jk6ktm5396k2.apps.googleusercontent.com"

        private val GMAIL_SCOPES = setOf(
            Scope("https://www.googleapis.com/auth/gmail.modify")
        )
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val encryptedPrefs: SharedPreferences by lazy {
        createEncryptedPrefs()
    }

    init {
        val cached = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
        val accountName = encryptedPrefs.getString(KEY_ACCOUNT_NAME, null)
        val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)

        if (cached != null && accountName != null && expiry > System.currentTimeMillis()) {
            _authState.value = AuthState.Authenticated(accountName)
        } else if (cached != null) {
            _authState.value = AuthState.TokenExpired(accountName.orEmpty())
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    // -----------------------------------------------------------------------
    // Credential Manager — launch bottom-sheet account picker
    // -----------------------------------------------------------------------

    /**
     * Launches the Google sign-in flow.
     *
     * [credentialManager.getCredential] MUST run on the main thread — wrapping
     * it in `withContext(Dispatchers.IO)` throws IllegalStateException and the
     * bottom sheet silently fails. Only the token exchange moves off-thread.
     */
    suspend fun signIn(activity: android.app.Activity): Result<String> {
        try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val accountName = googleIdTokenCredential.id

                val accessToken = fetchAccessToken(activity, accountName)
                if (accessToken != null) {
                    _authState.value = AuthState.Authenticated(accountName)
                    return Result.success(accountName)
                } else {
                    return Result.failure(Exception("Failed to obtain access token"))
                }
            } else {
                return Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialException) {
            Log.e(tag, "Credential Manager failed: ${e.message}")
            return Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Sign-in failed: ${e.message}")
            return Result.failure(e)
        }
    }

    // -----------------------------------------------------------------------
    // GoogleAuthUtil — fetch OAuth token with gmail.modify scope
    // -----------------------------------------------------------------------

    private suspend fun fetchAccessToken(
        activity: android.app.Activity,
        accountName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val token = GoogleAuthUtil.getToken(
                context,
                accountName,
                "oauth2:https://www.googleapis.com/auth/gmail.modify"
            )

            storeToken(accountName, token, System.currentTimeMillis() + 3600_000L)
            Log.i(tag, "Access token obtained for $accountName")
            token
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            // First-time OAuth consent for gmail.modify — launch the recovery intent
            Log.w(tag, "User consent required for ${e.message}")
            activity.runOnUiThread { activity.startActivity(e.intent) }
            null
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch access token: ${e.message}")
            null
        }
    }

    // -----------------------------------------------------------------------
    // Token storage — EncryptedSharedPreferences
    // -----------------------------------------------------------------------

    fun getStoredAccessToken(): String? {
        val token = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
        val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)

        return if (token != null && expiry > System.currentTimeMillis()) {
            token
        } else {
            null
        }
    }

    fun getStoredAccountName(): String? {
        return encryptedPrefs.getString(KEY_ACCOUNT_NAME, null)
    }

    private fun storeToken(accountName: String, accessToken: String, expiry: Long) {
        encryptedPrefs.edit()
            .putString(KEY_ACCOUNT_NAME, accountName)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putLong(KEY_TOKEN_EXPIRY, expiry)
            .apply()
    }

    fun signOut() {
        encryptedPrefs.edit().clear().apply()
        _authState.value = AuthState.SignedOut
        Log.i(tag, "User signed out")
    }

    fun isSignedIn(): Boolean {
        return getStoredAccessToken() != null
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()

            androidx.security.crypto.EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to create encrypted prefs: ${e.message}")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}

sealed class AuthState {
    data object Unknown : AuthState()
    data object SignedOut : AuthState()
    data class Authenticated(val accountName: String) : AuthState()
    data class TokenExpired(val accountName: String) : AuthState()
}
