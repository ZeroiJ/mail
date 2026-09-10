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

    suspend fun getEmailById(id: String): EmailMessage?

    suspend fun deleteEmail(email: EmailMessage)

    suspend fun deleteExpiredOtps()

    /**
     * Pull recent emails from Gmail REST, strip trackers, run AI parsing,
     * classify bundles, and upsert into Room. Returns the number of new
     * or updated messages inserted.
     */
    suspend fun syncRecentEmails(): Int
}
