package com.example.mail.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.mail.data.local.EmailDao
import com.example.mail.data.local.EmailMessage
import com.example.mail.domain.repository.EmailRepository
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [EmailRepository]. Local-only for now:
 * sources paging straight from the database. Network sync (Gmail REST)
 * will be layered on top later without touching the UI.
 */
@Singleton
class EmailRepositoryImpl @Inject constructor(
    private val emailDao: EmailDao
) : EmailRepository {

    override fun getTriageQueueFlow(): Flow<PagingData<EmailMessage>> {
        // Finite daily deck: bound to the last 24h so Inbox Zero stays attainable.
        val startOfDay = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { emailDao.getTriageQueuePaged(startOfDay) }
        ).flow
    }

    override fun getAllEmailsFlow(): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { emailDao.getAllEmailsPaged() }
        ).flow
    }

    override fun getOtpEmailsFlow(): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { emailDao.getOtpEmailsPaged() }
        ).flow
    }

    override suspend fun getEmailById(id: String): EmailMessage? = emailDao.getEmailById(id)

    override suspend fun deleteEmail(email: EmailMessage) = emailDao.deleteEmail(email)

    override suspend fun deleteExpiredOtps() = emailDao.deleteExpiredOtps(System.currentTimeMillis())
}
