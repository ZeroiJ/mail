package com.example.mail.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches an OAuth2 Bearer token to every request. The token is resolved
 * lazily from [tokenProvider] on each call so a refreshed token is always
 * used without rebuilding the client.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()

        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }
}
