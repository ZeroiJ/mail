package com.example.mail.data.repository

import android.util.Base64
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.mail.data.local.Draft
import com.example.mail.data.local.DraftDao
import com.example.mail.data.local.EmailDao
import com.example.mail.data.local.EmailMessage
import com.example.mail.data.remote.GmailApiService
import com.example.mail.data.remote.dto.MessageDetailDto
import com.example.mail.data.remote.dto.MessagePartDto
import com.example.mail.data.remote.dto.ModifyMessageRequest
import com.example.mail.data.remote.dto.SendMessageRequest
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
    private val draftDao: DraftDao,
    private val gmailApi: GmailApiService,
    private val gemini: GeminiProcessor
) : EmailRepository {

    private val tag = "EmailRepoImpl"

    private companion object {
        // Sync the full inbox (user requested all emails, not just last 24h).
        const val QUERY_RECENT_DAY = "in:inbox"
        const val SYNC_BATCH_SIZE = 50
    }

    @Volatile
    private var inboxNextPageToken: String? = null

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

    override suspend fun resetSnoozedEmails() = emailDao.resetSnoozedEmails(System.currentTimeMillis())

    override suspend fun syncRecentEmails(): Int = withContext(Dispatchers.IO) {
        val listResult = runCatching {
            gmailApi.listMessages(q = QUERY_RECENT_DAY, maxResults = SYNC_BATCH_SIZE)
        }
        val response = listResult.getOrElse { e ->
            Log.e(tag, "Sync failed at list stage: ${e.message}")
            return@withContext 0
        }
        inboxNextPageToken = response.nextPageToken
        val summaries = response.messages.orEmpty()

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

    override suspend fun syncMoreEmails(): Int = withContext(Dispatchers.IO) {
        val token = inboxNextPageToken ?: return@withContext 0
        val response = runCatching {
            gmailApi.listMessages(
                q = QUERY_RECENT_DAY,
                maxResults = SYNC_BATCH_SIZE,
                pageToken = token
            )
        }.getOrElse { e ->
            Log.e(tag, "Load-more failed at list stage: ${e.message}")
            return@withContext 0
        }
        inboxNextPageToken = response.nextPageToken
        val summaries = response.messages.orEmpty()
        if (summaries.isEmpty()) return@withContext 0

        val entities = coroutineScope {
            summaries.map { summary ->
                async {
                    runCatching {
                        mapToEntity(gmailApi.getMessage(id = summary.id, format = "full"))
                    }.getOrElse { e ->
                        Log.e(tag, "Load-more fetch failed for ${summary.id}: ${e.message}")
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (entities.isNotEmpty()) {
            emailDao.insertAll(entities)
        }

        Log.i(tag, "Load-more complete: ${entities.size} messages upserted")
        entities.size
    }

    override suspend fun hasMoreEmails(): Boolean = inboxNextPageToken != null

    override suspend fun searchEmails(query: String): List<EmailMessage> =
        withContext(Dispatchers.IO) {
            val summaries = runCatching {
                gmailApi.listMessages(q = query, maxResults = SYNC_BATCH_SIZE)
            }.getOrElse { e ->
                Log.e(tag, "Search failed at list stage: ${e.message}")
                return@withContext emptyList()
            }.messages.orEmpty()

            if (summaries.isEmpty()) return@withContext emptyList()

            val entities = coroutineScope {
                summaries.map { summary ->
                    async {
                        runCatching {
                            mapToEntity(gmailApi.getMessage(id = summary.id, format = "full"))
                        }.getOrElse { e ->
                            Log.e(tag, "Search fetch failed for ${summary.id}: ${e.message}")
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            if (entities.isNotEmpty()) {
                emailDao.insertAll(entities)
            }
            entities
        }

    override suspend fun sendEmail(
        to: String,
        cc: String,
        bcc: String,
        subject: String,
        body: String
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val raw = buildRfc822(to, cc, bcc, subject, body)
            val encoded = Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP)
            gmailApi.sendMessage(request = SendMessageRequest(raw = encoded)).id
        }.getOrElse { e ->
            Log.e(tag, "Send failed: ${e.message}")
            null
        }
    }

    override fun getDrafts(): Flow<List<Draft>> = draftDao.getAll()

    override suspend fun saveDraft(draft: Draft): Long = withContext(Dispatchers.IO) {
        draftDao.upsert(draft.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteDraft(draftId: Long) = withContext(Dispatchers.IO) {
        draftDao.deleteById(draftId)
    }

    private fun buildRfc822(
        to: String,
        cc: String,
        bcc: String,
        subject: String,
        body: String
    ): ByteArray {
        val subjectB64 = Base64.encodeToString(
            subject.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
        )
        val message = buildString {
            append("To: $to\r\n")
            if (cc.isNotBlank()) append("Cc: $cc\r\n")
            if (bcc.isNotBlank()) append("Bcc: $bcc\r\n")
            append("Subject: =?UTF-8?B?$subjectB64?=\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n")
            append("Content-Transfer-Encoding: 8bit\r\n")
            append("\r\n")
            append(body)
        }
        return message.toByteArray(Charsets.UTF_8)
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
            // NO_WRAP = reject line separators; NO_PADDING = ignore missing '='.
            val decoded = Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
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
