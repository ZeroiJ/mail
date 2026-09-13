package com.example.mail.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "labels")
data class Label(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String = "user",
    val messageCount: Int = 0
)

@Entity(
    tableName = "email_label_cross_ref",
    primaryKeys = ["messageId", "labelId"]
)
data class EmailLabelCrossRef(
    val messageId: String,
    val labelId: String
)
