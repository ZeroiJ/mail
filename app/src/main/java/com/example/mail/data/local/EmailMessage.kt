package com.example.mail.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_messages")
data class EmailMessage(
    @PrimaryKey
    val id: String, // Gmail Message ID
    val threadId: String,
    val sender: String,
    val subject: String,
    val snippet: String,
    val bodyHtml: String, // Original HTML
    val bodyMarkdown: String, // Parsed clean text for Reader Mode
    val timestamp: Long,
    val isOTP: Boolean = false, // Flags for ephemeral widget rendering
    val expiresAt: Long = 0L // Timestamp for auto-deletion
)
