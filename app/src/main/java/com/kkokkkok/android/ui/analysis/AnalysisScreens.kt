package com.kkokkkok.android.ui.analysis

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kkokkkok.android.R
import com.kkokkkok.android.analysis.*
import com.kkokkkok.android.ui.design.*

internal fun ClueRisk.color(): Color = when (this) {
    ClueRisk.HIGH -> KkokColors.High
    ClueRisk.CAUTION -> KkokColors.Point
    ClueRisk.LOW -> KkokColors.Safe
}
internal fun ClueRisk.textColor(): Color = if (this == ClueRisk.CAUTION) KkokColors.Ink else KkokColors.White

@Composable
private fun RiskBadge(risk: ClueRisk) {
    val label = when (risk) {
        ClueRisk.HIGH -> R.string.result_high
        ClueRisk.CAUTION -> R.string.result_caution
        ClueRisk.LOW -> R.string.result_low
    }
    KkokText(stringResource(label), Modifier.background(risk.color(), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        KkokTypography.LabelLarge, risk.textColor())
}

@Composable
private fun ProgressTrack(percent: Int, color: Color, height: Int) {
    Box(Modifier.fillMaxWidth().height(height.dp).clip(CircleShape).background(KkokColors.Gray100)
        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(percent / 100f, 0f..1f) }) {
        Box(Modifier.fillMaxWidth(percent / 100f).fillMaxHeight().background(color, CircleShape))
    }
}

