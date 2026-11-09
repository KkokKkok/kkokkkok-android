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
)

class ProtectionHistoryViewModel(
    private val repository: ProtectionHistoryRepository = MockProtectionHistoryRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProtectionHistoryState())
    val state = mutableState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, error = false)
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
