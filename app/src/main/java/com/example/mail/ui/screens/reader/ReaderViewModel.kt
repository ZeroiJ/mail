package com.example.mail.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mail.data.local.EmailMessage
import com.example.mail.domain.repository.EmailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmailRepository
) : ViewModel() {

    private val emailId: String = checkNotNull(savedStateHandle["emailId"])

    val email: StateFlow<EmailMessage?> = repository.getEmailById(emailId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /**
     * Every message in the opened conversation, oldest first. Empty until the
     * opened email resolves its threadId.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val thread: StateFlow<List<EmailMessage>> = email
        .filterNotNull()
        .map { it.threadId }
        .distinctUntilChanged()
        .flatMapLatest { threadId ->
            if (threadId.isBlank()) flowOf(emptyList())
            else repository.observeThread(threadId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun markThreadRead(threadId: String) {
        if (threadId.isBlank()) return
        viewModelScope.launch { repository.markThreadRead(threadId) }
    }
}