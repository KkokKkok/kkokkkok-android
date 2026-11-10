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
    val deleting: Boolean = false,
    val deleteError: String? = null,
    val deleted: Boolean = false,
)

class ProfileViewModel(
    private val repository: ProfileRepository = MockProfileRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileState())
    val state = mutableState.asStateFlow()

    private var loadingJob: kotlinx.coroutines.Job? = null
    init { retry() }

    fun openDeleteDialog() {
        val current = mutableState.value
        if (current.overview != null && !current.isLoading && !current.error) {
            mutableState.value = current.copy(showDeleteDialog = true, deleteError = null)
        }
    }

    fun dismissDeleteDialog() {
        if (mutableState.value.deleting) return
        mutableState.value = mutableState.value.copy(showDeleteDialog = false)
    }

    fun retry() {
        if (mutableState.value.deleting) return
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true, error = false)
            loadOverview()
        }
    }

    fun deleteAll() {
        val current = mutableState.value
        if (current.deleting || !current.showDeleteDialog || current.overview?.isMock != false) return
        loadingJob?.cancel()
        mutableState.value = current.copy(deleting = true, deleteError = null)
        viewModelScope.launch {
            try {
                repository.deleteAll()
                mutableState.value = ProfileState(isLoading = false, deleted = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                mutableState.value = mutableState.value.copy(deleting = false,
                    deleteError = error.message ?: "삭제를 완료하지 못했어요. 다시 시도해 주세요.")
            }
        }
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
