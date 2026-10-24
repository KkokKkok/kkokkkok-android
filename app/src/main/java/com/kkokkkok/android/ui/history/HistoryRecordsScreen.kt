package com.kkokkkok.android.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kkokkkok.android.R
import com.kkokkkok.android.history.HistoryKind
import com.kkokkkok.android.history.HistoryRecord
import com.kkokkkok.android.history.HistoryRisk
import com.kkokkkok.android.history.HistoryState
import com.kkokkkok.android.history.HistoryViewModel
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokSpacing
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

@Composable
fun HistoryRecordsScreen(
    viewModel: HistoryViewModel,
    onHome: () -> Unit,
    onProtectionHistory: () -> Unit,
    onAccount: () -> Unit,
    onOpenRecord: (HistoryRecord) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(Modifier.fillMaxSize().background(KkokColors.Background).padding(top = top)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = KkokSpacing.Page).padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HistoryHeader(state, viewModel::toggleSearch, viewModel::updateQuery)
            HistorySegment(state.kind, viewModel::selectKind)
            when {
                state.isLoading -> KkokText("기록을 불러오고 있어요…", Modifier.padding(vertical = 28.dp), color = KkokColors.Gray600)
                state.error -> EmptyHistory("기록을 불러오지 못했어요", "다시 시도해 주세요", onRetry = viewModel::retry)
                state.visibleRecords.isEmpty() -> EmptyHistory("검색 결과가 없어요", "다른 이름으로 찾아보세요")
                else -> {
                    state.visibleRecords.groupBy { it.group }.forEach { (group, records) ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            KkokText(group, style = KkokTypography.LabelLarge, color = KkokColors.Gray500)
                            records.forEach { record ->
                                HistoryRecordCard(record, onClick = if (record.kind == HistoryKind.PHOTO) {
                                    { onOpenRecord(record) }
                                } else null)
                            }
                        }
                    }
                }
            }
            RetentionNotice()
        }
        HistoryTabBar(onHome = onHome, onProtectionHistory = onProtectionHistory, onAccount = onAccount)
    }
}

@Composable
private fun HistoryHeader(state: HistoryState, onToggleSearch: () -> Unit, onQuery: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
        if (state.isSearchOpen) {
            BasicTextField(
                value = state.query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = KkokTypography.HeadlineMedium.copy(color = KkokColors.Ink),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth()) {
                        if (state.query.isEmpty()) KkokText("기록 검색", style = KkokTypography.HeadlineMedium, color = KkokColors.Gray400)
                        inner()
                    }
                },
                modifier = Modifier.weight(1f).testTag("history-search"),
            )
        } else {
            KkokText("검사 기록", Modifier.weight(1f), KkokTypography.HeadlineMedium)
            KkokText("모의 자료", style = KkokTypography.LabelSmall, color = KkokColors.Primary)
            Spacer(Modifier.width(10.dp))
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(KkokColors.White)
                .clickable(role = Role.Button, onClick = onToggleSearch)
                .semantics { contentDescription = if (state.isSearchOpen) "검색 닫기" else "기록 검색"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            if (state.isSearchOpen) KkokText("×", style = KkokTypography.TitleLarge, color = KkokColors.Gray600)
            else SearchIcon()
        }
    }
}

@Composable
private fun SearchIcon() {
    Canvas(Modifier.size(20.dp)) {
        val stroke = Stroke(2.dp.toPx())
        drawCircle(KkokColors.Ink, radius = 6.dp.toPx(), center = Offset(size.width * .43f, size.height * .43f), style = stroke)
        drawLine(KkokColors.Ink, Offset(size.width * .62f, size.height * .62f), Offset(size.width * .9f, size.height * .9f), 2.dp.toPx())
    }
}

@Composable
private fun HistorySegment(kind: HistoryKind, onSelect: (HistoryKind) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(CircleShape).background(KkokColors.Gray100).padding(4.dp)) {
        HistoryKind.entries.forEach { item ->
            val selected = item == kind
            Box(
                Modifier.weight(1f).clip(CircleShape).background(if (selected) KkokColors.White else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(item) }.padding(vertical = 10.dp)
                    .semantics { role = Role.Tab; this.selected = selected },
                contentAlignment = Alignment.Center,
            ) {
                KkokText(if (item == HistoryKind.PHOTO) "사진 검사" else "SNS 분석", style = KkokTypography.TitleMedium,
                    color = if (selected) KkokColors.Ink else KkokColors.Gray500)
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(record: HistoryRecord, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(KkokColors.White)
            .then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .semantics(mergeDescendants = true) {
                contentDescription = "${record.title} 기록"
                if (onClick != null) role = Role.Button else disabled()
            }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MockThumbnail(record.id.hashCode().absoluteModulo(4))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            KkokText(record.title, style = KkokTypography.TitleMedium)
            KkokText("${record.detail} · 단서 ${record.clueCount}개", style = KkokTypography.BodySmall, color = KkokColors.Gray500)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val score = if (record.protected && record.protectedScore != null) "점수 ${record.score} → ${record.protectedScore}" else "점수 ${record.score}"
                KkokText(score, style = KkokTypography.LabelMedium, color = if (record.protected) KkokColors.Primary else KkokColors.Gray600)
                if (record.protected) {
                    KkokText("보호 완료", Modifier.clip(CircleShape).background(KkokColors.Secondary).padding(horizontal = 8.dp, vertical = 2.dp),
                        KkokTypography.LabelMedium, KkokColors.Primary)
                }
            }
        }
        HistoryRiskBadge(record.risk)
    }
}

