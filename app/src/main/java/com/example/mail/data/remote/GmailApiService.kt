package com.example.mail.data.remote

import com.example.mail.data.remote.dto.MessageDetailDto
import com.example.mail.data.remote.dto.MessageListResponse
import com.example.mail.data.remote.dto.ModifyMessageRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service for the Gmail REST API.
 * All endpoints accept a [userId]; the special value "me" (default) resolves
 * to the authenticated user, so call sites can stay generic.
 */
interface GmailApiService {

    /**
     * List message summaries matching an optional query.
     * Backed by `GET /gmail/v1/users/{userId}/messages`.
     *
     * @param userId    Gmail user id to scope the query to ("me" by default)
     * @param q         Gmail search query (e.g. "in:inbox newer_than:1d")
     * @param maxResults max number of results to return
     * @param pageToken  token for the next page of results
     */
    @GET("gmail/v1/users/{userId}/messages")
    suspend fun listMessages(
        @Path("userId") userId: String = "me",
        @Query("q") q: String? = null,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null
    ): MessageListResponse

    /**
     * Fetch a single message by its Gmail Message ID.
     * Backed by `GET /gmail/v1/users/{userId}/messages/{id}`.
     *
     * @param userId Gmail user id to scope the query to ("me" by default)
     * @param id     Gmail Message ID
     * @param format e.g. "full" to include headers and body parts.
     */
    @GET("gmail/v1/users/{userId}/messages/{id}")
    suspend fun getMessage(
        @Path("userId") userId: String = "me",
        @Path("id") id: String,
        @Query("format") format: String = "full"
    ): MessageDetailDto

    /**
     * Modify a message's labels. Backed by
     * `POST /gmail/v1/users/{userId}/messages/{id}/modify`.
     *
     * @param userId Gmail user id to scope the query to ("me" by default)
     * @param id     Gmail Message ID
     * @param request labels to add/remove (e.g. TRASH, INBOX)
     */
    @POST("gmail/v1/users/{userId}/messages/{id}/modify")
    suspend fun modifyMessage(
        @Path("userId") userId: String = "me",
        @Path("id") id: String,
        @Body request: ModifyMessageRequest
    ): MessageDetailDto
}
