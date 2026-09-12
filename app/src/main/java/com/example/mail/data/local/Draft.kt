package com.example.mail.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "recipients_to")
    val to: String = "",
    @ColumnInfo(name = "recipients_cc")
    val cc: String = "",
    @ColumnInfo(name = "recipients_bcc")
    val bcc: String = "",
    val subject: String = "",
    val body: String = "",
    val serverDraftId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
