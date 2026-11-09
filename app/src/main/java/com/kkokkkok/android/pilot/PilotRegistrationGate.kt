package com.kkokkkok.android.pilot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.kkokkkok.android.BuildConfig
import com.kkokkkok.android.ui.design.*
import kotlinx.coroutines.launch

@Composable
fun PilotRegistrationGate(content: @Composable () -> Unit) {
    if (!BuildConfig.PILOT_ACCESS_REQUIRED) { content(); return }
    val generation by PilotCredentials.generation.collectAsStateWithLifecycle()
    var ready by remember(generation) { mutableStateOf(false) }
    var loading by remember(generation) { mutableStateOf(true) }
    var code by remember { mutableStateOf("") } // Deliberately not saved across Activity/process recreation.
    var error by remember(generation) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val repository = remember { PilotRepository() }
    LaunchedEffect(generation, retry) {
        loading = true
        error = null
        try { ready = repository.validate() }
        catch (_: Exception) { error = "서버에 연결하지 못했어요. 연결을 확인하고 다시 시도해 주세요." }
        finally { loading = false }
    }
    if (ready) {
        // Dispose all feature ViewModels when credentials change; a new installation must not inherit old screen state.
        val store = remember(generation) { ViewModelStore() }
        val owner = remember(store) { object : ViewModelStoreOwner { override val viewModelStore = store } }
        DisposableEffect(store) { onDispose { store.clear() } }
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
        return
    }
    Column(Modifier.fillMaxSize().background(KkokColors.Background).safeDrawingPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        KkokText("비공개 테스트 참여", style = KkokTypography.HeadlineMedium)
        Spacer(Modifier.height(16.dp))
        KkokText("공유받은 초대 코드로 이 기기를 한 번 등록해 주세요. 등록한 기기에서 내 사진과 기록을 확인할 수 있어요.")
        Spacer(Modifier.height(24.dp))
        if (loading) KkokText("기기 등록을 확인하고 있어요.", modifier = Modifier.testTag("pilot-loading"))
        else {
            if (PilotCredentials.hasSession()) {
                KkokText(error ?: "등록 확인이 필요해요.", color = KkokColors.High)
                Spacer(Modifier.height(16.dp))
                PilotButton("다시 연결", true) { retry++ }
            } else {
                KkokText("초대 코드", style = KkokTypography.TitleMedium)
                Spacer(Modifier.height(8.dp))
                BasicTextField(code, { code = it.take(64) }, singleLine = true,
                    textStyle = KkokTypography.BodyMedium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("pilot-invite-input").semantics { contentDescription = "초대 코드 입력" }.border(1.dp, KkokColors.Gray400, RoundedCornerShape(12.dp)).padding(16.dp))
                error?.let { Spacer(Modifier.height(12.dp)); KkokText(it, color = KkokColors.High) }
                Spacer(Modifier.height(20.dp))
                PilotButton("기기 등록", code.isNotBlank()) {
                    loading = true
                    error = null
                    scope.launch {
                        try { repository.enroll(code); code = ""; ready = true }
                        catch (failure: Exception) { error = failure.message ?: "등록하지 못했어요. 다시 시도해 주세요." }
                        finally { loading = false }
                    }
                }
                Spacer(Modifier.height(16.dp))
                KkokText("앱을 지우거나 기기를 바꾸면 다시 등록해야 해요.", style = KkokTypography.BodySmall, color = KkokColors.Gray600)
            }
        }
    }
}

@Composable private fun PilotButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).background(if (enabled) KkokColors.Primary else KkokColors.Gray200, RoundedCornerShape(28.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(16.dp), contentAlignment = Alignment.Center) {
        KkokText(text, style = KkokTypography.TitleMedium, color = if (enabled) KkokColors.White else KkokColors.Gray600)
    }
}
