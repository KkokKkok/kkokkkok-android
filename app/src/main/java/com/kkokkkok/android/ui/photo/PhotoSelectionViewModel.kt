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
import kotlinx.coroutines.launch

/** Session-only photo selection and analysis state; not a persisted API request. */
class PhotoSelectionViewModel(
    private val repository: AnalysisRepository = MockAnalysisRepository(),
) : ViewModel() {
    private val selection = MutableStateFlow<Uri?>(null)
    val selectedPhoto = selection.asStateFlow()
    private val analysis = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState = analysis.asStateFlow()
    private var analysisJob: Job? = null

    fun onPhotoPicked(uri: Uri?) {
        // Cancelling a replacement must not discard the previous selection.
        if (uri != null) {
            cancelAnalysis()
            selection.value = uri
        }
    }

    fun startAnalysis() {
        val photo = selection.value ?: return
        cancelAnalysis()
        analysisJob = viewModelScope.launch {
            try {
                repository.analyze(photo).collect { analysis.value = it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Do not expose exception text, which may contain local photo references.
                analysis.value = AnalysisState.Failed
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        analysis.value = AnalysisState.Idle
    }
}
