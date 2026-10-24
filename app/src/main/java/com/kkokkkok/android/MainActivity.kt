package com.kkokkkok.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kkokkkok.android.analysis.AnalysisState
import com.kkokkkok.android.ui.analysis.AnalysisFlowScreen
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkokkkok.android.ui.home.HomeScreen
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import com.kkokkkok.android.protection.ProtectionEditViewModel
import com.kkokkkok.android.protection.ProtectionStep
import com.kkokkkok.android.ui.protection.ProtectionEditFlow

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
internal fun KkokKkokApp(
    selection: PhotoSelectionViewModel = viewModel(),
    protection: ProtectionEditViewModel = viewModel(),
) {
    val photo by selection.selectedPhoto.collectAsStateWithLifecycle()
    val analysis by selection.analysisState.collectAsStateWithLifecycle()
    val edit by protection.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val protectionPicker = remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            if (protectionPicker.value) protection.begin(uri) else {
                selection.onPhotoPicked(uri)
                selection.startAnalysis()
            }
        }
    }
    if (edit.photo != null) {
        val onBack: () -> Unit = {
            if (edit.step == ProtectionStep.LOCATION) protection.leave() else protection.back()
        }
        BackHandler(onBack = onBack)
        ProtectionEditFlow(
            state = edit,
            onBack = onBack,
            onChangePhoto = {
                protectionPicker.value = true
                picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
            onLocation = protection::updateLocation,
            onLocationCategory = protection::selectLocationCategory,
            onContinue = {
                when (edit.step) {
                    ProtectionStep.LOCATION -> protection.continueToTargets()
                    ProtectionStep.TARGETS -> protection.continueToRequest()
                    else -> Unit
                }
            },
            onToggleMode = protection::toggleMode,
            onToggleTarget = protection::toggleTarget,
            onStrength = protection::updateStrength,
            onRequest = protection::updateRequest,
            onSuggestion = protection::useSuggestion,
            onCreate = protection::createPreview,
            onNotSaved = { Toast.makeText(context, "모의 결과는 실제 사진에 적용되지 않아 저장할 수 없어요.", Toast.LENGTH_SHORT).show() },
        )
    } else if (analysis == AnalysisState.Idle || photo == null) {
        HomeScreen(
            onSelectPhoto = {
                protectionPicker.value = false
                picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
            onProtectPhoto = {
                protectionPicker.value = true
                picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
        )
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
