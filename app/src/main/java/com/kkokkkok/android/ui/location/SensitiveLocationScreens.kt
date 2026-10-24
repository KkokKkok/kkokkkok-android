package com.kkokkkok.android.ui.location

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
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kkokkkok.android.R
import com.kkokkkok.android.location.LocationType
import com.kkokkkok.android.location.SensitiveLocation
import com.kkokkkok.android.location.SensitiveLocationState
import com.kkokkkok.android.location.SensitiveLocationViewModel
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.IconBadge
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokSpacing
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

@Composable
fun SensitiveLocationFlow(
    viewModel: SensitiveLocationViewModel,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedLocationId = remember { mutableStateOf<String?>(null) }
    val editing = state.editing
    BackHandler {
        when {
            editing != null -> viewModel.cancelEdit()
            selectedLocationId.value != null -> selectedLocationId.value = null
            else -> onBack()
        }
    }
    if (editing != null) {
        SensitiveLocationEditor(
            location = editing,
            onBack = viewModel::cancelEdit,
            onName = viewModel::updateName,
            onType = viewModel::updateType,
            onRadius = viewModel::updateRadius,
            onSave = {
                if (editing.name.isBlank()) onMessage("장소 이름을 입력해 주세요.") else viewModel.save()
            },
        )
    } else {
        val selected = state.locations.firstOrNull { it.id == selectedLocationId.value }
        if (selected != null) {
            SensitiveLocationDetail(
                location = selected,
                onBack = { selectedLocationId.value = null },
                onEdit = { viewModel.beginEdit(selected) },
                onDelete = {
                    viewModel.delete(selected.id)
                    selectedLocationId.value = null
                    onMessage("장소를 삭제했어요.")
                },
                onSns = { onMessage("SNS 검사는 아직 준비 중이에요. 화면의 기록은 모두 모의 자료예요.") },
            )
        } else {
            SensitiveLocationList(
                state = state,
                onBack = onBack,
                onOpen = { selectedLocationId.value = it.id },
                onAdd = viewModel::beginAdd,
            )
        }
    }
}
@Composable
private fun SensitiveLocationList(
    state: SensitiveLocationState,
    onBack: () -> Unit,
    onOpen: (SensitiveLocation) -> Unit,
    onAdd: () -> Unit,
) {
    LocationFrame {
        Column(Modifier.fillMaxSize().background(KkokColors.Background)) {
            LocationHeader("내 민감 위치", onBack, right = "모의 데이터")
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = KkokSpacing.Page).padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FigmaIcon(R.drawable.figma_mascot, Modifier.size(44.dp))
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(KkokColors.White).padding(12.dp)) {
                        KkokText("지키고 싶은 장소를 모아 뒀어요", style = KkokTypography.TitleMedium)
                        KkokText("등록한 장소가 드러나는 사진은 더 꼼꼼히 검사해요", style = KkokTypography.BodySmall, color = KkokColors.Gray600)
                    }
                }
                if (state.isLoading) {
                    KkokText("장소를 불러오고 있어요…", Modifier.padding(vertical = 24.dp), color = KkokColors.Gray600)
                } else if (state.locations.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.White).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        KkokText("아직 등록한 장소가 없어요", style = KkokTypography.TitleMedium)
                        KkokText("지키고 싶은 장소를 추가해 보세요", style = KkokTypography.BodySmall, color = KkokColors.Gray600)
                    }
                }
                state.locations.forEach { location -> PlaceCard(location, onClick = { onOpen(location) }) }
                Row(
                    Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, KkokColors.Gray200, RoundedCornerShape(24.dp))
                        .clickable(role = Role.Button, onClick = onAdd),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    KkokText("＋ 장소 추가하기", style = KkokTypography.TitleMedium, color = KkokColors.Gray500)
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(KkokColors.Point).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LockIcon()
                    Spacer(Modifier.width(10.dp))
                    KkokText("등록한 위치는 나만 볼 수 있고, 위험 판단에만 쓰여요.\n언제든 지울 수 있어요.", Modifier.weight(1f), KkokTypography.BodySmall)
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(location: SensitiveLocation, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.White)
            .clickable(role = Role.Button, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).background(KkokColors.Secondary, CircleShape), contentAlignment = Alignment.Center) {
            PlaceIcon(location.type)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KkokText(location.name, style = KkokTypography.TitleMedium)
                location.risk?.let { RiskBadge(it) }
            }
            KkokText("${location.address} · 반경 ${location.radiusMeters}m", style = KkokTypography.BodySmall, color = KkokColors.Gray600)
            KkokText(location.scanSummary, style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
        }
        KkokText("›", style = KkokTypography.TitleLarge, color = KkokColors.Gray400)
    }
}

