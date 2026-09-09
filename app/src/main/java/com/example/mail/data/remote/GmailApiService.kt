package com.example.mail.data.remote

import com.example.mail.data.remote.dto.MessageDetailDto
import com.example.mail.data.remote.dto.MessageListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service for the Gmail REST API.
 * All endpoints are scoped to the authenticated user ("me").
 */
interface GmailApiService {

    /**
     * List message summaries matching an optional query.
     * Backed by `GET /gmail/v1/users/me/messages`.
     *
     * @param q       Gmail search query (e.g. "in:inbox newer_than:1d")
     * @param maxResults max number of results to return
     * @param pageToken  token for the next page of results
     */
    @GET("gmail/v1/users/me/messages")
    suspend fun listMessages(
        @Query("q") q: String? = null,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null
    ): MessageListResponse

    /**
     * Fetch a single message by its Gmail Message ID.
     * Backed by `GET /gmail/v1/users/me/messages/{id}`.
     *
     * @param format e.g. "full" to include headers and body parts.
     */
    @GET("gmail/v1/users/me/messages/{id}")
    suspend fun getMessage(
        @Path("id") id: String,
        @Query("format") format: String = "full"
    ): MessageDetailDto
}
