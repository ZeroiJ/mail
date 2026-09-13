package com.example.mail.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: EmailMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emails: List<EmailMessage>)

    @Update
    suspend fun updateEmail(email: EmailMessage)

    @Delete
    suspend fun deleteEmail(email: EmailMessage)

    @Query("DELETE FROM email_messages WHERE id = :emailId")
    suspend fun deleteEmailById(emailId: String)

    @Query("DELETE FROM email_messages WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: String)

    @Query("SELECT * FROM email_messages WHERE id = :emailId")
    fun getEmailById(emailId: String): Flow<EmailMessage?>

    @Query("SELECT * FROM email_messages WHERE (snoozedUntil = 0 OR snoozedUntil < :now) ORDER BY timestamp DESC")
    fun getPagedEmails(now: Long): PagingSource<Int, EmailMessage>

    @Query("SELECT * FROM email_messages WHERE bundle_type = :bundle AND (snoozedUntil = 0 OR snoozedUntil < :now) ORDER BY timestamp DESC")
    fun getPagedEmailsByBundle(bundle: String, now: Long): PagingSource<Int, EmailMessage>

    /**
     * Conversation deck: one row per thread (the newest non-snoozed message),
     * ordered by recency, with the number of unread messages in each thread.
     * Uses a correlated subquery instead of window functions because SQLite
     * on minSdk 26 (3.18) predates ROW_NUMBER() OVER.
     */
    @Query(
        """
        SELECT e.*, COALESCE(u.unreadCount, 0) AS unreadCount
        FROM email_messages e
        LEFT JOIN (
            SELECT threadId, COUNT(*) AS unreadCount FROM email_messages WHERE isRead = 0 GROUP BY threadId
        ) u ON u.threadId = e.threadId
        WHERE e.timestamp = (
            SELECT MAX(timestamp) FROM email_messages m
            WHERE m.threadId = e.threadId AND (m.snoozedUntil = 0 OR m.snoozedUntil < :now)
        )
        AND (e.snoozedUntil = 0 OR e.snoozedUntil < :now)
        ORDER BY e.timestamp DESC
        """
    )
    fun getConversations(now: Long): PagingSource<Int, ConversationItem>

    @Query(
        """
        SELECT e.*, COALESCE(u.unreadCount, 0) AS unreadCount
        FROM email_messages e
        LEFT JOIN (
            SELECT threadId, COUNT(*) AS unreadCount FROM email_messages WHERE isRead = 0 GROUP BY threadId
        ) u ON u.threadId = e.threadId
        WHERE e.timestamp = (
            SELECT MAX(timestamp) FROM email_messages m
            WHERE m.threadId = e.threadId AND m.bundle_type = :bundle
                AND (m.snoozedUntil = 0 OR m.snoozedUntil < :now)
        )
        AND e.bundle_type = :bundle
        AND (e.snoozedUntil = 0 OR e.snoozedUntil < :now)
        ORDER BY e.timestamp DESC
        """
    )
    fun getConversationsByBundle(bundle: String, now: Long): PagingSource<Int, ConversationItem>

    /** All messages in a thread, oldest first — powers the expandable reader. */
    @Query("SELECT * FROM email_messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun observeThread(threadId: String): Flow<List<EmailMessage>>

    /** Mark every message in a thread read (local, optimistic). */
    @Query("UPDATE email_messages SET isRead = 1 WHERE threadId = :threadId")
    suspend fun markThreadRead(threadId: String)

    @Query("SELECT COUNT(*) FROM email_messages WHERE threadId = :threadId AND isRead = 0")
    suspend fun countUnreadInThread(threadId: String): Int

    @Query("SELECT * FROM email_messages WHERE isOTP = 1 AND expiresAt > 0")
    fun getOtpEmailsPaged(): PagingSource<Int, EmailMessage>

    @Query("UPDATE email_messages SET snoozedUntil = :until WHERE id = :emailId")
    suspend fun updateSnoozedUntil(emailId: String, until: Long)

    @Query("DELETE FROM email_messages WHERE expiresAt > 0 AND expiresAt < :currentTime")
    suspend fun deleteExpiredOtps(currentTime: Long)

    @Query("UPDATE email_messages SET snoozedUntil = 0 WHERE snoozedUntil > 0 AND snoozedUntil < :now")
    suspend fun resetSnoozedEmails(now: Long)

    @Query("SELECT COUNT(*) FROM email_messages")
    suspend fun getEmailCount(): Int

    @Query("DELETE FROM email_messages")
    suspend fun deleteAllEmails()
}
