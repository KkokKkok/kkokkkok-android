package com.kkokkkok.android.ui.analysis

import android.net.Uri
import androidx.compose.runtime.*
import com.kkokkkok.android.analysis.AnalysisState

@Composable
internal fun AnalysisFlowScreen(uri: Uri, state: AnalysisState, onHome: () -> Unit, onRetry: () -> Unit) {
    var attempt by remember(uri) { mutableIntStateOf(0) }
    val photo = rememberPhotoImage(uri, attempt)
    if (photo == PhotoImage.Failed || state == AnalysisState.Failed) {
        AnalysisFailureScreen(onRetry = { attempt++; onRetry() }, onHome = onHome)
    } else when (state) {
        is AnalysisState.Running -> AnalysisRunningScreen(photo, state, onHome)
        is AnalysisState.Completed -> AnalysisResultScreen(photo, state.result, onHome)
        else -> Unit
    }
}
