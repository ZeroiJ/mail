package com.example.mail.data.local

import androidx.room.Embedded

/**
 * One row per thread for the triage conversation deck: the newest
 * message in the thread plus the number of unread messages in it.
 * Produced by the conversation-grouped PagingSource queries in [EmailDao].
 */
data class ConversationItem(
    @Embedded val message: EmailMessage,
    val unreadCount: Int
)