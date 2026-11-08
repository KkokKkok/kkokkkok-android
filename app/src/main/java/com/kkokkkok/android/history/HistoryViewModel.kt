package com.kkokkkok.android.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryState(
    val kind: HistoryKind = HistoryKind.PHOTO,
    val isMock: Boolean = true,
    val records: List<HistoryRecord> = emptyList(),
    val query: String = "",
    val isSearchOpen: Boolean = false,
    val isLoading: Boolean = true,
    val error: Boolean = false,
) {
    val visibleRecords: List<HistoryRecord>
        get() = records.filter { it.title.contains(query.trim(), ignoreCase = true) }
}

internal class HistoryViewModelFactory(
    private val repository: HistoryRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return HistoryViewModel(repository) as T
    }
}

class HistoryViewModel(
    private val repository: HistoryRepository = MockHistoryRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(HistoryState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    fun selectKind(kind: HistoryKind) {
        if (kind == mutableState.value.kind) return
        mutableState.value = mutableState.value.copy(kind = kind, query = "")
        load()
    }

    fun toggleSearch() {
        val state = mutableState.value
        mutableState.value = state.copy(isSearchOpen = !state.isSearchOpen, query = "")
    }

    fun updateQuery(query: String) {
        mutableState.value = mutableState.value.copy(query = query.take(40))
    }

    fun refresh() = load()

    fun retry() = load()

    private fun load() {
      loadJob?.cancel()
      loadJob = viewModelScope.launch {
        val kind = mutableState.value.kind
        mutableState.value = mutableState.value.copy(isLoading = true, error = false, isMock = repository.isMock(kind))
        try {
            val records = repository.getRecords(kind)
            mutableState.value = mutableState.value.copy(records = records, isLoading = false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(records = emptyList(), isLoading = false, error = true)
        }
      }
    }
}
