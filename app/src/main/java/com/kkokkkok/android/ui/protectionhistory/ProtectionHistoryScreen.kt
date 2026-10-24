package com.kkokkkok.android.ui.protectionhistory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kkokkkok.android.R
import com.kkokkkok.android.protectionhistory.ProtectionHistoryViewModel
import com.kkokkkok.android.protectionhistory.ProtectionRecord
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokSpacing
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

@Composable
fun ProtectionHistoryScreen(
    viewModel: ProtectionHistoryViewModel,
    onHome: () -> Unit,
    onInspectionHistory: () -> Unit,
    onAccount: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val top = (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 8.dp).coerceAtLeast(0.dp)
    Column(Modifier.fillMaxSize().background(KkokColors.Background).padding(top = top)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = KkokSpacing.Page)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KkokText("보호 기록", style = KkokTypography.HeadlineMedium)
            Row(Modifier.fillMaxWidth().height(28.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                KkokText("전체", style = KkokTypography.LabelLarge, color = KkokColors.Gray500)
            }
            when {
                state.isLoading -> KkokText("기록을 불러오고 있어요…", Modifier.padding(vertical = 24.dp), color = KkokColors.Gray600)
                state.error -> ProtectionHistoryEmpty("기록을 불러오지 못했어요", "다시 시도해 주세요", viewModel::retry)
                state.records.isEmpty() -> ProtectionHistoryEmpty("아직 보호 기록이 없어요", "사진을 고르면 보호 기록을 확인할 수 있어요")
                else -> Column(verticalArrangement = Arrangement.spacedBy(KkokSpacing.Item)) {
                    state.records.chunked(2).forEach { rowRecords ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KkokSpacing.Item)) {
                            rowRecords.forEach { record ->
                                ProtectionRecordCard(record, Modifier.weight(1f))
                            }
                            if (rowRecords.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                    ProtectionNotice()
                }
            }
        }
        ProtectionHistoryTabBar(onHome, onInspectionHistory, onAccount)
    }
}

@Composable
private fun ProtectionRecordCard(record: ProtectionRecord, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(KkokColors.White)
            .semantics(mergeDescendants = true) {
                contentDescription = "모의 보호 기록 ${record.originalScore}에서 ${record.protectedScore}, ${record.date}"
            }.padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ProtectionThumbnail(record.artwork, after = false, Modifier.weight(1f))
            ArrowRight()
            ProtectionThumbnail(record.artwork, after = true, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            KkokText(
                "${record.originalScore} → ${record.protectedScore}",
                style = KkokTypography.TitleMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = KkokColors.Safe,
            )
            KkokText(record.date, style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
        }
    }
}

@Composable
private fun ProtectionThumbnail(variant: Int, after: Boolean, modifier: Modifier = Modifier) {
    val sky = when (variant) {
        0 -> Color(0xFFE8EEE8)
        1 -> Color(0xFFE3EBEF)
        2 -> Color(0xFFE8E3DC)
        else -> Color(0xFFF1E5D8)
    }
    Canvas(modifier.height(62.dp).clip(RoundedCornerShape(16.dp)).background(sky)) {
        val w = size.width
        val h = size.height
        val ground = if (after) Color(0xFFB9CDBA) else Color(0xFFB2C7B3)
        drawRect(ground, Offset(0f, h * .68f), Size(w, h * .32f))
        when (variant) {
            0 -> {
                drawRect(Color(0xFF9C826A), Offset(w * .08f, h * .12f), Size(w * .34f, h * .7f))
                drawRect(Color(0xFFFFF8ED), Offset(w * .47f, h * .18f), Size(w * .42f, h * .48f))
                drawRect(Color(0xFFADC7C4), Offset(w * .51f, h * .22f), Size(w * .34f, h * .38f))
                drawCircle(Color(0xFF846A53), w * .12f, Offset(w * .66f, h * .78f))
                if (after) drawRoundRect(KkokColors.Secondary, Offset(w * .57f, h * .31f), Size(w * .17f, h * .12f), CornerRadius(2.dp.toPx()))
            }
            1 -> {
                drawRect(Color(0xFF758C82), Offset(w * .08f, h * .26f), Size(w * .84f, h * .52f))
                drawCircle(Color(0xFFD7AF94), w * .18f, Offset(w * .5f, h * .38f))
                drawRoundRect(Color(0xFF4D6671), Offset(w * .26f, h * .5f), Size(w * .48f, h * .42f), CornerRadius(w * .16f))
                if (after) drawCircle(Color(0xFFE3EBEF), w * .08f, Offset(w * .78f, h * .18f))
            }
            2 -> {
                drawRect(Color(0xFFF6F1E8), Offset(w * .19f, h * .08f), Size(w * .64f, h * .75f))
                drawRect(Color(0xFF89A5A0), Offset(w * .25f, h * .14f), Size(w * .52f, h * .69f))
                repeat(3) { index ->
                    drawRect(Color(0xFFE9E1D2), Offset(w * (.31f + index * .14f), h * .22f), Size(w * .06f, h * .1f))
                    drawRect(Color(0xFFE9E1D2), Offset(w * (.31f + index * .14f), h * .42f), Size(w * .06f, h * .1f))
                }
                if (after) drawRect(Color(0xFFB8C6B3), Offset(w * .65f, h * .55f), Size(w * .12f, h * .18f))
            }
            else -> {
                drawRoundRect(Color(0xFFFBF6EE), Offset(w * .12f, h * .14f), Size(w * .76f, h * .68f), CornerRadius(8.dp.toPx()))
                drawCircle(Color(0xFFD8A76F), w * .28f, Offset(w * .5f, h * .5f))
                drawCircle(Color(0xFF789566), w * .1f, Offset(w * .38f, h * .42f))
                drawCircle(Color(0xFFB7794F), w * .08f, Offset(w * .59f, h * .57f))
                if (after) drawCircle(Color(0xFFFFD78A), w * .06f, Offset(w * .68f, h * .3f))
            }
        }
    }
}

