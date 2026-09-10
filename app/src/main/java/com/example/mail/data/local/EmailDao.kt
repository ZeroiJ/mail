package com.example.mail.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

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
    suspend fun getEmailById(emailId: String): EmailMessage?

    @Query("SELECT * FROM email_messages ORDER BY timestamp DESC")
    fun getAllEmailsPaged(): PagingSource<Int, EmailMessage>

    @Query("SELECT * FROM email_messages WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getTriageQueuePaged(startTime: Long): PagingSource<Int, EmailMessage>

    @Query("SELECT * FROM email_messages WHERE isOTP = 1 AND expiresAt > 0")
    fun getOtpEmailsPaged(): PagingSource<Int, EmailMessage>

    @Query("DELETE FROM email_messages WHERE expiresAt > 0 AND expiresAt < :currentTime")
    suspend fun deleteExpiredOtps(currentTime: Long)

    @Query("SELECT COUNT(*) FROM email_messages")
    suspend fun getEmailCount(): Int

    @Query("DELETE FROM email_messages")
    suspend fun deleteAllEmails()
}
