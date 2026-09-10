package com.example.mail.domain.repository

import androidx.paging.PagingData
import com.example.mail.data.local.EmailMessage
import kotlinx.coroutines.flow.Flow

/**
 * Pure repository interface for email access. UI layer depends on this, never
 * on Room/Paging implementation details. Implementations live in `data/repository`.
 */
interface EmailRepository {

    /**
     * Full inbox as a paged flow ordered by recency. Powers the triage deck;
     * each page holds [pageSize] messages via Paging 3.
     */
    fun getPagedEmails(): Flow<PagingData<EmailMessage>>

    fun getOtpEmailsFlow(): Flow<PagingData<EmailMessage>>

    /**
     * Reactive single-email stream for the Reader/Detail view. Emits null
     * while no row matches (e.g. the message was deleted meanwhile).
     */
    fun getEmailById(id: String): Flow<EmailMessage?>

    /**
     * Archive a message: remove it from the local deck optimistically, then
     * remove the INBOX label on the server.
     */
    suspend fun archiveEmail(id: String)

    /**
     * Delete a message: remove it from the local deck optimistically, then
     * move it to TRASH on the server.
     */
    suspend fun deleteEmail(id: String)

    /**
     * Snooze a message: remove the INBOX label on the server and hide it from
     * the deck locally until [untilTimestamp].
     */
    suspend fun snoozeEmail(id: String, untilTimestamp: Long)

    suspend fun deleteExpiredOtps()

    /**
     * Pull recent emails from Gmail REST, strip trackers, run AI parsing,
     * classify bundles, and upsert into Room. Returns the number of new
     * or updated messages inserted.
     */
    suspend fun syncRecentEmails(): Int
}
