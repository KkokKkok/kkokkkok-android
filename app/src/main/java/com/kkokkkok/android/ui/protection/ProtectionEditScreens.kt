package com.kkokkkok.android.ui.protection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import com.kkokkkok.android.R
import com.kkokkkok.android.protection.*
import com.kkokkkok.android.ui.analysis.PhotoImage
import com.kkokkkok.android.ui.analysis.rememberPhotoImage
import com.kkokkkok.android.ui.design.*

@Composable
internal fun ProtectionEditFlow(
    state: ProtectionEditState,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit,
    onLocation: (String) -> Unit,
    onLocationCategory: (String) -> Unit,
    onContinue: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleTarget: (String) -> Unit,
    onStrength: (Float) -> Unit,
    onRequest: (String) -> Unit,
    onSuggestion: (String) -> Unit,
    onCreate: () -> Unit,
    onNotSaved: () -> Unit,
) {
    when (state.step) {
        ProtectionStep.LOCATION -> LocationScreen(state, onBack, onChangePhoto, onLocation, onLocationCategory, onContinue)
        ProtectionStep.TARGETS -> TargetScreen(state, onBack, onToggleMode, onToggleTarget, onStrength, onContinue)
        ProtectionStep.REQUEST -> RequestScreen(state, onBack, onRequest, onSuggestion, onCreate)
        ProtectionStep.RESULT -> PreviewScreen(state, onBack, onNotSaved)
    }
}

@Composable
private fun EditFrame(
    title: String = "보호 편집",
    step: String,
    onBack: () -> Unit,
    footerLabel: String,
    footerEnabled: Boolean = true,
    onFooter: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(40.dp)
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(18.dp)
    Column(Modifier.fillMaxSize().background(KkokColors.Background)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).padding(top = top)) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            Box(Modifier.padding(start = 8.dp).size(48.dp).align(Alignment.CenterStart)
                .clip(CircleShape).background(KkokColors.White).clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "뒤로" }, contentAlignment = Alignment.Center) {
                FigmaIcon(R.drawable.figma_back)
            }
            KkokText(title, Modifier.align(Alignment.Center), KkokTypography.TitleLarge)
            KkokText(step, Modifier.align(Alignment.CenterEnd).padding(end = 20.dp), KkokTypography.LabelLarge, KkokColors.Primary)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        EditButton(footerLabel, Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = bottom),
            enabled = footerEnabled, onClick = onFooter)
    }
}

