package com.example.mail.data.remote.dto

import kotlinx.serialization.Serializable

/** Response of `GET /gmail/v1/users/me/messages`. */
@Serializable
data class MessageListResponse(
    val messages: List<MessageSummaryDto>? = null,
    val nextPageToken: String? = null,
    val resultSizeEstimate: Int = 0
)

/** Lightweight summary object returned in a message list. */
@Serializable
data class MessageSummaryDto(
    val id: String,
    val threadId: String
)

/** Response of `GET /gmail/v1/users/me/messages/{id}`. */
@Serializable
data class MessageDetailDto(
    val id: String,
    val threadId: String? = null,
    val labelIds: List<String>? = null,
    val snippet: String? = null,
    val internalDate: String? = null, // microseconds since epoch
    val payload: MessagePartDto? = null
)

/** A single MIME part of a message. */
@Serializable
data class MessagePartDto(
    val partId: String? = null,
    val mimeType: String? = null,
    val filename: String? = null,
    val headers: List<HeaderDto>? = null,
    val body: MessagePartBodyDto? = null,
    val parts: List<MessagePartDto>? = null
)

/** A single message header, e.g. Subject or From. */
@Serializable
data class HeaderDto(
    val name: String? = null,
    val value: String? = null
)

/** Raw body payload, base64url-encoded in `data`. */
@Serializable
data class MessagePartBodyDto(
    val attachmentId: String? = null,
    val size: Int = 0,
    val data: String? = null
)
