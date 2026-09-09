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

/**
 * ViewModel for the finite triage queue. Drives the card-deck swipe view
 * (Inbox Zero < 2 min) and surfaces ephemeral OTPs in a top widget.
 */
@HiltViewModel
class TriageViewModel @Inject constructor(
    private val repository: EmailRepository
) : ViewModel() {

    /** Paginated triage queue — finite daily deck. */
    val triageEmails: Flow<PagingData<EmailMessage>> =
        repository.getTriageQueueFlow()
            .cachedIn(viewModelScope)

    /** OTP messages shown in the top widget section (live, paged). */
    val otpEmails: Flow<PagingData<EmailMessage>> =
        repository.getOtpEmailsFlow()
            .cachedIn(viewModelScope)

    // -----------------------------------------------------------------------
    // Triage action handlers — drive the card-deck gesture mapping:
    //   swipe left  → delete  (StarkRed feedback)
    //   swipe right → archive
    //   swipe up    → snooze
    // -----------------------------------------------------------------------

    /** Delete a message by ID (swipe left). */
    fun delete(id: String) {
        viewModelScope.launch {
            repository.getEmailById(id)?.let { repository.deleteEmail(it) }
        }
    }

    /** Archive a message by ID (swipe right). TODO: wire to Gmail label once sync is live. */
    fun archive(id: String) {
        viewModelScope.launch {
            repository.getEmailById(id)?.let {
                // Placeholder: delete locally for now. Real archive = add Gmail "TRASH" label.
                repository.deleteEmail(it)
            }
        }
    }

    /** Snooze a message by ID (swipe up). TODO: re-insert with future timestamp. */
    fun snooze(id: String) {
        viewModelScope.launch {
            // Placeholder: no-op until snooze scheduling is implemented.
        }
    }

    /** Cleanup expired OTPs. Call on screen appear / periodic timer. */
    fun cleanupExpiredOtps() {
        viewModelScope.launch {
            repository.deleteExpiredOtps()
        }
    }
}
