package com.example.mail.data.remote

import com.example.mail.data.remote.dto.MessageDetailDto
import com.example.mail.data.remote.dto.MessageListResponse
import com.example.mail.data.remote.dto.ModifyMessageRequest
import com.example.mail.data.remote.dto.SendMessageRequest
import com.example.mail.data.remote.dto.DraftDto
import com.example.mail.data.remote.dto.CreateDraftRequest
import com.example.mail.data.remote.dto.LabelListResponse
import com.example.mail.data.remote.dto.GmailLabelDto
import com.example.mail.data.remote.dto.CreateLabelRequest
import com.example.mail.data.remote.dto.PatchLabelRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    /**
     * Send an email. Backed by `POST /gmail/v1/users/{userId}/messages/send`.
     *
     * @param request base64url-encoded RFC822 message in `raw`.
     */
    @POST("gmail/v1/users/{userId}/messages/send")
    suspend fun sendMessage(
        @Path("userId") userId: String = "me",
        @Body request: SendMessageRequest
    ): MessageDetailDto

    /**
     * Create a server-side draft. Backed by
     * `POST /gmail/v1/users/{userId}/drafts`.
     */
    @POST("gmail/v1/users/{userId}/drafts")
    suspend fun createDraft(
        @Path("userId") userId: String = "me",
        @Body request: CreateDraftRequest
    ): DraftDto

    /**
     * Delete a server-side draft. Backed by
     * `DELETE /gmail/v1/users/{userId}/drafts/{id}`.
     */
    @DELETE("gmail/v1/users/{userId}/drafts/{id}")
    suspend fun deleteDraft(
        @Path("userId") userId: String = "me",
        @Path("id") id: String
    )

    /**
     * Send an existing draft. Backed by
     * `POST /gmail/v1/users/{userId}/drafts/send`.
     */
    @POST("gmail/v1/users/{userId}/drafts/send")
    suspend fun sendDraft(
        @Path("userId") userId: String = "me",
        @Body request: Map<String, String>
    ): MessageDetailDto

    @GET("gmail/v1/users/{userId}/labels")
    suspend fun listLabels(
        @Path("userId") userId: String = "me"
    ): LabelListResponse

    @POST("gmail/v1/users/{userId}/labels")
    suspend fun createLabel(
        @Path("userId") userId: String = "me",
        @Body request: CreateLabelRequest
    ): GmailLabelDto

    @PATCH("gmail/v1/users/{userId}/labels/{id}")
    suspend fun patchLabel(
        @Path("userId") userId: String = "me",
        @Path("id") id: String,
        @Body request: PatchLabelRequest
    ): GmailLabelDto

    @DELETE("gmail/v1/users/{userId}/labels/{id}")
    suspend fun deleteLabel(
        @Path("userId") userId: String = "me",
        @Path("id") id: String
    )
}
