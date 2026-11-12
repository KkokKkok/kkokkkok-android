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
internal fun DeleteDataDialog(onDismiss: () -> Unit, onConfirm: () -> Unit = {}, isMock: Boolean = true,
    sharedLocalData: Boolean = false, deleting: Boolean = false, error: String? = null) {
    Dialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
            dismissOnClickOutside = false, dismissOnBackPress = !deleting),
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
                KkokText("원본·수정 사진과 검사·편집 기록, 민감 위치와 누적 분석 결과를 지워요. 되돌릴 수 없어요.",
                    Modifier.fillMaxWidth(),
                    KkokTypography.BodyMedium.copy(textAlign = TextAlign.Center, lineBreak = LineBreak.Paragraph), KkokColors.Gray600)
                KkokText(if (isMock) "모의 화면 · 실제 데이터는 삭제되지 않아요" else "기기 갤러리에 저장한 사진은 남아 있어요.", Modifier.fillMaxWidth(),
                    KkokTypography.LabelSmall.copy(textAlign = TextAlign.Center), KkokColors.Gray500)
                if (sharedLocalData) KkokText("로컬 테스트 모드에서는 공용 사진과 검사·편집 기록도 함께 삭제돼요.",
                    style = KkokTypography.BodySmall, color = KkokColors.High)
                error?.let { KkokText(it, style = KkokTypography.BodySmall, color = KkokColors.High) }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogButton("취소", Modifier.weight(1f), destructive = false, enabled = !deleting, onClick = onDismiss)
                    DialogButton(if (deleting) "삭제 중…" else "모두 삭제", Modifier.weight(1f), destructive = true, enabled = !isMock && !deleting, onClick = onConfirm)
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

/** Logout confirmation. Same modal style as the delete dialog; Figma defines no settings screen yet. */
@Composable
internal fun LogoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect { window.setDimAmount(0f) }
        Box(Modifier.fillMaxSize().background(KkokColors.Ink.copy(alpha = .45f)).padding(24.dp),
            contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 320.dp).fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp),
                        ambientColor = KkokColors.Ink.copy(alpha = .18f), spotColor = KkokColors.Ink.copy(alpha = .18f))
                    .background(KkokColors.White, RoundedCornerShape(28.dp))
                    .semantics { paneTitle = "로그아웃 확인" }
                    .testTag("logout-dialog")
                    .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KkokText("로그아웃할까요?", Modifier.fillMaxWidth(), KkokTypography.TitleLarge.copy(textAlign = TextAlign.Center))
                KkokText("내 사진과 기록은 그대로 남아 있어요.\n다시 로그인하면 이어서 볼 수 있어요.", Modifier.fillMaxWidth(),
                    KkokTypography.BodyMedium.copy(textAlign = TextAlign.Center, lineBreak = LineBreak.Paragraph), KkokColors.Gray600)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogButton("취소", Modifier.weight(1f), destructive = false, onClick = onDismiss)
                    DialogButton("로그아웃", Modifier.weight(1f).testTag("logout-confirm"), destructive = true, onClick = onConfirm)
                }
            }
        }
    }
}
