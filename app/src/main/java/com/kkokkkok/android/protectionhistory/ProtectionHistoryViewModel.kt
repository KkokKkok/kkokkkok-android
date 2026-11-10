package com.kkokkkok.android.protectionhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProtectionHistoryState(
    val records: List<ProtectionRecord> = emptyList(),
    val isLoading: Boolean = true,
    val error: Boolean = false,
    val selectedId: String? = null,
)

class ProtectionHistoryViewModel(
    private val repository: ProtectionHistoryRepository = MockProtectionHistoryRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProtectionHistoryState())
    val state = mutableState.asStateFlow()


    fun retry() = load()
    fun openRecord(id: String) {
        val current = mutableState.value
        if (current.records.any { it.id == id && it.originalUri != null && it.preview != null })
            mutableState.value = current.copy(selectedId = id)
    }
    fun closeRecord() { mutableState.value = mutableState.value.copy(selectedId = null) }
    private var loadJob: kotlinx.coroutines.Job? = null
    init { retry() }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, error = false, selectedId = null)
            try {
                mutableState.value = ProtectionHistoryState(records = repository.getRecords(), isLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = ProtectionHistoryState(isLoading = false, error = true)
            }
        }
    }
}

internal class ProtectionHistoryViewModelFactory(private val repository: ProtectionHistoryRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProtectionHistoryViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return ProtectionHistoryViewModel(repository) as T
    }
}