@Composable
private fun MockThumbnail(variant: Int) {
    val sky = when (variant) {
        0 -> Color(0xFFE8EEE8)
        1 -> Color(0xFFE3EBEF)
        2 -> Color(0xFFE8E3DC)
        else -> Color(0xFFF1E5D8)
    }
    Canvas(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(sky)) {
        val w = size.width
        val h = size.height
        when (variant) {
            0 -> {
                drawRect(Color(0xFFB2C7B3), Offset(0f, h * .62f), androidx.compose.ui.geometry.Size(w, h * .38f))
                drawRoundRect(Color(0xFF9B8269), Offset(w * .08f, h * .12f), androidx.compose.ui.geometry.Size(w * .32f, h * .68f), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                drawRect(Color(0xFFFFF8ED), Offset(w * .45f, h * .2f), androidx.compose.ui.geometry.Size(w * .4f, h * .46f))
                drawCircle(Color(0xFF846A53), w * .12f, Offset(w * .66f, h * .72f))
            }
            1 -> {
                drawRect(Color(0xFFBCCED3), Offset(0f, h * .58f), androidx.compose.ui.geometry.Size(w, h * .42f))
                repeat(3) { index -> drawRect(Color(0xFF758C82), Offset(w * (.08f + .3f * index), h * (.24f + (index % 2) * .12f)), androidx.compose.ui.geometry.Size(w * .2f, h * .46f)) }
                drawCircle(KkokColors.Point, w * .1f, Offset(w * .78f, h * .18f))
            }
            2 -> {
                drawRect(Color(0xFFB9C7A7), Offset(0f, h * .58f), androidx.compose.ui.geometry.Size(w, h * .42f))
                val hill = Path().apply { moveTo(0f, h * .7f); lineTo(w * .45f, h * .28f); lineTo(w, h * .62f); lineTo(w, h); lineTo(0f, h); close() }
                drawPath(hill, Color(0xFF8C9B79))
                drawCircle(Color(0xFFF9D47C), w * .12f, Offset(w * .76f, h * .2f))
            }
            else -> {
                drawRect(Color(0xFFD8C8B7), Offset(0f, h * .62f), androidx.compose.ui.geometry.Size(w, h * .38f))
                drawRoundRect(Color(0xFFFBF6EE), Offset(w * .17f, h * .2f), androidx.compose.ui.geometry.Size(w * .66f, h * .48f), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                drawCircle(Color(0xFFFFD78A), w * .15f, Offset(w * .5f, h * .43f))
            }
        }
    }
}

@Composable
private fun HistoryRiskBadge(risk: HistoryRisk) {
    val (label, color, textColor) = when (risk) {
        HistoryRisk.HIGH -> Triple("높음", KkokColors.High, KkokColors.White)
        HistoryRisk.CAUTION -> Triple("주의", KkokColors.Point, KkokColors.Ink)
        HistoryRisk.LOW -> Triple("낮음", KkokColors.Safe, KkokColors.White)
    }
    KkokText(label, Modifier.clip(CircleShape).background(color).padding(horizontal = 10.dp, vertical = 4.dp), KkokTypography.LabelLarge, textColor)
}

@Composable
private fun RetentionNotice() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HistoryLockIcon()
        KkokText("기록은 30일 뒤 자동으로 지워져요 · 모의 기록", style = KkokTypography.LabelMedium, color = KkokColors.Gray400)
    }
}

@Composable
private fun HistoryLockIcon() {
    Canvas(Modifier.size(14.dp)) {
        val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(KkokColors.Gray400, Offset(2.dp.toPx(), 6.dp.toPx()),
            androidx.compose.ui.geometry.Size(size.width - 4.dp.toPx(), size.height - 8.dp.toPx()),
            androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = stroke)
        drawArc(KkokColors.Gray400, 180f, 180f, false, Offset(4.dp.toPx(), 1.dp.toPx()),
            androidx.compose.ui.geometry.Size(size.width - 8.dp.toPx(), 10.dp.toPx()), style = stroke)
    }
}

@Composable
private fun EmptyHistory(title: String, message: String, onRetry: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KkokText(title, style = KkokTypography.TitleMedium)
        KkokText(message, style = KkokTypography.BodySmall, color = KkokColors.Gray500)
        if (onRetry != null) {
            KkokText("다시 불러오기", Modifier.clip(CircleShape).background(KkokColors.Secondary)
                .clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 14.dp, vertical = 8.dp),
                KkokTypography.LabelLarge, KkokColors.Primary)
        }
    }
}

@Composable
private fun HistoryTabBar(onHome: () -> Unit, onProtectionHistory: () -> Unit, onAccount: () -> Unit) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(18.dp)
    Row(
        Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(horizontal = 28.dp).padding(top = 10.dp, bottom = bottom),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HistoryTab(R.drawable.figma_home, "홈", selected = false, onClick = onHome)
        HistoryTab(R.drawable.figma_clock, "검사 기록", selected = true, onClick = {})
        HistoryTab(R.drawable.figma_pencil, "보호 기록", selected = false, onClick = onProtectionHistory)
        HistoryTab(R.drawable.figma_user, "마이", selected = false, onClick = onAccount)
    }
}

@Composable
private fun HistoryTab(icon: Int, label: String, selected: Boolean, onClick: (() -> Unit)?) {
    Column(
        Modifier.then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Tab, onClick = onClick))
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                if (onClick == null && !selected) disabled()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.clip(CircleShape).background(if (selected) KkokColors.Secondary else KkokColors.White).padding(horizontal = 14.dp, vertical = 4.dp)) {
            FigmaIcon(icon)
        }
        KkokText(label, style = KkokTypography.LabelMedium, color = if (selected) KkokColors.Primary else KkokColors.Gray400)
    }
}

private fun Int.absoluteModulo(divisor: Int): Int = kotlin.math.abs(this % divisor)
