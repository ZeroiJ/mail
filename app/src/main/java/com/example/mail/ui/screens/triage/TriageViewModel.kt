package com.example.mail.ui.screens.triage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mail.data.local.EmailMessage
import com.example.mail.domain.repository.EmailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TriageViewModel @Inject constructor(
    private val repository: EmailRepository
) : ViewModel() {

    val emails: Flow<PagingData<EmailMessage>> =
        repository.getPagedEmails()
            .cachedIn(viewModelScope)

    val otpEmails: Flow<PagingData<EmailMessage>> =
        repository.getOtpEmailsFlow()
            .cachedIn(viewModelScope)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncCount = MutableStateFlow(0)
    val lastSyncCount: StateFlow<Int> = _lastSyncCount.asStateFlow()

    fun sync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val count = repository.syncRecentEmails()
                _lastSyncCount.value = count
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteEmail(id) }
    }

    fun archive(id: String) {
        viewModelScope.launch { repository.archiveEmail(id) }
    }

    fun snooze(id: String) {
        viewModelScope.launch {
            repository.snoozeEmail(id, System.currentTimeMillis() + DEFAULT_SNOOZE_MS)
        }
    }

    fun cleanupExpiredOtps() {
        viewModelScope.launch {
            repository.deleteExpiredOtps()
        }
    }

    private companion object {
        const val DEFAULT_SNOOZE_MS = 2L * 60 * 60 * 1000
    }
}