@Composable
private fun RiskBadge(risk: String) {
    val high = risk == "높음"
    Box(Modifier.clip(CircleShape).background(if (high) KkokColors.High else KkokColors.Point).padding(horizontal = 10.dp, vertical = 4.dp)) {
        KkokText(risk, style = KkokTypography.LabelLarge, color = if (high) KkokColors.White else KkokColors.Ink)
    }
}

@Composable
private fun SensitiveLocationEditor(
    location: SensitiveLocation,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onType: (LocationType) -> Unit,
    onRadius: (Int) -> Unit,
    onSave: () -> Unit,
) {
    LocationFrame {
        Column(Modifier.fillMaxSize().background(KkokColors.Background)) {
            LocationHeader(if (location.id.startsWith("new-")) "장소 추가" else "장소 편집", onBack, right = "모의 데이터")
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = KkokSpacing.Page).padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("장소 이름", required = true)
                    BasicTextField(
                        value = location.name,
                        onValueChange = onName,
                        singleLine = true,
                        textStyle = KkokTypography.BodyMedium.copy(color = KkokColors.Ink),
                        decorationBox = { inner ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(KkokColors.White)
                                    .border(1.5.dp, KkokColors.Gray200, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                            ) { Box { if (location.name.isEmpty()) KkokText("장소 이름을 입력해 주세요", style = KkokTypography.BodyMedium, color = KkokColors.Gray400); inner() } }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("location-name"),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("종류")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LocationType.entries.forEach { type -> SelectChip(type.label, location.type == type) { onType(type) } }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("위치", required = true)
                    Row(
                        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(KkokColors.White)
                            .border(2.dp, KkokColors.Primary, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KkokText("⌕", style = KkokTypography.TitleLarge, color = KkokColors.Gray600)
                        Spacer(Modifier.width(10.dp))
                        KkokText(location.address, style = KkokTypography.BodyMedium)
                        Spacer(Modifier.weight(1f))
                        KkokText("예시 위치", style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
                    }
                    LocationMap(Modifier.fillMaxWidth().height(180.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("보호 반경")
                    Row(Modifier.fillMaxWidth().clip(CircleShape).background(KkokColors.Gray100).padding(4.dp)) {
                        listOf(100, 300, 500, 1000).forEach { radius ->
                            val label = if (radius == 1000) "1km" else "${radius}m"
                            Box(
                                Modifier.weight(1f).clip(CircleShape).background(if (location.radiusMeters == radius) KkokColors.White else Color.Transparent)
                                    .clickable(role = Role.RadioButton) { onRadius(radius) }.padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) { KkokText(label, style = KkokTypography.TitleMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = .1.sp), color = if (location.radiusMeters == radius) KkokColors.Primary else KkokColors.Gray500) }
                        }
                    }
                    KkokText("이 범위 안에서 찍힌 사진은 더 꼼꼼히 검사해요", style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(KkokColors.Point).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    LockIcon()
                    Spacer(Modifier.width(10.dp))
                    KkokText("주소는 나만 볼 수 있고, 위험 판단에만 쓰여요.", style = KkokTypography.BodySmall)
                }
            }
            PrimaryLocationButton("저장하기", onSave)
        }
    }
}

@Composable
private fun SensitiveLocationDetail(
    location: SensitiveLocation,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSns: () -> Unit,
) {
    LocationFrame {
        Column(Modifier.fillMaxSize().background(KkokColors.Background)) {
            LocationHeader(location.name, onBack, right = "편집", onRight = onEdit)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = KkokSpacing.Page).padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LocationMap(Modifier.fillMaxWidth().height(180.dp), showMockLabel = true)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(KkokColors.White).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconBadge(R.drawable.figma_home, 36.dp, KkokColors.Secondary)
                    Column {
                        KkokText("${location.address} 근처", style = KkokTypography.TitleMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = .1.sp))
                        KkokText("${location.type.label} · 반경 ${location.radiusMeters}m", style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
                    }
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(KkokColors.White).padding(14.dp)) {
                    StatCell(location.linkedPosts.toString(), "연결된 게시물", KkokColors.Primary, Modifier.weight(1f))
                    StatCell(location.riskScore.toString(), "누적 위험 점수", KkokColors.High, Modifier.weight(1f))
                    StatCell(location.photoScans.toString(), "이 장소 사진 검사", KkokColors.Primary, Modifier.weight(1f))
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        KkokText("단서가 보인 게시물", style = KkokTypography.TitleMedium)
                        KkokText("전체", style = KkokTypography.LabelLarge, color = KkokColors.Gray500)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) { index ->
                            Column(
                                Modifier.weight(1f).height(62.dp).clip(RoundedCornerShape(16.dp)).background(if (index % 2 == 0) KkokColors.Secondary else KkokColors.Gray200),
                                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                KkokText("▧", style = KkokTypography.TitleMedium, color = KkokColors.Primary)
                                KkokText("모의", style = KkokTypography.LabelSmall, color = KkokColors.Gray600)
                            }
                        }
                    }
                }
                PrimaryLocationButton(
                    "이 장소로 SNS 누적 검사", onSns,
                    horizontalInset = false, topPadding = 0.dp, bottomPadding = 0.dp,
                )
                Box(
                    Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) { KkokText("이 장소 삭제", style = KkokTypography.LabelLarge, color = KkokColors.High) }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        KkokText(value, style = KkokTypography.HeadlineSmall, color = color)
        KkokText(label, style = KkokTypography.LabelSmall.copy(textAlign = TextAlign.Center), color = KkokColors.Gray500)
    }
}

