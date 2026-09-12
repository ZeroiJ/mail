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

    @Query("SELECT * FROM email_messages WHERE id = :emailId")
    fun getEmailById(emailId: String): Flow<EmailMessage?>

    @Query("SELECT * FROM email_messages WHERE (snoozedUntil = 0 OR snoozedUntil < :now) ORDER BY timestamp DESC")
    fun getPagedEmails(now: Long): PagingSource<Int, EmailMessage>

    @Query("SELECT * FROM email_messages WHERE bundle_type = :bundle AND (snoozedUntil = 0 OR snoozedUntil < :now) ORDER BY timestamp DESC")
    fun getPagedEmailsByBundle(bundle: String, now: Long): PagingSource<Int, EmailMessage>

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