@Composable
private fun ArrowRight() {
    Canvas(Modifier.size(14.dp)) {
        val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(KkokColors.Gray400, Offset(size.width * .12f, size.height * .5f), Offset(size.width * .86f, size.height * .5f), stroke.width)
        val arrow = Path().apply {
            moveTo(size.width * .58f, size.height * .22f)
            lineTo(size.width * .88f, size.height * .5f)
            lineTo(size.width * .58f, size.height * .78f)
        }
        drawPath(arrow, KkokColors.Gray400, style = stroke)
    }
}

@Composable
private fun ProtectionNotice() {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 45.dp).clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFEDBE)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProtectionLockIcon()
        KkokText("모의 예시예요 · 실제 사진은 저장하지 않아요", style = KkokTypography.LabelMedium, color = KkokColors.Gray600)
    }
}

@Composable
private fun ProtectionLockIcon() {
    Canvas(Modifier.size(16.dp)) {
        val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(KkokColors.Ink, Offset(2.dp.toPx(), 7.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 9.dp.toPx()), CornerRadius(2.dp.toPx()), style = stroke)
        drawArc(KkokColors.Ink, 180f, 180f, false, Offset(4.dp.toPx(), 1.dp.toPx()), Size(size.width - 8.dp.toPx(), 11.dp.toPx()), style = stroke)
    }
}

@Composable
private fun ProtectionHistoryEmpty(title: String, message: String, onRetry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KkokText(title, style = KkokTypography.TitleMedium)
        KkokText(message, color = KkokColors.Gray500)
        if (onRetry != null) {
            KkokText("다시 불러오기", Modifier.clip(CircleShape).background(KkokColors.Secondary)
                .clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 14.dp, vertical = 8.dp),
                KkokTypography.LabelLarge, KkokColors.Primary)
        }
    }
}

@Composable
private fun ProtectionHistoryTabBar(onHome: () -> Unit, onInspectionHistory: () -> Unit, onAccount: () -> Unit) {
    val bottom = 18.dp
    Row(
        Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(horizontal = 28.dp).padding(top = 10.dp, bottom = bottom),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ProtectionTab(R.drawable.figma_home, "홈", false, onHome)
        ProtectionTab(R.drawable.figma_clock, "검사 기록", false, onInspectionHistory)
        ProtectionTab(R.drawable.figma_pencil, "보호 기록", true, {})
        ProtectionTab(R.drawable.figma_user, "마이", false, onAccount)
    }
}

@Composable
private fun ProtectionTab(icon: Int, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(role = Role.Tab, onClick = onClick).semantics(mergeDescendants = true) {
            role = Role.Tab
            selected = active
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.clip(CircleShape).background(if (active) KkokColors.Secondary else KkokColors.White)
            .padding(horizontal = 14.dp, vertical = 4.dp)) { FigmaIcon(icon) }
        KkokText(label, style = KkokTypography.LabelMedium, color = if (active) KkokColors.Primary else KkokColors.Gray400)
    }
}
