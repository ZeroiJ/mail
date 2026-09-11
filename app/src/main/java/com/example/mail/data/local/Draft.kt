package com.example.mail.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val body: String = "",
    val serverDraftId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
