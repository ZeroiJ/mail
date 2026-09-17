package com.example.mail.ui.screens.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mail.data.local.Draft
import com.example.mail.data.local.EmailMessage
import com.example.mail.domain.repository.EmailRepository
import com.example.mail.util.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val repository: EmailRepository,
    private val authManager: AuthManager
) : ViewModel() {

    companion object {
        const val MODE_REPLY = "reply"
        const val MODE_REPLY_ALL = "replyAll"
        const val MODE_FORWARD = "forward"
    }

    val to = MutableStateFlow("")
    val cc = MutableStateFlow("")
    val bcc = MutableStateFlow("")
    val subject = MutableStateFlow("")
    val body = MutableStateFlow("")

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _sendResult = MutableStateFlow<Boolean?>(null)
    val sendResult: StateFlow<Boolean?> = _sendResult.asStateFlow()

    private var editingDraftId: Long = 0L
    private var inReplyTo: String = ""
    private var references: String = ""
    private var replyPrepared = false

    fun prepareReply(emailId: String, mode: String) {
        if (replyPrepared || emailId.isBlank() || mode.isBlank()) return
        replyPrepared = true
        viewModelScope.launch {
            val original = repository.getEmailById(emailId).first() ?: return@launch
            when (mode) {
                MODE_REPLY_ALL -> {
                    to.value = extractAddress(original.sender)
                    cc.value = mergeRecipients(original)
                    applyThreading(original)
                }
                MODE_FORWARD -> {
                    // To/subject/body stay blank; threading intentionally unset.
                }
                else -> {
                    to.value = extractAddress(original.sender)
                    applyThreading(original)
                }
            }
        }
    }

    private fun applyThreading(original: EmailMessage) {
        inReplyTo = original.rfcMessageId
        references = listOf(original.headerReferences, original.rfcMessageId)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun mergeRecipients(original: EmailMessage): String {
        val self = authManager.getStoredAccountName().orEmpty()
        return (splitAddresses(original.toRecipients) + splitAddresses(original.ccRecipients))
            .map { extractAddress(it) }
            .filter { it.isNotBlank() && !it.equals(self, ignoreCase = true) }
            .distinct()
            .joinToString(", ")
    }

    private fun extractAddress(raw: String): String {
        val bracketed = Regex("<([^>]+)>").find(raw)?.groupValues?.getOrNull(1)
        return (bracketed ?: raw).trim()
    }

    private fun splitAddresses(header: String): List<String> {
        return header.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun loadDraft(draft: Draft) {
        editingDraftId = draft.id
        to.value = draft.to
        cc.value = draft.cc
        bcc.value = draft.bcc
        subject.value = draft.subject
        body.value = draft.body
    }

    fun send(onDone: () -> Unit) {
        if (_isSending.value || to.value.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                val messageId = repository.sendEmail(
                    to = to.value.trim(),
                    cc = cc.value.trim(),
                    bcc = bcc.value.trim(),
                    subject = subject.value.trim(),
                    body = body.value,
                    inReplyTo = inReplyTo,
                    references = references
                )
                _sendResult.value = messageId != null
                if (messageId != null) {
                    if (editingDraftId != 0L) repository.deleteDraft(editingDraftId)
                    onDone()
                }
            } finally {
                _isSending.value = false
            }
        }
    }

    fun saveDraft(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.saveDraft(
                Draft(
                    id = editingDraftId,
                    to = to.value.trim(),
                    cc = cc.value.trim(),
                    bcc = bcc.value.trim(),
                    subject = subject.value.trim(),
                    body = body.value
                )
            )
            onDone()
        }
    }

    fun consumeResult() {
        _sendResult.value = null
    }
}
