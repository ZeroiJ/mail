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
     * Finite daily triage queue as a paged flow. Bounded to recent messages so
     * the card-deck view always has a finite deck (Inbox Zero < 2 min).
     */
    fun getTriageQueueFlow(): Flow<PagingData<EmailMessage>>

    fun getAllEmailsFlow(): Flow<PagingData<EmailMessage>>

    fun getOtpEmailsFlow(): Flow<PagingData<EmailMessage>>

    suspend fun getEmailById(id: String): EmailMessage?

    suspend fun deleteEmail(email: EmailMessage)

    suspend fun deleteExpiredOtps()
}
