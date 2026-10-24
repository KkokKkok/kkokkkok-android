package com.kkokkkok.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kkokkkok.android.analysis.AnalysisState
import com.kkokkkok.android.ui.analysis.AnalysisFlowScreen
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkokkkok.android.ui.home.HomeScreen
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent { KkokKkokApp() }
    }
}

@Composable
internal fun KkokKkokApp(selection: PhotoSelectionViewModel = viewModel()) {
    val photo by selection.selectedPhoto.collectAsStateWithLifecycle()
    val analysis by selection.analysisState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        selection.onPhotoPicked(uri)
        if (uri != null) selection.startAnalysis()
    }
    if (analysis == AnalysisState.Idle || photo == null) {
        HomeScreen(onSelectPhoto = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) })
    } else {
        BackHandler(onBack = selection::cancelAnalysis)
        AnalysisFlowScreen(requireNotNull(photo), analysis, selection::cancelAnalysis, selection::startAnalysis)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppPreview() {
    HomeScreen()
}
