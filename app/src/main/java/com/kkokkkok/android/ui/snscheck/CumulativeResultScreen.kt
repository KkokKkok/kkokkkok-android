package com.kkokkkok.android.ui.snscheck

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import com.kkokkkok.android.R
import com.kkokkkok.android.snscheck.SnsExposureResult
import com.kkokkkok.android.snscheck.SnsPlaceKind
import com.kkokkkok.android.snscheck.SnsPlaceOption
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

@Composable
internal fun CumulativeResultScreen(
    result: SnsExposureResult,
    place: SnsPlaceOption?,
    onBack: () -> Unit,
    onOtherPlace: () -> Unit,
    onConfirm: () -> Unit,
) {
    SnsFrame(
        title = "누적 노출 결과", centerTitle = true, onBack = onBack,
        footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).height(60.dp).clip(CircleShape).background(KkokColors.White)
                    .border(1.5.dp, KkokColors.Gray200, CircleShape)
                    .clickable(role = Role.Button, onClick = onOtherPlace), contentAlignment = Alignment.Center) {
                    KkokText("다른 장소 검사", style = KkokTypography.TitleLarge)
                }
                Box(Modifier.weight(1f).shadow(8.dp, CircleShape,
                    ambientColor = KkokColors.Primary.copy(alpha = .32f),
                    spotColor = KkokColors.Primary.copy(alpha = .32f))) {
                    SnsPrimaryButton("확인", onClick = onConfirm)
                }
            }
        },
    ) {
        ExposureSummary(result, place)
        ResultCard {
            KkokText("반복된 단서", style = KkokTypography.TitleMedium)
            if (result.cues.isEmpty()) {
                KkokText("이 장소로 확인한 모의 단서가 없어요.", color = KkokColors.Gray600)
            }
            val total = result.cues.sumOf { it.postCount }.coerceAtLeast(1)
            result.cues.forEachIndexed { index, cue ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        KkokText(cue.title, Modifier.weight(1f))
                        KkokText("${cue.postCount}장", style = KkokTypography.LabelLarge, color = KkokColors.Gray600)
                    }
                    val fraction = cue.postCount.toFloat() / total
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(KkokColors.Gray100)
                        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }) {
                        Box(Modifier.fillMaxWidth(fraction).height(10.dp).clip(CircleShape)
                            .background(if (index == 0) KkokColors.High else KkokColors.Point))
                    }
                }
            }
        }
        ResultCard(background = KkokColors.Secondary) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                KkokText("단서가 보이는 게시물 ${result.posts.size}개", Modifier.weight(1f), KkokTypography.TitleMedium)
                KkokText("전체 보기", Modifier.semantics { role = Role.Button; disabled() },
                    KkokTypography.LabelLarge, KkokColors.Gray500)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                result.posts.take(5).forEach { post ->
                    Image(painterResource(post.thumbnail), contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                }
            }
            Row(Modifier.fillMaxWidth().clip(CircleShape).background(KkokColors.White)
                .semantics { role = Role.Button; disabled(); contentDescription = "이 게시물들 보호 편집하기, 준비 중" }
                .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                FigmaIcon(R.drawable.figma_sns_result_shield)
                KkokText("이 게시물들 보호 편집하기", style = KkokTypography.TitleMedium, color = KkokColors.Gray500)
            }
        }
        ResultCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).background(KkokColors.Point, CircleShape), contentAlignment = Alignment.Center) {
                    FigmaIcon(R.drawable.figma_sns_result_bell)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    KkokText("반복 노출 미리 알림", style = KkokTypography.TitleMedium)
                    KkokText("새 사진이 이 장소와 비슷하면 검사할 때 알려드려요", style = KkokTypography.BodySmall.copy(lineBreak = LineBreak.Simple), color = KkokColors.Gray600)
                }
                Box(Modifier.size(width = 44.dp, height = 26.dp).clip(CircleShape).background(KkokColors.Gray200)
                    .semantics {
                        role = Role.Switch
                        disabled()
                        contentDescription = "반복 노출 미리 알림"
                        stateDescription = "꺼짐 · 준비 중"
                    }) {
                    Box(Modifier.align(Alignment.CenterStart).padding(2.dp).size(22.dp).background(KkokColors.White, CircleShape))
                }
            }
        }
        if (result.isMock) {
            KkokText("모의 결과예요. 점수·위험도는 고정 예시예요.\n실제 사진이나 SNS 게시물을 분석하지 않았어요.",
                style = KkokTypography.BodySmall, color = KkokColors.Gray500)
        }
        KkokText("전체 보기·일괄 보호 편집·미리 알림은 준비 중이에요.",
            style = KkokTypography.BodySmall, color = KkokColors.Gray500)
    }
}

@Composable
private fun ExposureSummary(result: SnsExposureResult, place: SnsPlaceOption?) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(KkokColors.White).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).background(if (place?.kind == SnsPlaceKind.SCHOOL) KkokColors.Primary else KkokColors.Secondary, CircleShape), contentAlignment = Alignment.Center) {
                FigmaIcon(when (place?.kind) {
                    SnsPlaceKind.SCHOOL -> R.drawable.figma_place_school
                    SnsPlaceKind.OTHER -> R.drawable.figma_scan
                    else -> R.drawable.figma_sns_result_home
                })
            }
            Column(Modifier.weight(1f)) {
                KkokText(place?.name ?: "선택 장소", style = KkokTypography.TitleMedium)
                KkokText(place?.detail.orEmpty(), style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
            }
            if (result.cues.isNotEmpty()) {
                Box(Modifier.clip(CircleShape).background(KkokColors.High).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    KkokText(result.sampleRiskLabel, style = KkokTypography.LabelLarge, color = KkokColors.White)
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KkokText(if (result.cues.isEmpty()) "—" else result.sampleScore.toString(), style = KkokTypography.Score, color = KkokColors.High)
            KkokText("/ 100", style = KkokTypography.TitleMedium, color = KkokColors.Gray400)
        }
        KkokText("게시물 ${result.selectedPostCount}개 중 ${result.posts.size}개에 이 장소의 단서가 반복돼요",
            style = KkokTypography.TitleLarge.copy(lineBreak = LineBreak.Simple))
        KkokText("한 장씩은 괜찮아 보여도, 모아 보면 생활권이 드러날 수\n있어요",
            style = KkokTypography.BodySmall.copy(lineBreak = LineBreak.Simple), color = KkokColors.Gray600)
    }
}

@Composable
private fun ResultCard(
    background: androidx.compose.ui.graphics.Color = KkokColors.White,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
