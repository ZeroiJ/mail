package com.example.mail.domain.repository

import androidx.paging.PagingData
import com.example.mail.data.local.ConversationItem
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

    fun getPagedEmailsByBundle(bundle: String): Flow<PagingData<EmailMessage>>

    /**
     * Conversation deck: one row per thread (newest message + unread count),
     * ordered by recency. Powers the triage deck in conversation mode.
     */
    fun getConversations(): Flow<PagingData<ConversationItem>>

    fun getConversationsByBundle(bundle: String): Flow<PagingData<ConversationItem>>

    /**
     * All messages in a thread, oldest first. Powers the expandable
     * conversation view in the reader.
     */
    fun observeThread(threadId: String): Flow<List<EmailMessage>>

    /**
     * Mark an entire thread read: optimistic local update, then remove the
     * UNREAD label server-side via `threads.modify` (one call per thread).
     */
    suspend fun markThreadRead(threadId: String)

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
     * Archive an entire conversation: remove the INBOX label from every
     * message server-side via `threads.modify`, and drop the whole thread
     * from the local deck.
     */
    suspend fun archiveConversation(threadId: String)

    /**
     * Delete an entire conversation: TRASH server-side via `threads.modify`,
     * and drop the whole thread from the local deck.
     */
    suspend fun deleteConversation(threadId: String)

    /**
     * Snooze a message: remove the INBOX label on the server and hide it from
     * the deck locally until [untilTimestamp].
     */
    suspend fun snoozeEmail(id: String, untilTimestamp: Long)

    suspend fun deleteExpiredOtps()

    suspend fun resetSnoozedEmails()

    /**
     * Pull recent emails from Gmail REST, strip trackers, run AI parsing,
     * classify bundles, and upsert into Room. Returns the number of new
     * or updated messages inserted.
     */
    suspend fun syncRecentEmails(): Int

    /**
     * Fetch the NEXT page of the inbox using the stored page token.
     * Returns the number inserted, or 0 when no further pages exist.
     * Powers the on-request "LOAD MORE" batch loading.
     */
    suspend fun syncMoreEmails(): Int

    /** True when the last list call returned a next-page token. */
    suspend fun hasMoreEmails(): Boolean

    /**
     * Network search against Gmail with a raw query string. Results are
     * upserted into Room so the reader can resolve them by ID, and returned
     * for immediate display.
     */
    suspend fun searchEmails(query: String): List<EmailMessage>

    /**
     * Send a plain-text email. Builds the RFC822 payload locally and posts
     * it via `messages.send`. Returns the sent message ID, or null on failure.
     */
    suspend fun sendEmail(
        to: String,
        cc: String,
        bcc: String,
        subject: String,
        body: String,
        inReplyTo: String = "",
        references: String = ""
    ): String?

    /** All local drafts, newest first. */
    fun getDrafts(): Flow<List<com.example.mail.data.local.Draft>>

    /**
     * Insert or update a draft locally, then mirror it to the server
     * (create or update by `serverDraftId`). Returns the saved row with
     * the server ID filled in. Local save always succeeds; server sync
     * failures degrade to offline mode.
     */
    suspend fun saveDraft(draft: com.example.mail.data.local.Draft): com.example.mail.data.local.Draft

    /** Delete a draft locally and on the server. */
    suspend fun deleteDraft(draftId: Long)

    /** All labels (system + user), synced from Gmail. */
    fun getLabels(): Flow<List<com.example.mail.data.local.Label>>

    /** Pull the label list from Gmail and upsert locally. */
    suspend fun syncLabels()

    /** Create a user label on Gmail and cache it. Returns the label ID or null. */
    suspend fun createLabel(name: String): String?

    /** Rename a user label on Gmail and locally. */
    suspend fun renameLabel(labelId: String, name: String): Boolean

    /** Delete a user label on Gmail and locally. */
    suspend fun deleteLabel(labelId: String): Boolean

    /** Apply a label to a message (server + local cross-ref). */
    suspend fun applyLabel(messageId: String, labelId: String)

    /** Remove a label from a message (server + local cross-ref). */
    suspend fun removeLabel(messageId: String, labelId: String)

    /** Label IDs currently on a message, from the local cross-ref table. */
    suspend fun getMessageLabelIds(messageId: String): List<String>
}