@Composable
private fun LocationScreen(state: ProtectionEditState, onBack: () -> Unit, onChangePhoto: () -> Unit, onLocation: (String) -> Unit, onLocationCategory: (String) -> Unit, onContinue: () -> Unit) {
    val photo = state.photo?.let { rememberPhotoImage(it, 0) }
    EditFrame(step = "2 / 4", onBack = onBack, footerLabel = "분석하고 수정하기", footerEnabled = state.location.isNotBlank(), onFooter = onContinue) {
        Row(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(22.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PhotoThumb(photo, Modifier.size(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KkokText("선택한 사진", style = KkokTypography.LabelMedium, color = KkokColors.Gray500)
                KkokText("사진이 선택되었어요", style = KkokTypography.TitleMedium)
            }
            KkokText("변경", Modifier.clickable(role = Role.Button, onClick = onChangePhoto), KkokTypography.LabelLarge, KkokColors.Primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KkokText("어디에서 찍은 사진인가요?", style = KkokTypography.HeadlineSmall)
            }
            KkokText("필수", Modifier.background(KkokColors.High, CircleShape).padding(horizontal = 14.dp, vertical = 7.dp),
                KkokTypography.LabelLarge, KkokColors.White)
        }
        KkokText("위치를 알아야 이 장소를 드러내는 단서만 골라서 지울 수 있어요.", color = KkokColors.Gray500)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(KkokColors.White)
            .border(2.dp, KkokColors.Primary, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KkokText("⌕", style = KkokTypography.TitleLarge, color = KkokColors.Gray500)
            BasicTextField(value = state.location, onValueChange = onLocation, singleLine = true,
                textStyle = KkokTypography.BodyMedium.copy(color = KkokColors.Ink), modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner -> Box { if (state.location.isEmpty()) KkokText("장소를 입력해 주세요", color = KkokColors.Gray400); inner() } })
        }
        MapPlaceholder()
        Row(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KkokText("⌖", Modifier.background(KkokColors.Secondary, CircleShape).padding(8.dp), KkokTypography.TitleMedium, KkokColors.Primary)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                KkokText("꼭꼭로 24 · 2층", style = KkokTypography.TitleMedium)
                KkokText("이 위치는 이 화면에서만 사용해요", style = KkokTypography.LabelMedium, color = KkokColors.Gray500)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("집 주변", "학교", "직장", "자주 가는 곳").forEach { label ->
                LocationCategoryChip(label, state.locationCategory == label, Modifier.weight(1f)) { onLocationCategory(label) }
            }
        }
        Row(Modifier.fillMaxWidth().background(KkokColors.Point.copy(alpha = .4f), RoundedCornerShape(18.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LockIcon()
            KkokText("입력한 위치는 위험 판단에만 쓰이며, 저장하거나 공유하지 않아요.",
                style = KkokTypography.LabelMedium, color = KkokColors.Ink)
        }
    }
}

@Composable
private fun MapPlaceholder() {
    Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFF2EDEA))) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) { Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp).background(Color(0xFFE5DDD8), RoundedCornerShape(10.dp))) }
            } }
        }
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(16.dp).background(KkokColors.White))
        Box(Modifier.align(Alignment.Center).fillMaxHeight().width(14.dp).background(KkokColors.White))
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = 60.dp.toPx()
            drawCircle(KkokColors.Primary.copy(alpha = .13f), radius, center)
            drawCircle(KkokColors.Primary, radius, center, style = Stroke(1.5.dp.toPx(), pathEffect =
                PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
            val halfWidth = 12.dp.toPx()
            val headY = center.y - 22.dp.toPx()
            val pin = Path().apply {
                moveTo(center.x, center.y + 22.dp.toPx())
                cubicTo(center.x - 4.dp.toPx(), center.y + 12.dp.toPx(), center.x - halfWidth, center.y - 2.dp.toPx(), center.x - halfWidth, headY)
                cubicTo(center.x - halfWidth, center.y - 40.dp.toPx(), center.x + halfWidth, center.y - 40.dp.toPx(), center.x + halfWidth, headY)
                cubicTo(center.x + halfWidth, center.y - 2.dp.toPx(), center.x + 4.dp.toPx(), center.y + 12.dp.toPx(), center.x, center.y + 22.dp.toPx())
                close()
            }
            drawPath(pin, KkokColors.Primary)
            drawCircle(KkokColors.White, 5.dp.toPx(), Offset(center.x, headY))
        }
    }
}

@Composable
private fun LockIcon(color: Color = KkokColors.Ink) {
    Canvas(Modifier.size(18.dp)) {
        val width = 2.dp.toPx()
        val shackle = Path().apply {
            moveTo(size.width * .28f, size.height * .48f)
            lineTo(size.width * .28f, size.height * .32f)
            cubicTo(size.width * .28f, size.height * .02f, size.width * .72f, size.height * .02f, size.width * .72f, size.height * .32f)
            lineTo(size.width * .72f, size.height * .48f)
        }
        drawPath(shackle, color, style = Stroke(width, cap = StrokeCap.Round))
        drawRoundRect(color, Offset(size.width * .16f, size.height * .42f),
            androidx.compose.ui.geometry.Size(size.width * .68f, size.height * .53f), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        drawCircle(KkokColors.Background, 1.dp.toPx(), Offset(size.width * .5f, size.height * .66f))
    }
}

@Composable
private fun TargetScreen(
    state: ProtectionEditState,
    onBack: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleTarget: (String) -> Unit,
    onStrength: (Float) -> Unit,
    onContinue: () -> Unit,
) {
    val photo = state.photo?.let { rememberPhotoImage(it, 0) }
    val targets = listOf("창밖 간판", "창밖 건물 · 동 번호", "위치 정보(GPS)")
    EditFrame(step = "3 / 4", onBack = onBack, footerLabel = "다음 · 요청 입력", onFooter = onContinue) {
        Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp)).background(KkokColors.White)) {
            PhotoImageLayer(photo)
            TargetHighlight("2", KkokColors.Point, Modifier.align(Alignment.TopStart).offset(x = 30.dp, y = 2.dp).size(150.dp, 74.dp))
            TargetHighlight("1", KkokColors.High, Modifier.align(Alignment.TopStart).offset(x = 100.dp, y = 67.dp).size(82.dp, 28.dp))
        }
        Row(Modifier.fillMaxWidth().background(KkokColors.Gray100, CircleShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SegmentedChoice("자동 수정", state.mode == EditMode.AUTOMATIC, Modifier.weight(1f), onToggleMode)
            SegmentedChoice("직접 고르기", state.mode == EditMode.MANUAL, Modifier.weight(1f), onToggleMode)
        }
        Column(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(24.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            targets.forEachIndexed { index, target ->
                val selected = target in state.selectedTargets
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(
                    enabled = index != 2, role = Role.Checkbox, onClick = { onToggleTarget(target) }).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (index == 2) {
                        Box(Modifier.size(26.dp).background(KkokColors.Gray100, CircleShape), contentAlignment = Alignment.Center) {
                            LockIcon(KkokColors.Gray500)
                        }
                    } else KkokText(if (selected) "✓" else "○", Modifier.size(26.dp).background(if (selected) KkokColors.Primary else KkokColors.Gray100, CircleShape),
                        KkokTypography.TitleMedium, if (selected) KkokColors.White else KkokColors.Gray500)
                    KkokText("${index + 1}", Modifier.background(if (index == 1) KkokColors.Point else KkokColors.High, CircleShape).padding(horizontal = 7.dp, vertical = 3.dp),
                        KkokTypography.LabelMedium, if (index == 1) KkokColors.Ink else KkokColors.White)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        KkokText(target, style = KkokTypography.TitleMedium)
                        KkokText(listOf("간판 글자만 자연스럽게 지우기", "창밖을 평범한 풍경으로 바꾸기", "저장할 때 항상 삭제돼요")[index],
                            style = KkokTypography.LabelMedium, color = KkokColors.Gray500)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            KkokText("수정 강도", Modifier.weight(1f), KkokTypography.TitleMedium)
            KkokText("자연스럽게", style = KkokTypography.LabelLarge, color = KkokColors.Primary)
        }
        StrengthSlider(state.strength, onStrength)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            KkokText("살짝", style = KkokTypography.LabelMedium, color = KkokColors.Gray400)
            KkokText("자연스럽게", style = KkokTypography.LabelMedium, color = KkokColors.Gray400)
            KkokText("확실하게", style = KkokTypography.LabelMedium, color = KkokColors.Gray400)
        }
    }
}

