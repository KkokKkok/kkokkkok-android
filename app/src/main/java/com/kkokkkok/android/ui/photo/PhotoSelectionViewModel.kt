package com.kkokkkok.android.ui.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkokkkok.android.analysis.AnalysisRepository
import com.kkokkkok.android.analysis.AnalysisStep
import com.kkokkkok.android.analysis.AnalysisState
import com.kkokkkok.android.analysis.MockAnalysisRepository
import androidx.lifecycle.ViewModelProvider
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
    val requiresManualLocation: Boolean = false,
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
    private var preparationJob: Job? = null

    fun onPhotoPicked(uri: Uri?) {
        // Cancelling a replacement must not discard the previous selection.
        if (uri != null) {
            preparationJob?.cancel()
            cancelAnalysis()
            selection.value = uri
            location.value = PhotoLocationState(location = "")
            analysis.value = AnalysisState.Running(AnalysisStep.METADATA, 5)
            preparationJob = viewModelScope.launch {
                try {
                    val hasExifGps = repository.hasExifGps(uri)
                    if (selection.value != uri) return@launch
                    when (hasExifGps) {
                        null -> {
                            if (analysisJob == null) {
                                location.value = PhotoLocationState()
                                analysis.value = AnalysisState.Idle
                            }
                        }
                        true -> {
                            location.value = PhotoLocationState(location = "")
                            startAnalysis()
                        }
                        false -> {
                            location.value = PhotoLocationState(location = "", requiresManualLocation = true)
                            analysis.value = AnalysisState.Idle
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (selection.value == uri) analysis.value = AnalysisState.Failed
                }
            }
        }
    }

    fun updateLocation(value: String) {
        location.update {
            it.copy(location = value.take(100), error = value.isBlank())
        }
    }

    fun selectLocationCategory(category: String) {
        location.update { it.copy(category = category, error = false) }
    }

    fun startAnalysis(): Boolean {
        val photo = selection.value ?: return false
        val selectedLocation = location.value.location.trim()
        if (location.value.requiresManualLocation && selectedLocation.isEmpty()) {
            location.update { it.copy(error = true) }
            return false
        }
        analysisJob?.cancel()
        analysisJob = null
        analysis.value = AnalysisState.Idle
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
        preparationJob?.cancel()
        preparationJob = null
        cancelAnalysis()
        selection.value = null
        location.value = PhotoLocationState()
    }

    fun cancelAnalysis() {
        val wasPreparingPhoto = preparationJob?.isActive == true
        val canOfferManualFallback = location.value.location.isBlank() && selection.value != null
        preparationJob?.cancel()
        preparationJob = null
        stopAnalysis()
        if ((wasPreparingPhoto || canOfferManualFallback) && selection.value != null) {
            location.update { it.copy(requiresManualLocation = true) }
        }
    }

    fun retryAnalysis() {
        val photo = selection.value ?: return
        if (location.value.requiresManualLocation || location.value.location.isNotBlank()) {
            startAnalysis()
        } else {
            onPhotoPicked(photo)
        }
    }

    private fun stopAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        analysis.value = AnalysisState.Idle
    }
}

internal class PhotoSelectionViewModelFactory(
    private val repository: AnalysisRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(PhotoSelectionViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return PhotoSelectionViewModel(repository) as T
    }
}
