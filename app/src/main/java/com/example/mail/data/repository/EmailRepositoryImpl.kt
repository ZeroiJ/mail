package com.example.mail.data.repository

import android.util.Base64
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.mail.data.local.EmailDao
import com.example.mail.data.local.EmailMessage
import com.example.mail.data.remote.GmailApiService
import com.example.mail.data.remote.dto.MessageDetailDto
import com.example.mail.data.remote.dto.MessagePartDto
import com.example.mail.data.remote.dto.ModifyMessageRequest
import com.example.mail.domain.repository.EmailRepository
import com.example.mail.util.AutoBundler
import com.example.mail.util.BundleType
import com.example.mail.util.GeminiProcessor
import com.example.mail.util.TrackerStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailRepositoryImpl @Inject constructor(
    private val emailDao: EmailDao,
    private val gmailApi: GmailApiService,
    private val gemini: GeminiProcessor
) : EmailRepository {

    private val tag = "EmailRepoImpl"

    private companion object {
        // Restrict sync to the inbox so archived/snoozed/trashed messages
        // are not resurrected locally on every poll.
        const val QUERY_RECENT_DAY = "in:inbox newer_than:1d"
        const val SYNC_BATCH_SIZE = 20
    }

    override fun getPagedEmails(): Flow<PagingData<EmailMessage>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { emailDao.getPagedEmails(System.currentTimeMillis()) }
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

    override fun getEmailById(id: String): Flow<EmailMessage?> = emailDao.getEmailById(id)

    override suspend fun archiveEmail(id: String) {
        withContext(Dispatchers.IO) {
            emailDao.deleteEmailById(id)
            runCatching {
                gmailApi.modifyMessage(
                    id = id,
                    request = ModifyMessageRequest(removeLabelIds = listOf("INBOX"))
                )
            }.onFailure { e ->
                Log.e(tag, "Archive failed for $id: ${e.message}")
            }
        }
    }

    override suspend fun deleteEmail(id: String) {
        withContext(Dispatchers.IO) {
            emailDao.deleteEmailById(id)
            runCatching {
                gmailApi.modifyMessage(
                    id = id,
                    request = ModifyMessageRequest(
                        addLabelIds = listOf("TRASH"),
                        removeLabelIds = listOf("INBOX")
                    )
                )
            }.onFailure { e ->
                Log.e(tag, "Delete failed for $id: ${e.message}")
            }
        }
    }

    override suspend fun snoozeEmail(id: String, untilTimestamp: Long) {
        withContext(Dispatchers.IO) {
            emailDao.updateSnoozedUntil(id, untilTimestamp)
            runCatching {
                gmailApi.modifyMessage(
                    id = id,
                    request = ModifyMessageRequest(removeLabelIds = listOf("INBOX"))
                )
            }.onFailure { e ->
                Log.e(tag, "Snooze failed for $id: ${e.message}")
            }
        }
    }

    override suspend fun deleteExpiredOtps() = emailDao.deleteExpiredOtps(System.currentTimeMillis())

    override suspend fun syncRecentEmails(): Int = withContext(Dispatchers.IO) {
        val listResult = runCatching {
            gmailApi.listMessages(q = QUERY_RECENT_DAY, maxResults = SYNC_BATCH_SIZE)
        }
        val summaries = listResult.getOrElse { e ->
            Log.e(tag, "Sync failed at list stage: ${e.message}")
            return@withContext 0
        }.messages.orEmpty()

        if (summaries.isEmpty()) return@withContext 0

        val entities = coroutineScope {
            summaries.map { summary ->
                async {
                    runCatching {
                        mapToEntity(gmailApi.getMessage(id = summary.id, format = "full"))
                    }.getOrElse { e ->
                        Log.e(tag, "Failed to fetch message ${summary.id}: ${e.message}")
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (entities.isNotEmpty()) {
            emailDao.insertAll(entities)
        }

        Log.i(tag, "Sync complete: ${entities.size} messages upserted")
        entities.size
    }

    private suspend fun mapToEntity(detail: MessageDetailDto): EmailMessage? {
        val headers = detail.payload?.headers
        val sender = headers?.firstOrNull { it.name == "From" }?.value.orEmpty()
        val subject = headers?.firstOrNull { it.name == "Subject" }?.value.orEmpty()
        val snippet = detail.snippet.orEmpty()
        val threadId = detail.threadId.orEmpty()
        val timestamp = detail.internalDate?.toLongOrNull()?.div(1000)
            ?: System.currentTimeMillis()

        val rawHtml = extractHtmlBody(detail.payload) ?: ""
        val cleanHtml = TrackerStripper.strip(rawHtml)
        val plainText = htmlToPlainText(cleanHtml)

        val summary = gemini.generateThreadSummary(plainText)
        val otpCode = gemini.extractOtp(plainText)
        val isOtp = otpCode.isNotEmpty()
        val otpExpiry = if (isOtp) {
            System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)
        } else {
            0L
        }

        val bundleType = AutoBundler.classify(sender, subject)

        // Ensure OTP flag is set when AI detects a code OR bundler classifies as OTP.
        val finalIsOtp = isOtp || bundleType == BundleType.OTP

        return EmailMessage(
            id = detail.id,
            threadId = threadId,
            sender = sender,
            subject = subject,
            snippet = snippet,
            bodyHtml = cleanHtml,
            bodyMarkdown = plainText,
            summary = summary,
            bundleType = bundleType.label,
            timestamp = timestamp,
            isOTP = finalIsOtp,
            expiresAt = if (finalIsOtp) otpExpiry else 0L
        )
    }

    private fun extractHtmlBody(part: MessagePartDto?): String? {
        if (part == null) return null

        if (part.mimeType == "text/html" && part.body?.data != null) {
            return decodeBase64Url(part.body.data!!)
        }

        for (child in part.parts.orEmpty()) {
            val found = extractHtmlBody(child)
            if (found != null) return found
        }

        return null
    }

    private fun decodeBase64Url(data: String): String {
        return try {
            // Gmail uses URL-safe base64 without padding.
            val padded = data.replace('-', '+').replace('_', '/')
            val decoded = Base64.decode(padded, Base64.DEFAULT)
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(tag, "Base64 decode failed: ${e.message}")
            ""
        }
    }

    private fun htmlToPlainText(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#39;"), "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
