package com.kkokkkok.android.ui.analysis

import android.net.Uri
import androidx.compose.runtime.*
import com.kkokkkok.android.analysis.AnalysisState

@Composable
internal fun AnalysisFlowScreen(
    uri: Uri,
    state: AnalysisState,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRetry: () -> Unit,
    onProtect: () -> Unit,
) {
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
