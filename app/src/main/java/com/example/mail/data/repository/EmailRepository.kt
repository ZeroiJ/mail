package com.example.mail.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.mail.data.local.EmailDao
import com.example.mail.data.local.EmailMessage
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Single source of truth for email data. Combines Room (offline-first cache)
 * with Gmail REST API sync (delegated upstream). All reads flow through here
 * so the UI layer never touches Room/Gmail directly.
 */
class EmailRepository(
    private val emailDao: EmailDao
) {

    /**
     * Finite daily triage queue. `startTime` bounds the query to a single day
     * so the card-deck view always has a finite deck (Inbox Zero < 2 min).
     */
    fun getTriageQueueFlow(): Flow<PagingData<EmailMessage>> {
        val startOfDay = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { emailDao.getTriageQueuePaged(startOfDay) }
        ).flow
    }

    fun getAllEmailsFlow(): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { emailDao.getAllEmailsPaged() }
        ).flow
    }

    fun getOtpEmailsFlow(): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { emailDao.getOtpEmailsPaged() }
        ).flow
    }

    suspend fun getEmailById(id: String): EmailMessage? = emailDao.getEmailById(id)

    suspend fun deleteEmail(email: EmailMessage) = emailDao.deleteEmail(email)

    suspend fun deleteExpiredOtps() = emailDao.deleteExpiredOtps(System.currentTimeMillis())
}
