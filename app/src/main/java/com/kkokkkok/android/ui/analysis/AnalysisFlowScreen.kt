package com.kkokkkok.android.ui.analysis

import android.net.Uri
import androidx.compose.runtime.*
import com.kkokkkok.android.analysis.AnalysisState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
internal fun AnalysisFlowScreen(
    uri: Uri,
    state: AnalysisState,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRetry: () -> Unit,
    onProtect: () -> Unit,
    batchStates: List<AnalysisState>? = null,
    batchPhotos: List<Uri> = emptyList(),
    batchIndex: Int = 0,
    onBatchPage: (Int) -> Unit = {},
) {
    if (batchStates != null && batchStates.isNotEmpty()) {
        val allFinished = batchStates.none { it is AnalysisState.Running }
        val visibleIndex = if (allFinished) batchIndex else batchStates.indexOfFirst { it is AnalysisState.Running }.coerceAtLeast(0)
        val visibleState = batchStates[visibleIndex]
        val visibleUri = batchPhotos.getOrNull(visibleIndex) ?: uri
        val page = if (allFinished) batchIndex else visibleIndex
        val attempt by remember(visibleUri) { mutableIntStateOf(0) }
        val image = rememberPhotoImage(visibleUri, attempt)
        when {
            visibleState is AnalysisState.Running -> AnalysisRunningScreen(image, visibleState, onBack)
            visibleState is AnalysisState.Completed && image != PhotoImage.Failed -> AnalysisResultScreen(
                image, visibleState.result, onBack, onProtect,
                batchPage = page, batchCount = batchStates.size,
                onPreviousPage = { onBatchPage((page - 1).coerceAtLeast(0)) },
                onNextPage = { onBatchPage((page + 1).coerceAtMost(batchStates.lastIndex)) },
            )
            else -> AnalysisFailureScreen(
                onRetry = onRetry, onBack = onBack, onHome = onHome,
                batchPage = page, batchCount = batchStates.size,
                onPreviousPage = { onBatchPage((page - 1).coerceAtLeast(0)) },
                onNextPage = { onBatchPage((page + 1).coerceAtMost(batchStates.lastIndex)) },
            )
        }
        return
    }
    var attempt by remember(uri) { mutableIntStateOf(0) }
    val photo = rememberPhotoImage(uri, attempt)
    if (photo == PhotoImage.Failed || state == AnalysisState.Failed) {
        AnalysisFailureScreen(onRetry = { attempt++; onRetry() }, onBack = onBack, onHome = onHome)
    } else when (state) {
        is AnalysisState.Running -> AnalysisRunningScreen(photo, state, onBack)
        is AnalysisState.Completed -> AnalysisResultScreen(photo, state.result, onBack, onProtect)
        else -> Unit
    }
}
