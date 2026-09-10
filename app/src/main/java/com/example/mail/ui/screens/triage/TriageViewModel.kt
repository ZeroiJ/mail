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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TriageViewModel @Inject constructor(
    private val repository: EmailRepository
) : ViewModel() {

    val triageEmails: Flow<PagingData<EmailMessage>> =
        repository.getTriageQueueFlow()
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
        viewModelScope.launch {
            repository.getEmailById(id)?.let { repository.deleteEmail(it) }
        }
    }

    fun archive(id: String) {
        viewModelScope.launch {
            repository.getEmailById(id)?.let {
                repository.deleteEmail(it)
            }
        }
    }

    fun snooze(id: String) {
        viewModelScope.launch {
            // TODO: re-insert with future timestamp.
        }
    }

    fun cleanupExpiredOtps() {
        viewModelScope.launch {
            repository.deleteExpiredOtps()
        }
    }
}
