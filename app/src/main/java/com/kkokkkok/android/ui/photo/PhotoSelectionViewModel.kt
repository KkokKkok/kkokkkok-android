package com.kkokkkok.android.ui.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkokkkok.android.analysis.AnalysisRepository
import com.kkokkkok.android.analysis.AnalysisStep
import com.kkokkkok.android.analysis.AnalysisState
import com.kkokkkok.android.analysis.MockAnalysisRepository
import com.kkokkkok.android.analysis.AnalysisBatchPhoto
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
    private val photos = MutableStateFlow<List<Uri>>(emptyList())
    val selectedPhotos = photos.asStateFlow()
    private val batchStates = MutableStateFlow<List<AnalysisState>>(emptyList())
    val batchAnalysisStates = batchStates.asStateFlow()
    private val pageIndex = MutableStateFlow(0)
    val resultPageIndex = pageIndex.asStateFlow()
    private val manualLocationNeeded = MutableStateFlow(false)
    val needsLocationInput = manualLocationNeeded.asStateFlow()
    private val photoLocations = mutableMapOf<Uri, String>()
    private val manualLocationUris = mutableListOf<Uri>()
    private var manualLocationCursor = 0
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
            photos.value = listOf(uri)
            batchStates.value = emptyList()
            manualLocationNeeded.value = false
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
                            manualLocationNeeded.value = true
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

    fun onPhotosPicked(uris: List<Uri>) {
        val unique = uris.distinct().take(20)
        if (unique.size <= 1) {
            onPhotoPicked(unique.firstOrNull())
            return
        }
        preparationJob?.cancel()
        cancelAnalysis()
        photos.value = unique
        selection.value = unique.first()
        batchStates.value = emptyList()
        pageIndex.value = 0
        photoLocations.clear()
        manualLocationUris.clear()
        manualLocationCursor = 0
        manualLocationNeeded.value = false
        location.value = PhotoLocationState(location = "")
        analysis.value = AnalysisState.Running(AnalysisStep.METADATA, 5)
        preparationJob = viewModelScope.launch {
            try {
                unique.forEach { uri ->
                    val hasGps = repository.hasExifGps(uri)
                    if (selection.value != unique.first()) return@launch
                    if (hasGps != true) manualLocationUris += uri
                }
                if (manualLocationUris.isEmpty()) startBatchAnalysis()
                else showLocationFor(manualLocationUris.first())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                analysis.value = AnalysisState.Failed
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
        if (photos.value.size > 1) {
            if (location.value.location.isBlank()) {
                location.update { it.copy(error = true) }
                return false
            }
            photoLocations[requireNotNull(selection.value)] = location.value.location.trim()
            manualLocationCursor += 1
            if (manualLocationCursor < manualLocationUris.size) {
                showLocationFor(manualLocationUris[manualLocationCursor])
            } else startBatchAnalysis()
            return true
        }
        val photo = selection.value ?: return false
        val selectedLocation = location.value.location.trim()
        if (location.value.requiresManualLocation && selectedLocation.isEmpty()) {
            location.update { it.copy(error = true) }
            return false
        }
        analysisJob?.cancel()
        manualLocationNeeded.value = false
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

    fun showResultPage(index: Int) {
        if (photos.value.size < 2 || index !in photos.value.indices) return
        pageIndex.value = index
    }

    private fun showLocationFor(uri: Uri) {
        selection.value = uri
        location.value = PhotoLocationState(location = "", requiresManualLocation = true)
        manualLocationNeeded.value = true
        analysis.value = AnalysisState.Idle
    }

    private fun startBatchAnalysis() {
        manualLocationNeeded.value = false
        pageIndex.value = 0
        val request = photos.value.map { AnalysisBatchPhoto(it, photoLocations[it].orEmpty()) }
        batchStates.value = List(request.size) { AnalysisState.Running(AnalysisStep.METADATA, 5) }
        analysis.value = batchStates.value.firstOrNull() ?: AnalysisState.Idle
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            try {
                repository.analyzeBatch(request).collect { states ->
                    batchStates.value = states
                    analysis.value = states.firstOrNull() ?: AnalysisState.Idle
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                batchStates.value = List(request.size) { AnalysisState.Failed }
                analysis.value = AnalysisState.Failed
            }
        }
    }

    fun cancelPhotoLocation() {
        preparationJob?.cancel()
        preparationJob = null
        cancelAnalysis()
        selection.value = null
        photos.value = emptyList()
        batchStates.value = emptyList()
        manualLocationNeeded.value = false
        photoLocations.clear()
        manualLocationUris.clear()
        pageIndex.value = 0
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
        if (photos.value.size > 1) {
            startBatchAnalysis()
            return
        }
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