@Composable
internal fun AnalysisRunningScreen(photo: PhotoImage, state: AnalysisState.Running, onCancel: () -> Unit) {
    AnalysisFrame(stringResource(R.string.analysis_title), onCancel,
        footer = { AnalysisButton(stringResource(R.string.analysis_cancel), onClick = onCancel) }) {
        PhotoPreview(photo, scan = state.percent)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FigmaIcon(R.drawable.figma_analysis_mascot)
            Column(Modifier.weight(1f).background(KkokColors.White, RoundedCornerShape(20.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KkokText(stringResource(R.string.analysis_message), style = KkokTypography.TitleLarge)
                KkokText(stringResource(R.string.analysis_notice), style = KkokTypography.BodySmall, color = KkokColors.Gray600)
            }
        }
        Column(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(24.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalysisStep.entries.forEach { step ->
                val done = step.ordinal < state.step.ordinal
                val active = step == state.step
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        done -> Box(Modifier.size(24.dp).background(KkokColors.Safe, CircleShape), contentAlignment = Alignment.Center) {
                            FigmaIcon(R.drawable.figma_analysis_check)
                        }
                        active -> FigmaIcon(R.drawable.figma_analysis_dot)
                        else -> Box(Modifier.size(24.dp).background(KkokColors.Gray100, CircleShape))
                    }
                    val label = when (step) {
                        AnalysisStep.METADATA -> R.string.analysis_metadata
                        AnalysisStep.TEXT -> R.string.analysis_text
                        AnalysisStep.SIGNS -> R.string.analysis_signs
                        AnalysisStep.BUILDINGS -> R.string.analysis_buildings
                    }
                    KkokText(stringResource(label), Modifier.weight(1f), KkokTypography.BodyMedium,
                        if (done || active) KkokColors.Ink else KkokColors.Gray400)
                    KkokText(stringResource(if (done) R.string.analysis_done else if (active) R.string.analysis_active else R.string.analysis_waiting),
                        style = KkokTypography.LabelMedium, color = if (active) KkokColors.Primary else KkokColors.Gray500)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                KkokText(stringResource(R.string.analysis_progress), Modifier.weight(1f), KkokTypography.BodyMedium, KkokColors.Gray600)
                KkokText("${state.percent}%", style = KkokTypography.LabelLarge, color = KkokColors.Primary)
            }
            ProgressTrack(state.percent, KkokColors.Primary, 10)
        }
    }
}

@Composable
internal fun AnalysisResultScreen(
    photo: PhotoImage,
    result: AnalysisResult,
    onBack: () -> Unit,
    onProtect: () -> Unit = {},
) {
    val risk = if (result.score >= 70) ClueRisk.HIGH else if (result.score >= 40) ClueRisk.CAUTION else ClueRisk.LOW
    var showSaveWarning by remember(result.score) { mutableStateOf(false) }
    val context = LocalContext.current
    if (showSaveWarning) {
        Dialog(
            onDismissRequest = { showSaveWarning = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 15.dp).clip(RoundedCornerShape(28.dp))
                    .background(KkokColors.White).padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FigmaIcon(R.drawable.figma_mascot, Modifier.size(52.dp))
                KkokText("아직 위험 단서가 남아 있어요", style = KkokTypography.TitleLarge)
                KkokText(
                    "위험 점수 ${result.score} · ${if (risk == ClueRisk.HIGH) "높음" else "주의"}\n이대로 올리면 간판과 GPS 정보로 위치가 드러날 수 있어요.",
                    style = KkokTypography.BodySmall.copy(textAlign = TextAlign.Center),
                    color = KkokColors.Gray600,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SaveWarningButton("그래도 저장", Modifier.weight(1f)) {
                        showSaveWarning = false
                        Toast.makeText(context, "모의 결과는 원본 사진을 저장하지 않아요.", Toast.LENGTH_SHORT).show()
                    }
                    SaveWarningButton("보호 편집하기", Modifier.weight(1f), primary = true) {
                        showSaveWarning = false
                        onProtect()
                    }
                }
            }
        }
    }
    AnalysisFrame(stringResource(R.string.result_title), onBack, footer = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnalysisButton(stringResource(R.string.result_save), Modifier.weight(1f), onClick = { showSaveWarning = true })
            AnalysisButton(stringResource(R.string.result_edit), Modifier.weight(1f), primary = true, onClick = onProtect)
        }
    }) {
        Column(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(28.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KkokText(stringResource(R.string.result_score_title), Modifier.weight(1f), KkokTypography.BodyMedium, KkokColors.Gray500)
                RiskBadge(risk)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KkokText("${result.score}", style = KkokTypography.Score, color = risk.color())
                KkokText("/ 100", Modifier.padding(bottom = 6.dp), KkokTypography.TitleMedium, KkokColors.Gray400)
            }
            ProgressTrack(result.score, risk.color(), 12)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(R.string.result_gauge_low, R.string.result_gauge_caution, R.string.result_gauge_high, R.string.result_gauge_max).forEach {
                    KkokText(stringResource(it), style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
                }
            }
            KkokText(stringResource(R.string.result_count, result.clues.size), style = KkokTypography.TitleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FigmaIcon(R.drawable.figma_info)
                KkokText(stringResource(R.string.result_notice), style = KkokTypography.BodySmall, color = KkokColors.Gray500)
            }
        }
        PhotoPreview(photo, result.clues)
        KkokText(stringResource(R.string.result_risks), style = KkokTypography.TitleLarge)
        result.clues.forEachIndexed { index, clue ->
            Row(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(20.dp)).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(26.dp).background(clue.risk.color(), CircleShape), contentAlignment = Alignment.Center) {
                    KkokText("${index + 1}", style = KkokTypography.LabelLarge, color = clue.risk.textColor())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KkokText(clue.title, Modifier.weight(1f), KkokTypography.TitleMedium)
                        RiskBadge(clue.risk)
                    }
                    val category = when (clue.kind) {
                        ClueKind.TEXT -> R.string.result_text_category
                        ClueKind.BUILDING -> R.string.result_building_category
                        ClueKind.METADATA -> R.string.result_metadata_category
                    }
                    KkokText(stringResource(category), style = KkokTypography.LabelMedium, color = KkokColors.Primary)
                    KkokText(clue.description, style = KkokTypography.BodySmall, color = KkokColors.Gray600)
                }
            }
        }
    }
}

@Composable
private fun SaveWarningButton(label: String, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(CircleShape)
            .background(if (primary) KkokColors.Primary else KkokColors.White)
            .border(if (primary) 0.dp else 1.5.dp, if (primary) KkokColors.Primary else KkokColors.Gray200, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KkokText(label, style = KkokTypography.TitleMedium, color = if (primary) KkokColors.White else KkokColors.Ink)
    }
}

@Composable
internal fun AnalysisFailureScreen(onRetry: () -> Unit, onBack: () -> Unit, onHome: () -> Unit) {
    AnalysisFrame(stringResource(R.string.analysis_title), onBack, footer = {
        AnalysisButton(stringResource(R.string.analysis_retry), primary = true, onClick = onRetry)
        AnalysisButton(stringResource(R.string.analysis_home), onClick = onHome)
    }) {
        Column(Modifier.fillMaxWidth().heightIn(min = 440.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
            FigmaIcon(R.drawable.figma_sad_mascot)
            KkokText(stringResource(R.string.analysis_failure_title), style = KkokTypography.HeadlineSmall.copy(textAlign = TextAlign.Center))
            KkokText(stringResource(R.string.analysis_failure_description), style = KkokTypography.BodyMedium.copy(textAlign = TextAlign.Center),
                color = KkokColors.Gray600)
        }
    }
}
