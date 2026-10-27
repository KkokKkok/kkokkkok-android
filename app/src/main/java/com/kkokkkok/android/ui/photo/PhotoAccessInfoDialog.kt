package com.kkokkkok.android.ui.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
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
internal fun PhotoAccessInfoDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect { window.setDimAmount(0f) }
        Box(
            Modifier.fillMaxSize().background(KkokColors.Ink.copy(alpha = .45f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.offset(x = 15.dp, y = 21.dp).widthIn(max = 344.dp).fillMaxWidth().requiredHeight(300.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(KkokColors.White).padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FigmaIcon(R.drawable.figma_mascot, Modifier.size(52.dp))
                KkokText(
                    "사진을 선택할게요",
                    Modifier.fillMaxWidth(),
                    KkokTypography.TitleLarge.copy(textAlign = TextAlign.Center),
                )
                KkokText(
                    "시스템 사진 선택 화면에서\n검사할 사진을 선택해요.\n선택한 사진만 검사에 사용해요.",
                    Modifier.fillMaxWidth(),
                    KkokTypography.BodyMedium.copy(textAlign = TextAlign.Center),
                    KkokColors.Gray600,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PhotoInfoButton("나중에", Modifier.weight(1f), primary = false, onClick = onDismiss)
                    PhotoInfoButton("계속하기", Modifier.weight(1f), primary = true, onClick = onContinue)
                }
            }
        }
    }
}

@Composable
private fun PhotoInfoButton(label: String, modifier: Modifier, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(CircleShape)
            .background(if (primary) KkokColors.Primary else KkokColors.White)
            .border(if (primary) 0.dp else 1.5.dp, KkokColors.Gray200, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        KkokText(label, style = KkokTypography.TitleMedium, color = if (primary) KkokColors.White else KkokColors.Ink)
    }
}
