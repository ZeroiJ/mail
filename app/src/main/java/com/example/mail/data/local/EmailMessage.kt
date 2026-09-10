package com.example.mail.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mail.util.BundleType

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
    val summary: String = "", // On-device 2-sentence TL;DR (Gemini Nano)
    @ColumnInfo(name = "bundle_type")
    val bundleType: String = BundleType.PERSONAL.label, // AutoBundler category
    val timestamp: Long,
    val isOTP: Boolean = false, // Flags for ephemeral widget rendering
    val expiresAt: Long = 0L, // Timestamp for auto-deletion
    val snoozedUntil: Long = 0L // Timestamp until which the message stays hidden from the deck (0 = not snoozed)
)
