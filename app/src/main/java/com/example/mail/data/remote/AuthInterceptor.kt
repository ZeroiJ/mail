package com.example.mail.data.remote

import android.util.Log
import com.example.mail.util.AuthManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authManager: AuthManager
) : Interceptor {

    private val tag = "AuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = authManager.getStoredAccessToken()

        val request = if (token.isNullOrBlank()) {
            Log.w(tag, "No access token — request sent without Authorization header")
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }
}
