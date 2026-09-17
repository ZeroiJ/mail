package com.example.mail.ui.screens.triage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.mail.data.local.ConversationItem
import com.example.mail.data.local.EmailMessage
import com.example.mail.domain.repository.EmailRepository
import com.example.mail.util.AuthManager
import com.example.mail.util.BundleType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TriageViewModel @Inject constructor(
    private val repository: EmailRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _accountEmail = MutableStateFlow<String?>(null)
    val accountEmail: StateFlow<String?> = _accountEmail.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _accountEmail.value = authManager.getStoredAccountName()
        }
    }

    private val _bundleFilter = MutableStateFlow<String?>(null)
    val bundleFilter: StateFlow<String?> = _bundleFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: Flow<PagingData<ConversationItem>> =
        _bundleFilter.flatMapLatest { bundle ->
            if (bundle == null) repository.getConversations()
            else repository.getConversationsByBundle(bundle)
        }.cachedIn(viewModelScope)

    val otpEmails: Flow<PagingData<EmailMessage>> =
        repository.getOtpEmailsFlow()
            .cachedIn(viewModelScope)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _lastSyncCount = MutableStateFlow(0)
    val lastSyncCount: StateFlow<Int> = _lastSyncCount.asStateFlow()

    fun sync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val count = repository.syncRecentEmails()
                _lastSyncCount.value = count
                _hasMore.value = repository.hasMoreEmails()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                repository.syncMoreEmails()
                _hasMore.value = repository.hasMoreEmails()
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun delete(threadId: String) {
        viewModelScope.launch { repository.deleteConversation(threadId) }
    }

    fun archive(threadId: String) {
        viewModelScope.launch { repository.archiveConversation(threadId) }
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

    fun cycleBundleFilter() {
        val order = listOf(
            null,
            BundleType.RECEIPT.label,
            BundleType.LOGISTICS.label,
            BundleType.NEWSLETTER.label,
            BundleType.SOCIAL.label,
            BundleType.OTP.label,
            BundleType.PERSONAL.label
        )
        val current = order.indexOf(_bundleFilter.value).coerceAtLeast(0)
        _bundleFilter.value = order[(current + 1) % order.size]
    }

    fun clearBundleFilter() {
        _bundleFilter.value = null
    }

    fun setBundleFilter(bundle: String?) {
        _bundleFilter.value = bundle
    }

    private companion object {
        const val DEFAULT_SNOOZE_MS = 2L * 60 * 60 * 1000
    }
}