@Composable
private fun RequestScreen(
    state: ProtectionEditState,
    onBack: () -> Unit,
    onRequest: (String) -> Unit,
    onSuggestion: (String) -> Unit,
    onCreate: () -> Unit,
) {
    val photo = state.photo?.let { rememberPhotoImage(it, 0) }
    val suggestions = listOf("음료는 그대로", "음식은 그대로", "분위기 유지", "배경만 바꿔줘")
    EditFrame(step = "4 / 4", onBack = onBack,
        footerLabel = if (state.isCreating) "모의 결과 만드는 중…" else "보호 이미지 만들기",
        footerEnabled = !state.isCreating && state.request.isNotBlank(), onFooter = onCreate) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FigmaIcon(R.drawable.figma_mascot)
            Column(Modifier.weight(1f).background(KkokColors.White, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KkokText("지키고 싶은 건 말로 알려주세요", style = KkokTypography.TitleMedium)
                KkokText("바꿀 곳과 지킬 곳을 꼭꼭이 나눠 줄게요", style = KkokTypography.BodySmall, color = KkokColors.Gray500)
            }
        }
        Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp)).background(KkokColors.White)) {
            PhotoImageLayer(photo)
            Box(Modifier.align(Alignment.TopStart).offset(x = 30.dp, y = 2.dp).size(150.dp, 104.dp)
                .border(2.dp, KkokColors.Primary, RoundedCornerShape(8.dp)).background(KkokColors.Primary.copy(alpha = .12f), RoundedCornerShape(8.dp)))
            Box(Modifier.align(Alignment.TopStart).offset(x = 150.dp, y = 112.dp).size(110.dp, 44.dp)
                .border(2.dp, KkokColors.Safe, RoundedCornerShape(8.dp)).background(KkokColors.Safe.copy(alpha = .16f), RoundedCornerShape(8.dp)))
            KkokText("바꿀 영역", Modifier.align(Alignment.TopStart).offset(x = 34.dp, y = 90.dp).background(KkokColors.Primary, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
                KkokTypography.LabelMedium, KkokColors.White)
            KkokText("지킬 영역", Modifier.align(Alignment.TopStart).offset(x = 152.dp, y = 100.dp).background(KkokColors.Safe, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
                KkokTypography.LabelMedium, KkokColors.White)
        }
        Column(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(20.dp))
            .border(1.5.dp, KkokColors.Primary, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicTextField(value = state.request, onValueChange = onRequest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(end = 48.dp),
                textStyle = KkokTypography.BodyMedium.copy(color = KkokColors.Ink),
                decorationBox = { inner -> Box { if (state.request.isEmpty()) KkokText("지키고 싶은 것을 적어주세요", color = KkokColors.Gray400); inner() } })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                KkokText("${state.request.length} / 200", style = KkokTypography.LabelSmall, color = KkokColors.Gray400)
                KkokText("➤", Modifier.background(KkokColors.Primary, CircleShape).padding(10.dp), KkokTypography.LabelLarge, KkokColors.White)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { suggestions.take(3).forEach { Suggestion(it) { onSuggestion(it) } } }
            Suggestion(suggestions.last()) { onSuggestion(suggestions.last()) }
        }
        Column(Modifier.fillMaxWidth().background(KkokColors.Secondary, RoundedCornerShape(22.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            KkokText("꼭꼭이 이렇게 이해했어요", style = KkokTypography.TitleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                KkokText("지킬 것", style = KkokTypography.BodySmall, color = KkokColors.Safe)
                SummaryChip("케이크"); SummaryChip("음료"); SummaryChip("카페 분위기")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                KkokText("바꿀 것", style = KkokTypography.BodySmall, color = KkokColors.Primary)
                SummaryChip("간판"); SummaryChip("창밖 건물"); SummaryChip("GPS 정보")
            }
        }
        if (state.error) KkokText("모의 결과를 만들지 못했어요. 다시 시도해 주세요.", color = KkokColors.High)
    }
}

@Composable
private fun StrengthSlider(value: Float, onValue: (Float) -> Unit) {
    Canvas(Modifier.fillMaxWidth().height(36.dp)
        .pointerInput(Unit) { detectTapGestures { onValue((it.x / size.width).coerceIn(0f, 1f)) } }
        .pointerInput(Unit) {
            detectDragGestures { change, _ -> onValue((change.position.x / size.width).coerceIn(0f, 1f)) }
        }) {
        val centerY = size.height / 2
        val stroke = 8.dp.toPx()
        drawLine(KkokColors.Gray100, Offset(0f, centerY), Offset(size.width, centerY), stroke, StrokeCap.Round)
        drawLine(KkokColors.Primary, Offset(0f, centerY), Offset(size.width * value, centerY), stroke, StrokeCap.Round)
        drawCircle(KkokColors.Primary, 12.dp.toPx(), Offset(size.width * value, centerY))
        drawCircle(KkokColors.White, 9.dp.toPx(), Offset(size.width * value, centerY))
    }
}

@Composable
private fun PreviewScreen(state: ProtectionEditState, onBack: () -> Unit, onNotSaved: () -> Unit) {
    val preview = state.preview ?: ProtectionPreview()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(18.dp)
    Column(Modifier.fillMaxSize().background(KkokColors.Background)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(40.dp))) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            Box(Modifier.padding(start = 8.dp).size(48.dp).align(Alignment.CenterStart)
                .clip(CircleShape).background(KkokColors.White).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) { FigmaIcon(R.drawable.figma_back) }
            KkokText("수정 결과", Modifier.align(Alignment.Center), KkokTypography.TitleLarge)
            KkokText("모의", Modifier.align(Alignment.CenterEnd).padding(end = 20.dp), KkokTypography.LabelLarge, KkokColors.Primary)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().height(290.dp).clip(RoundedCornerShape(24.dp)).background(KkokColors.White)) {
                SampleComparisonImage(R.drawable.protection_before, "수정 전", Modifier.weight(1f))
                Box(Modifier.width(3.dp).fillMaxHeight().background(KkokColors.White))
                SampleComparisonImage(R.drawable.protection_after, "수정 후", Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().heightIn(min = 172.dp).background(KkokColors.White, RoundedCornerShape(24.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KkokText("Figma 예시 · 선택한 사진은 수정되지 않았어요.", style = KkokTypography.LabelMedium, color = KkokColors.Gray500)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                    ScoreBlock("수정 전", preview.beforeScore, KkokColors.High)
                    KkokText("→", Modifier.background(KkokColors.Secondary, CircleShape).padding(10.dp), KkokTypography.TitleLarge, KkokColors.Primary)
                    ScoreBlock("수정 후", preview.afterScore, KkokColors.Safe)
                }
            }
            Column(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(24.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KkokText("다시 검사한 결과", style = KkokTypography.TitleMedium)
                preview.removedLabels.forEach { ResultLine("✓", it, KkokColors.Safe) }
                ResultLine("!", preview.remainingLabel, KkokColors.Point)
                if (state.error) KkokText("결과를 불러오지 못했어요.", color = KkokColors.High)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = bottom),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EditButton("다시 수정", Modifier.weight(1f), primary = false, onClick = onBack)
            EditButton("사진 저장하기", Modifier.weight(1f), primary = true, onClick = onNotSaved)
        }
    }
}

@Composable
private fun PhotoThumb(photo: PhotoImage?, modifier: Modifier = Modifier) {
    val bitmap = (photo as? PhotoImage.Loaded)?.bitmap
    if (bitmap != null) Image(bitmap, "선택한 사진", modifier.clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
    else Box(modifier.clip(RoundedCornerShape(12.dp)).background(KkokColors.Gray100), contentAlignment = Alignment.Center) {
        FigmaIcon(R.drawable.figma_scan)
    }
}

@Composable
private fun TargetHighlight(number: String, color: Color, modifier: Modifier) {
    Box(modifier.background(color.copy(alpha = .15f), RoundedCornerShape(10.dp))
        .border(3.dp, color, RoundedCornerShape(10.dp))) {
        Box(Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 4.dp).size(22.dp).background(color, CircleShape),
            contentAlignment = Alignment.Center) {
            KkokText(number, style = KkokTypography.LabelMedium,
                color = if (color == KkokColors.Point) KkokColors.Ink else KkokColors.White)
        }
    }
}

@Composable
private fun PhotoImageLayer(photo: PhotoImage?) {
    val bitmap = (photo as? PhotoImage.Loaded)?.bitmap
    if (bitmap != null) Image(bitmap, "선택한 사진", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    else Box(Modifier.fillMaxSize().background(KkokColors.Gray100), contentAlignment = Alignment.Center) { FigmaIcon(R.drawable.figma_scan) }
}

@Composable
private fun SegmentedChoice(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(CircleShape).background(if (selected) KkokColors.White else Color.Transparent)
        .clickable(role = Role.RadioButton, onClick = onClick).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        KkokText(text, style = KkokTypography.TitleMedium, color = if (selected) KkokColors.Ink else KkokColors.Gray500)
    }
}

@Composable
private fun Suggestion(text: String, onClick: () -> Unit) {
    KkokText(text, Modifier.clip(CircleShape).background(KkokColors.White)
        .border(1.dp, KkokColors.Gray200, CircleShape).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp), KkokTypography.LabelLarge, KkokColors.Gray600)
}

