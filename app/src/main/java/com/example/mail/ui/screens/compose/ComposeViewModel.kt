package com.example.mail.ui.screens.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mail.data.local.Draft
import com.example.mail.domain.repository.EmailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val repository: EmailRepository
) : ViewModel() {

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
                    body = body.value
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
