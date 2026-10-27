package com.kkokkkok.android.ui.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkokkkok.android.analysis.AnalysisRepository
import com.kkokkkok.android.analysis.AnalysisState
import com.kkokkkok.android.analysis.MockAnalysisRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoLocationState(
    val location: String = "꼭꼭로 24, 2층 카페",
    val category: String = "자주 가는 곳",
    val error: Boolean = false,
)

/** Session-only photo selection and analysis state; not a persisted API request. */
class PhotoSelectionViewModel(
    private val repository: AnalysisRepository = MockAnalysisRepository(),
) : ViewModel() {
    private val selection = MutableStateFlow<Uri?>(null)
    val selectedPhoto = selection.asStateFlow()
    private val analysis = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState = analysis.asStateFlow()
    private val location = MutableStateFlow(PhotoLocationState())
    val locationState = location.asStateFlow()
    private var analysisJob: Job? = null

    fun onPhotoPicked(uri: Uri?) {
        // Cancelling a replacement must not discard the previous selection.
        if (uri != null) {
            cancelAnalysis()
            selection.value = uri
            location.value = PhotoLocationState()
        }
    }

    fun updateLocation(value: String) {
        location.update {
            it.copy(location = value.take(100), category = "", error = value.isBlank())
        }
    }

    fun selectLocationCategory(category: String) {
        val mockLocation = when (category) {
            "집 주변" -> "꼭꼭로 24 · 집 주변 (예시)"
            "학교" -> "꼭꼭대학교 정문 (예시)"
            "직장" -> "꼭꼭빌딩 3층 (예시)"
            else -> "꼭꼭로 24, 2층 카페"
        }
        location.value = PhotoLocationState(mockLocation, category)
    }

    fun startAnalysis(): Boolean {
        val photo = selection.value ?: return false
        val selectedLocation = location.value.location.trim()
        if (selectedLocation.isEmpty()) {
            location.update { it.copy(error = true) }
            return false
        }
        cancelAnalysis()
        analysisJob = viewModelScope.launch {
            try {
                repository.analyze(photo, selectedLocation).collect { nextState ->
                    analysis.value = nextState
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Do not expose exception text, which may contain local photo references.
                analysis.value = AnalysisState.Failed
            }
        }
        return true
    }

    fun cancelPhotoLocation() {
        cancelAnalysis()
        selection.value = null
        location.value = PhotoLocationState()
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        analysis.value = AnalysisState.Idle
    }
}