@Composable
private fun SummaryChip(text: String) {
    KkokText(text, Modifier.background(KkokColors.White, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
        KkokTypography.LabelMedium, KkokColors.Ink)
}

@Composable
private fun LocationCategoryChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    KkokText(label, modifier.clip(CircleShape)
        .background(if (selected) KkokColors.Primary else KkokColors.White)
        .border(1.dp, if (selected) KkokColors.Primary else KkokColors.Gray200, CircleShape)
        .clickable(role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 7.dp, vertical = 8.dp), KkokTypography.LabelMedium,
        if (selected) KkokColors.White else KkokColors.Gray600)
}

@Composable
private fun SampleComparisonImage(resource: Int, label: String, modifier: Modifier) {
    Box(modifier.fillMaxHeight()) {
        Image(painterResource(resource), label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        KkokText(label, Modifier.align(Alignment.TopStart).padding(10.dp).background(KkokColors.Ink.copy(alpha = .7f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp), KkokTypography.LabelMedium, KkokColors.White)
    }
}

@Composable
private fun ScoreBlock(label: String, score: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        KkokText(label, style = KkokTypography.LabelSmall, color = KkokColors.Gray400)
        KkokText("$score", style = KkokTypography.Score.copy(fontSize = 38.sp), color = color)
        KkokText(if (score > 50) "높음" else "낮음", Modifier.background(color, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp),
            KkokTypography.LabelMedium, KkokColors.White)
    }
}

@Composable
private fun ResultLine(icon: String, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KkokText(icon, Modifier.size(24.dp).background(color, CircleShape), KkokTypography.TitleMedium, KkokColors.White)
        KkokText(text, style = KkokTypography.BodyMedium)
    }
}

@Composable
private fun EditButton(label: String, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.heightIn(min = 60.dp).clip(CircleShape)
        .background(if (primary) KkokColors.Primary else KkokColors.White)
        .border(if (primary) 0.dp else 1.5.dp, if (primary) KkokColors.Primary else KkokColors.Gray200, CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center) {
        KkokText(label, style = KkokTypography.TitleLarge, color = if (primary) KkokColors.White else KkokColors.Ink,
            modifier = Modifier.semantics { contentDescription = label })
    }
}
