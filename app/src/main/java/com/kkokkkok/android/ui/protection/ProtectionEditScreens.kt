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
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.kkokkkok.android.analysis.AnalysisClue
import com.kkokkkok.android.analysis.ClueRisk
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
    photoLabel: String? = null,
    nextPhotoLabel: String? = null,
    onNextPhoto: (() -> Unit)? = null,
) {
    when (state.step) {
        ProtectionStep.LOCATION -> LocationScreen(state, onBack, onChangePhoto, onLocation, onLocationCategory, onContinue)
        ProtectionStep.TARGETS -> TargetScreen(state, onBack, onToggleMode, onToggleTarget, onStrength, onContinue, photoLabel)
        ProtectionStep.REQUEST -> RequestScreen(state, onBack, onRequest, onSuggestion, onCreate, photoLabel)
        ProtectionStep.RESULT -> ProtectionResultScreen(state, onBack, onReedit = onBack, photoLabel = photoLabel,
            nextPhotoLabel = nextPhotoLabel, onNextPhoto = onNextPhoto)
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
    photoLabel: String? = null,
) {
    val photo = state.photo?.let { rememberPhotoImage(it, 0) }
    EditFrame(step = "3 / 4", onBack = onBack, footerLabel = "다음 · 요청 입력",
        footerEnabled = state.selectedTargets.isNotEmpty(), onFooter = onContinue) {
        photoLabel?.let { KkokText(it, style = KkokTypography.LabelLarge, color = KkokColors.Primary) }
        CluePhotoPreview(photo, state.availableClues, state.selectedTargets)
        Row(Modifier.fillMaxWidth().background(KkokColors.Gray100, CircleShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SegmentedChoice("자동 수정", state.mode == EditMode.AUTOMATIC, Modifier.weight(1f), onToggleMode)
            SegmentedChoice("직접 고르기", state.mode == EditMode.MANUAL, Modifier.weight(1f), onToggleMode)
        }
        Column(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(24.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.availableClues.forEachIndexed { index, clue ->
                val selected = clue.id in state.selectedTargets
                val color = if (clue.risk == ClueRisk.HIGH) KkokColors.High else KkokColors.Point
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(
                    role = Role.Checkbox, onClick = { onToggleTarget(clue.id) }).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KkokText(if (selected) "✓" else "○", Modifier.size(26.dp).background(if (selected) KkokColors.Primary else KkokColors.Gray100, CircleShape),
                        KkokTypography.TitleMedium, if (selected) KkokColors.White else KkokColors.Gray500)
                    KkokText("${index + 1}", Modifier.background(color, CircleShape).padding(horizontal = 7.dp, vertical = 3.dp),
                        KkokTypography.LabelMedium, if (color == KkokColors.Point) KkokColors.Ink else KkokColors.White)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        KkokText(clue.title, style = KkokTypography.TitleMedium)
                        KkokText(clue.description.ifBlank { "검사 결과의 표시 영역을 수정해요." },
                            style = KkokTypography.LabelMedium, color = KkokColors.Gray500)
                    }
                }
            }
            if (state.availableClues.isEmpty()) KkokText(
                "이 검사에는 아직 수정할 위험 단서 위치가 없어요. 주의·높음 단서의 수정 영역을 분석 결과에서 받을 수 있으면 편집할 수 있어요.",
                style = KkokTypography.BodySmall, color = KkokColors.Gray600,
            )
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
    photoLabel: String? = null,
) {
    val photo = state.photo?.let { rememberPhotoImage(it, 0) }
    val suggestions = listOf("음료는 그대로", "음식은 그대로", "분위기 유지", "배경만 바꿔줘")
    EditFrame(step = "4 / 4", onBack = onBack,
        footerLabel = if (state.isCreating) "사진 편집 중…" else "보호 이미지 만들기",
        footerEnabled = !state.isCreating && state.selectedTargets.isNotEmpty(), onFooter = onCreate) {
        photoLabel?.let { KkokText(it, style = KkokTypography.LabelLarge, color = KkokColors.Primary) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FigmaIcon(R.drawable.figma_mascot)
            Column(Modifier.weight(1f).background(KkokColors.White, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                KkokText("지키고 싶은 건 말로 알려주세요", style = KkokTypography.TitleMedium)
                KkokText("바꿀 곳과 지킬 곳을 꼭꼭이 나눠 줄게요", style = KkokTypography.BodySmall, color = KkokColors.Gray500)
            }
        }
        CluePhotoPreview(photo, state.availableClues.filter { it.id in state.selectedTargets }, state.selectedTargets, height = 160.dp)
        Column(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(20.dp))
            .border(1.5.dp, KkokColors.Primary, RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicTextField(value = state.request, onValueChange = onRequest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(end = 48.dp),
                textStyle = KkokTypography.BodyMedium.copy(color = KkokColors.Ink),
                decorationBox = { inner -> Box { if (state.request.isEmpty()) KkokText("지키고 싶은 것이 있으면 적어주세요 (선택)", color = KkokColors.Gray400); inner() } })
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
            KkokText("이번 편집 요청", style = KkokTypography.TitleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                KkokText("유지", style = KkokTypography.BodySmall, color = KkokColors.Safe)
                SummaryChip(state.request.ifBlank { "특별히 유지할 내용 없음" })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                KkokText("수정", style = KkokTypography.BodySmall, color = KkokColors.Primary)
                state.availableClues.filter { it.id in state.selectedTargets }.forEach { SummaryChip(it.title) }
            }
        }
        state.errorMessage?.let { KkokText(it, color = KkokColors.High) }
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
internal fun ProtectionResultScreen(state: ProtectionEditState, onBack: () -> Unit, onReedit: (() -> Unit)?,
    photoLabel: String? = null, nextPhotoLabel: String? = null, onNextPhoto: (() -> Unit)? = null) {
    val preview = state.preview
    val context = LocalContext.current
    val exporter = remember(context) { EditedPhotoExporter(context) }
    val scope = rememberCoroutineScope()
    var saving by remember(preview?.editedPhoto) { mutableStateOf(false) }
    var saved by remember(preview?.editedPhoto) { mutableStateOf(false) }
    var saveMessage by remember(preview?.editedPhoto) { mutableStateOf<String?>(null) }
    fun save(action: suspend () -> Unit) {
        saving = true
        saveMessage = null
        scope.launch {
            try {
                action()
                saved = true
                saveMessage = "사진을 저장했어요."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { saveMessage = "사진을 저장하지 못했어요. 저장 공간을 확인하고 다시 시도해 주세요." }
            finally { saving = false }
        }
    }
    val destinationPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { destination ->
        if (destination == null) { saving = false; saveMessage = "저장을 취소했어요." }
        else if (preview != null) save { exporter.saveToDocument(preview, destination) }
        else saving = false
    }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(18.dp)
    Column(Modifier.fillMaxSize().background(KkokColors.Background)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(40.dp))) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            Box(Modifier.padding(start = 8.dp).size(48.dp).align(Alignment.CenterStart)
                .clip(CircleShape).background(KkokColors.White).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) { FigmaIcon(R.drawable.figma_back) }
            KkokText("수정 결과", Modifier.align(Alignment.Center), KkokTypography.TitleLarge)
            KkokText(if (preview?.isMock == true) "모의" else "실제", Modifier.align(Alignment.CenterEnd).padding(end = 20.dp), KkokTypography.LabelLarge, KkokColors.Primary)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            photoLabel?.let { KkokText(it, style = KkokTypography.LabelLarge, color = KkokColors.Primary) }
            Row(Modifier.fillMaxWidth().height(290.dp).clip(RoundedCornerShape(24.dp)).background(KkokColors.White)) {
                ComparisonPhoto(state.photo, "수정 전", Modifier.weight(1f))
                Box(Modifier.width(3.dp).fillMaxHeight().background(KkokColors.White))
                ComparisonPhoto(preview?.editedPhoto, "수정 후", Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(24.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KkokText("다시 검사한 결과", style = KkokTypography.TitleMedium)
                val verificationLabel = when (preview?.verificationStatus) {
                    "completed" -> "재검사가 완료됐어요."
                    "failed" -> "수정은 완료됐지만 재검사에 실패했어요."
                    "mock" -> "모의 재검사 응답이라 보호 효과를 평가하지 않았어요."
                    "queued", "processing", "submitting" -> "수정한 사진을 다시 검사하고 있어요."
                    else -> "재검사 상태를 확인할 수 없어요."
                }
                KkokText(verificationLabel, style = KkokTypography.BodySmall, color = KkokColors.Gray600)
                preview?.verificationScore?.let { KkokText("재검사 점수 · $it", style = KkokTypography.TitleMedium) }
                preview?.verificationCueCount?.let { KkokText("다시 확인된 단서 · ${it}개", style = KkokTypography.BodySmall) }
                if (preview?.isMock == true) KkokText(
                    preview.mockNotice ?: "모의 편집 결과예요. 사진 내용은 실제로 바뀌지 않았어요.",
                    style = KkokTypography.BodySmall, color = KkokColors.High,
                )
                preview?.providerVersion?.let { KkokText("편집 처리 · $it", style = KkokTypography.LabelSmall, color = KkokColors.Gray500) }
                state.errorMessage?.let { KkokText(it, color = KkokColors.High) }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onNextPhoto != null && nextPhotoLabel != null) EditButton(nextPhotoLabel, Modifier.fillMaxWidth(),
                primary = false, compact = true, enabled = !saving, onClick = onNextPhoto)
            if (onReedit == null) KkokText("원본의 편집 가능한 검사 단서가 없어 다시 수정할 수 없어요.", style = KkokTypography.BodySmall)
            saveMessage?.let { KkokText(it, style = KkokTypography.BodySmall) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EditButton("다시 수정", Modifier.weight(1f), primary = false, compact = true,
                    enabled = !saving && onReedit != null, onClick = { onReedit?.invoke() })
                EditButton(if (saving) "저장 중…" else if (saved) "저장 완료" else "갤러리에 저장", Modifier.weight(1f),
                    compact = true, enabled = preview != null && !preview.isMock && !saving && !saved) {
                    if (preview != null && !preview.isMock && !saving && !saved) {
                        if (Build.VERSION.SDK_INT >= 29) save { exporter.saveToGallery(preview) }
                        else { saving = true; destinationPicker.launch(EditedPhotoExporter.fileName()) }
                    }
                }
            }
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
internal fun CluePhotoPreview(
    photo: PhotoImage?,
    clues: List<AnalysisClue>,
    selectedIds: Set<String>,
    height: androidx.compose.ui.unit.Dp = 210.dp,
) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(24.dp)).background(KkokColors.White),
    ) {
        val bitmap = (photo as? PhotoImage.Loaded)?.bitmap
        if (bitmap != null) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val imageWidth: androidx.compose.ui.unit.Dp
            val imageHeight: androidx.compose.ui.unit.Dp
            if (ratio >= maxWidth / height) {
                imageWidth = maxWidth
                imageHeight = maxWidth / ratio
            } else {
                imageHeight = height
                imageWidth = height * ratio
            }
            val left = (maxWidth - imageWidth) / 2
            val top = (height - imageHeight) / 2
            Image(bitmap, "검사 단서가 표시된 사진", Modifier.align(Alignment.Center).size(imageWidth, imageHeight), contentScale = ContentScale.Fit)
            clues.forEachIndexed { index, clue ->
                val region = clue.region ?: return@forEachIndexed
                val color = when {
                    clue.id !in selectedIds -> KkokColors.Gray500
                    clue.risk == ClueRisk.HIGH -> KkokColors.High
                    else -> KkokColors.Point
                }
                Box(Modifier.offset(
                    x = left + imageWidth * region.left,
                    y = top + imageHeight * region.top,
                ).size(
                    width = (imageWidth * (region.right - region.left)).coerceAtLeast(2.dp),
                    height = (imageHeight * (region.bottom - region.top)).coerceAtLeast(2.dp),
                ).testTag("protection-clue-${clue.id}").background(color.copy(alpha = .16f), RoundedCornerShape(6.dp)).border(2.dp, color, RoundedCornerShape(6.dp))) {
                    KkokText("${index + 1}", Modifier.align(Alignment.TopStart).background(color, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp), KkokTypography.LabelSmall,
                        if (color == KkokColors.Point) KkokColors.Ink else KkokColors.White)
                }
            }
        } else if (photo == PhotoImage.Failed) {
            KkokText("사진을 불러올 수 없어요", Modifier.align(Alignment.Center), color = KkokColors.Gray500)
        } else {
            Box(Modifier.fillMaxSize().background(KkokColors.Gray100))
        }
    }
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
private fun ComparisonPhoto(uri: android.net.Uri?, label: String, modifier: Modifier) {
    val photo = uri?.let { rememberPhotoImage(it, 0) }
    Box(modifier.fillMaxHeight()) {
        when (photo) {
            is PhotoImage.Loaded -> Image(photo.bitmap, label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else -> Box(Modifier.fillMaxSize().background(KkokColors.Gray100))
        }
        KkokText(label, Modifier.align(Alignment.TopStart).padding(10.dp).background(KkokColors.Ink.copy(alpha = .7f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp), KkokTypography.LabelMedium, KkokColors.White)
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
private fun EditButton(label: String, modifier: Modifier = Modifier, primary: Boolean = true, compact: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.heightIn(min = 60.dp).clip(CircleShape)
        .background(if (primary) KkokColors.Primary else KkokColors.White)
        .border(if (primary) 0.dp else 1.5.dp, if (primary) KkokColors.Primary else KkokColors.Gray200, CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center) {
        KkokText(label, style = if (compact) KkokTypography.TitleMedium else KkokTypography.TitleLarge, color = if (primary) KkokColors.White else KkokColors.Ink,
            modifier = Modifier.semantics { contentDescription = label })
    }
}
