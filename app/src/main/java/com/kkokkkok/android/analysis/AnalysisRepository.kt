package com.kkokkkok.android.analysis

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    /** Emits progress and a result. Implementations must cooperate with coroutine cancellation. */
    fun analyze(photo: Uri, location: String): Flow<AnalysisState>

    /** Returns whether EXIF GPS exists after preparing the photo; null keeps mock flows unchanged. */
    suspend fun hasExifGps(photo: Uri): Boolean? = null

    /** Each entry remains an independent single-photo inspection. */
    fun analyzeBatch(photos: List<AnalysisBatchPhoto>): Flow<List<AnalysisState>> = kotlinx.coroutines.flow.flow {
        val states = MutableList<AnalysisState>(photos.size) { AnalysisState.Running(AnalysisStep.METADATA, 5) }
        emit(states.toList())
        photos.forEachIndexed { index, photo ->
            analyze(photo.uri, photo.location).collect { state ->
                states[index] = state
                emit(states.toList())
            }
        }
    }
}

data class AnalysisBatchPhoto(val uri: Uri, val location: String)
