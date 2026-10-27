package com.kkokkkok.android.analysis

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    /** Emits progress and a result. Implementations must cooperate with coroutine cancellation. */
    fun analyze(photo: Uri, location: String): Flow<AnalysisState>
}
