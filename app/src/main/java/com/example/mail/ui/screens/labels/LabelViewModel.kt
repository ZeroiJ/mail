package com.example.mail.ui.screens.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mail.data.local.Label
import com.example.mail.domain.repository.EmailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabelViewModel @Inject constructor(
    private val repository: EmailRepository
) : ViewModel() {

    val labels: StateFlow<List<Label>> = repository.getLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appliedIds = MutableStateFlow<Set<String>>(emptySet())
    val appliedIds: StateFlow<Set<String>> = _appliedIds.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    init {
        viewModelScope.launch { repository.syncLabels() }
    }

    fun loadApplied(messageId: String) {
        viewModelScope.launch {
            _appliedIds.value = repository.getMessageLabelIds(messageId).toSet()
        }
    }

    fun toggle(messageId: String, labelId: String) {
        viewModelScope.launch {
            if (_appliedIds.value.contains(labelId)) {
                repository.removeLabel(messageId, labelId)
                _appliedIds.value = _appliedIds.value - labelId
            } else {
                repository.applyLabel(messageId, labelId)
                _appliedIds.value = _appliedIds.value + labelId
            }
        }
    }

    fun create(name: String, onDone: () -> Unit = {}) {
        if (name.isBlank() || _isWorking.value) return
        viewModelScope.launch {
            _isWorking.value = true
            try {
                if (repository.createLabel(name) != null) onDone()
            } finally {
                _isWorking.value = false
            }
        }
    }

    fun rename(labelId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renameLabel(labelId, name) }
    }

    fun delete(label: Label) {
        if (label.type == "system") return
        viewModelScope.launch { repository.deleteLabel(label.id) }
    }
}