@Composable
private fun LocationMap(modifier: Modifier = Modifier, showMockLabel: Boolean = false) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(KkokColors.Gray100)) {
      Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val block = KkokColors.Gray200
        val street = KkokColors.White
        // A schematic preview only; no tiles, coordinates, or live map provider.
        listOf(.04f to .04f, .245f to .04f, .51f to .04f, .75f to .04f, .04f to .54f, .24f to .54f, .52f to .54f, .74f to .54f).forEach { (x, y) ->
            drawRoundRect(block, Offset(w * x, h * y), androidx.compose.ui.geometry.Size(w * .17f, h * .25f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
        }
        drawRect(street, Offset(0f, h * .39f), androidx.compose.ui.geometry.Size(w, h * .09f))
        drawRect(street, Offset(w * .43f, 0f), androidx.compose.ui.geometry.Size(w * .04f, h))
        val diagonal = Path().apply { moveTo(0f, h * .84f); lineTo(w, h * .63f); lineTo(w, h * .72f); lineTo(0f, h * .95f); close() }
        drawPath(diagonal, street)
        drawRoundRect(KkokColors.Safe.copy(alpha = .28f), Offset(w * .77f, h * .66f), androidx.compose.ui.geometry.Size(w * .24f, h * .4f), androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()))
        val center = Offset(w * .45f, h * .44f)
        drawCircle(KkokColors.Primary.copy(alpha = .12f), w * .165f, center)
        drawCircle(KkokColors.Primary, w * .165f, center, style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))))
        val pinCenter = Offset(center.x, center.y - 14.dp.toPx())
        val pin = Path().apply {
            moveTo(pinCenter.x, pinCenter.y + 19.dp.toPx())
            cubicTo(pinCenter.x - 5.dp.toPx(), pinCenter.y + 11.dp.toPx(), pinCenter.x - 16.dp.toPx(), pinCenter.y + 1.dp.toPx(), pinCenter.x - 16.dp.toPx(), pinCenter.y - 4.dp.toPx())
            cubicTo(pinCenter.x - 16.dp.toPx(), pinCenter.y - 25.dp.toPx(), pinCenter.x + 16.dp.toPx(), pinCenter.y - 25.dp.toPx(), pinCenter.x + 16.dp.toPx(), pinCenter.y - 4.dp.toPx())
            cubicTo(pinCenter.x + 16.dp.toPx(), pinCenter.y + 1.dp.toPx(), pinCenter.x + 5.dp.toPx(), pinCenter.y + 11.dp.toPx(), pinCenter.x, pinCenter.y + 19.dp.toPx())
            close()
        }
        drawPath(pin, KkokColors.Primary)
        drawCircle(KkokColors.White, 5.dp.toPx(), Offset(pinCenter.x, pinCenter.y - 4.dp.toPx()))
      }
      if (showMockLabel) {
          Box(
              Modifier.align(Alignment.TopEnd).padding(10.dp).clip(CircleShape).background(KkokColors.White).padding(horizontal = 8.dp, vertical = 4.dp),
          ) { KkokText("모의 데이터", style = KkokTypography.LabelSmall, color = KkokColors.Primary) }
      }
    }
}

@Composable
private fun LocationHeader(title: String, onBack: () -> Unit, right: String = "", onRight: (() -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        Box(Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(40.dp).clip(CircleShape).background(KkokColors.White)
            .clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "뒤로 가기" }, contentAlignment = Alignment.Center) {
            FigmaIcon(R.drawable.figma_back)
        }
        KkokText(title, Modifier.align(Alignment.Center), KkokTypography.TitleLarge)
        if (right.isNotEmpty()) {
            KkokText(
                right,
                Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)
                    .then(if (onRight == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onRight)),
                KkokTypography.LabelSmall,
                KkokColors.Primary,
            )
        }
    }
}

@Composable
private fun LocationFrame(content: @Composable () -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(KkokColors.Background).padding(top = top)) { content() }
}

