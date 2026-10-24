package com.kkokkkok.android.ui.snscheck

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.kkokkkok.android.R
import com.kkokkkok.android.snscheck.SnsCumulativeState
import com.kkokkkok.android.snscheck.SnsRepeatedCue
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

@Composable
internal fun RepeatedCluesScreen(
    state: SnsCumulativeState,
    onBack: () -> Unit,
    onAnswer: (String, Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val selected = state.selectedPostIds
    val posts = state.posts.associateBy { it.id }
    val clues = state.clues.filter { cue -> cue.matchedPostIds.any { it in selected } }

    SnsFrame(
        title = "반복 단서 확인", right = "3 / 3", onBack = onBack,
        footer = { SnsPrimaryButton("누적 위험 확인하기", enabled = clues.isNotEmpty(), onClick = onNext) },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FigmaIcon(R.drawable.figma_mascot, Modifier.size(44.dp))
            Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(KkokColors.White)
                .padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val placeName = state.places.firstOrNull { it.id == state.selectedPlaceId }?.name ?: "선택 장소"
                KkokText("‘$placeName’ 게시물에서 반복된 단서예요", style = KkokTypography.TitleMedium)
                KkokText("이 장소의 단서가 맞는지 확인해 주세요", style = KkokTypography.BodySmall, color = KkokColors.Gray600)
            }
        }
        if (clues.isEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.White)
                .padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KkokText("반복된 모의 단서가 없어요", style = KkokTypography.TitleMedium)
                KkokText("다른 게시물을 골라 확인해 주세요.", style = KkokTypography.BodyMedium, color = KkokColors.Gray600)
            }
        } else {
            clues.forEach { cue ->
                val matchedCount = cue.matchedPostIds.count { it in selected }
                val thumbnails = cue.previewPostIds.filter { it in selected }.mapNotNull(posts::get)
                RepeatedCueCard(
                    cue = cue,
                    count = matchedCount,
                    thumbnails = thumbnails.map { it.thumbnail },
                    matchesPlace = state.clueAnswers[cue.id] == true,
                    onAnswer = { onAnswer(cue.id, it) },
                )
            }
        }
        KkokText("모의 단서 예시예요. 실제 사진이나 SNS 게시물을 분석하지 않았어요.",
            style = KkokTypography.BodySmall, color = KkokColors.Gray500)
    }
}

@Composable
private fun RepeatedCueCard(
    cue: SnsRepeatedCue,
    count: Int,
    thumbnails: List<Int>,
    matchesPlace: Boolean,
    onAnswer: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.White)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KkokText(cue.title, Modifier.weight(1f), KkokTypography.TitleMedium)
            KkokText("사진 ${count}장", style = KkokTypography.BodySmall, color = KkokColors.Gray500)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            thumbnails.forEach { thumbnail ->
                Image(
                    painter = painterResource(thumbnail), contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KkokText("이 장소 단서인가요?", Modifier.weight(1f), KkokTypography.LabelLarge, KkokColors.Gray600)
            AnswerChip("맞아요", selected = matchesPlace, selectedColor = KkokColors.Primary,
                cueTitle = cue.title, onClick = { onAnswer(true) })
            AnswerChip("아니에요", selected = !matchesPlace, selectedColor = KkokColors.Ink,
                cueTitle = cue.title, onClick = { onAnswer(false) })
        }
    }
}

@Composable
private fun AnswerChip(
    label: String,
    selected: Boolean,
    selectedColor: androidx.compose.ui.graphics.Color,
    cueTitle: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) selectedColor else KkokColors.White)
            .border(if (selected) 0.dp else 1.5.dp, if (selected) selectedColor else KkokColors.Gray200, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "$cueTitle $label" }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        KkokText(label, style = KkokTypography.LabelLarge,
            color = if (selected) KkokColors.White else KkokColors.Gray600)
    }
}
