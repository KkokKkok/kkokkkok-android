package com.kkokkkok.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.kkokkkok.android.R
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

@Composable
internal fun DeleteDataDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
            dismissOnClickOutside = false),
    ) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect { window.setDimAmount(0f) }
        // The full-screen dialog window blocks interaction with the profile underneath.
        Box(Modifier.fillMaxSize().background(KkokColors.Ink.copy(alpha = .45f)).padding(24.dp),
            contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 320.dp).fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp),
                        ambientColor = KkokColors.Ink.copy(alpha = .18f), spotColor = KkokColors.Ink.copy(alpha = .18f))
                    .background(KkokColors.White, RoundedCornerShape(28.dp))
                    .verticalScroll(rememberScrollState())
                    .semantics { paneTitle = "모든 데이터 삭제 확인" }
                    .testTag("delete-data-dialog")
                    .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FigmaIcon(R.drawable.figma_delete_mascot)
                KkokText("모든 데이터를 지울까요?", Modifier.fillMaxWidth(),
                    KkokTypography.TitleLarge.copy(textAlign = TextAlign.Center))
                KkokText("등록한 민감 위치, 검사 기록, SNS 분석 결과가 모두 지워지고 되돌릴 수 없어요.",
                    Modifier.fillMaxWidth(),
                    KkokTypography.BodyMedium.copy(textAlign = TextAlign.Center, lineBreak = LineBreak.Simple), KkokColors.Gray600)
                KkokText("모의 화면 · 실제 데이터는 삭제되지 않아요", Modifier.fillMaxWidth(),
                    KkokTypography.LabelSmall.copy(textAlign = TextAlign.Center), KkokColors.Gray500)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogButton("취소", Modifier.weight(1f), destructive = false, onClick = onDismiss)
                    DialogButton("모두 삭제", Modifier.weight(1f), destructive = true, enabled = false, onClick = {})
                }
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, modifier: Modifier, destructive: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.background(if (!enabled) KkokColors.Gray200 else if (destructive) KkokColors.High else KkokColors.White, CircleShape)
        .border(if (destructive) 0.dp else 1.5.dp, KkokColors.Gray200, CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center) {
        KkokText(label, style = KkokTypography.TitleMedium,
            color = if (!enabled) KkokColors.Gray500 else if (destructive) KkokColors.White else KkokColors.Ink)
    }
}