@Composable
private fun FieldLabel(label: String, required: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        KkokText(label, style = KkokTypography.LabelLarge, color = KkokColors.Gray600)
        if (required) KkokText("필수", style = KkokTypography.LabelSmall, color = KkokColors.High)
    }
}

@Composable
private fun PlaceIcon(type: LocationType) {
    Canvas(Modifier.size(22.dp)) {
        val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cx = size.width / 2
        val cy = size.height / 2
        when (type) {
            LocationType.HOME -> {
                val path = Path().apply { moveTo(cx, 2.dp.toPx()); lineTo(size.width - 2.dp.toPx(), cy - 1.dp.toPx()); lineTo(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()); lineTo(2.dp.toPx(), size.height - 2.dp.toPx()); lineTo(2.dp.toPx(), cy - 1.dp.toPx()); close() }
                drawPath(path, KkokColors.Primary, style = stroke)
                drawRect(KkokColors.Primary, Offset(cx - 2.dp.toPx(), cy + 2.dp.toPx()), androidx.compose.ui.geometry.Size(4.dp.toPx(), 6.dp.toPx()))
            }
            LocationType.SCHOOL -> {
                val cap = Path().apply { moveTo(2.dp.toPx(), cy - 2.dp.toPx()); lineTo(cx, 3.dp.toPx()); lineTo(size.width - 2.dp.toPx(), cy - 2.dp.toPx()); lineTo(cx, cy + 3.dp.toPx()); close() }
                drawPath(cap, KkokColors.Primary, style = stroke)
                drawLine(KkokColors.Primary, Offset(5.dp.toPx(), cy), Offset(5.dp.toPx(), cy + 5.dp.toPx()), 1.8.dp.toPx())
                drawArc(KkokColors.Primary, 5f, 170f, false, Offset(5.dp.toPx(), cy + 1.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 10.dp.toPx(), 7.dp.toPx()), style = stroke)
            }
            LocationType.WORK -> drawRoundRect(KkokColors.Primary, Offset(2.dp.toPx(), 3.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 4.dp.toPx(), size.height - 6.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = stroke)
            LocationType.FREQUENT -> {
                val heart = Path().apply {
                    moveTo(cx, size.height - 2.dp.toPx())
                    cubicTo(cx - 3.dp.toPx(), cy + 4.dp.toPx(), 2.dp.toPx(), cy + 1.dp.toPx(), 2.dp.toPx(), cy - 4.dp.toPx())
                    cubicTo(2.dp.toPx(), 2.dp.toPx(), cx - 4.dp.toPx(), 2.dp.toPx(), cx, cy + 5.dp.toPx())
                    cubicTo(cx + 4.dp.toPx(), 2.dp.toPx(), size.width - 2.dp.toPx(), 2.dp.toPx(), size.width - 2.dp.toPx(), cy - 4.dp.toPx())
                    cubicTo(size.width - 2.dp.toPx(), cy + 1.dp.toPx(), cx + 3.dp.toPx(), cy + 4.dp.toPx(), cx, size.height - 2.dp.toPx())
                }
                drawPath(heart, KkokColors.Primary, style = stroke)
            }
        }
    }
}

@Composable
private fun LockIcon() {
    Canvas(Modifier.size(18.dp)) {
        val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(KkokColors.Ink, Offset(3.dp.toPx(), 8.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 6.dp.toPx(), size.height - 10.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = stroke)
        drawArc(KkokColors.Ink, 180f, 180f, false, Offset(5.dp.toPx(), 1.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 10.dp.toPx(), 14.dp.toPx()), style = stroke)
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(CircleShape).background(if (selected) KkokColors.Primary else KkokColors.White)
            .then(if (selected) Modifier else Modifier.border(1.dp, KkokColors.Gray200, CircleShape))
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { KkokText(label, style = KkokTypography.BodySmall, color = if (selected) KkokColors.White else KkokColors.Gray600) }
}

@Composable
private fun PrimaryLocationButton(
    label: String,
    onClick: () -> Unit,
    horizontalInset: Boolean = true,
    topPadding: androidx.compose.ui.unit.Dp = 12.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp),
) {
    Box(
        Modifier.fillMaxWidth()
            .padding(horizontal = if (horizontalInset) KkokSpacing.Page else 0.dp)
            .padding(top = topPadding, bottom = bottomPadding),
    ) {
        Box(
            Modifier.fillMaxWidth().height(60.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(KkokColors.Primary)
                .clickable(role = Role.Button, onClick = onClick).testTag("location-primary-action"),
            contentAlignment = Alignment.Center,
        ) { KkokText(label, style = KkokTypography.TitleLarge, color = KkokColors.White) }
    }
}


