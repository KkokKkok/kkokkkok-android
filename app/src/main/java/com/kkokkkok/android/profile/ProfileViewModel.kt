package com.kkokkkok.android.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val overview: ProfileOverview? = null,
    val isLoading: Boolean = true,
    val error: Boolean = false,
    val showDeleteDialog: Boolean = false,
)

class ProfileViewModel(
    private val repository: ProfileRepository = MockProfileRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileState())
    val state = mutableState.asStateFlow()

    init { load() }

    fun openDeleteDialog() {
        val current = mutableState.value
        if (current.overview != null && !current.isLoading && !current.error) {
            mutableState.value = current.copy(showDeleteDialog = true)
        }
    }

    fun dismissDeleteDialog() {
        mutableState.value = mutableState.value.copy(showDeleteDialog = false)
    }

    fun toggleAutoDeleteOriginals() {
        val current = mutableState.value
        val overview = current.overview ?: return
        mutableState.value = current.copy(
            overview = overview.copy(autoDeleteOriginals = !overview.autoDeleteOriginals),
        )
    }

    fun retry() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, error = false)
            loadOverview()
        }
    }

    private fun load() {
        viewModelScope.launch { loadOverview() }
    }

    private suspend fun loadOverview() {
        try {
            mutableState.value = ProfileState(overview = repository.getOverview(), isLoading = false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = ProfileState(isLoading = false, error = true)
        }
    }
}
