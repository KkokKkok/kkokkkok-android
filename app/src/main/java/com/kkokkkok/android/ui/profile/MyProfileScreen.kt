package com.kkokkkok.android.ui.profile

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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kkokkkok.android.R
import com.kkokkkok.android.profile.ProfileOverview
import com.kkokkkok.android.profile.ProfilePlace
import com.kkokkkok.android.profile.ProfilePlaceKind
import com.kkokkkok.android.profile.ProfileViewModel
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

@Composable
fun MyProfileScreen(
    viewModel: ProfileViewModel,
    onHome: () -> Unit,
    onInspectionHistory: () -> Unit,
    onProtectionHistory: () -> Unit,
    onManageSensitiveLocations: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val top = (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 8.dp).coerceAtLeast(0.dp)
    Column(Modifier.fillMaxSize().background(KkokColors.Background).padding(top = top)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                state.isLoading -> KkokText("마이 화면을 준비하고 있어요…", Modifier.padding(vertical = 32.dp), color = KkokColors.Gray600)
                state.error || state.overview == null -> ProfileLoadError(viewModel::retry)
                else -> ProfileContent(
                    overview = requireNotNull(state.overview),
                    onToggleAutoDelete = viewModel::toggleAutoDeleteOriginals,
                    onInspectionHistory = onInspectionHistory,
                    onManageSensitiveLocations = onManageSensitiveLocations,
                )
            }
        }
        ProfileTabBar(onHome, onInspectionHistory, onProtectionHistory)
    }
}

@Composable
private fun ProfileContent(
    overview: ProfileOverview,
    onToggleAutoDelete: () -> Unit,
    onInspectionHistory: () -> Unit,
    onManageSensitiveLocations: () -> Unit,
) {
    ProfileHeader(overview.displayName)
    StatsCard(overview)
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        KkokText("등록 민감 위치", Modifier.weight(1f), KkokTypography.TitleLarge)
        KkokText(
            "관리 〉",
            Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onManageSensitiveLocations)
                .semantics { role = Role.Button },
            KkokTypography.LabelLarge,
            KkokColors.Gray500,
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        overview.sensitivePlaces.forEach { place ->
            SensitivePlaceChip(place, onManageSensitiveLocations)
        }
        AddPlaceChip(onManageSensitiveLocations)
    }
    ProfileMenuCard(onInspectionHistory)
    ProfileDataCard(overview, onToggleAutoDelete)
}

@Composable
private fun ProfileHeader(name: String) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(56.dp).background(KkokColors.Secondary, CircleShape), contentAlignment = Alignment.Center) {
            FigmaIcon(R.drawable.figma_mascot, Modifier.size(40.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            KkokText("${name}님", style = KkokTypography.HeadlineSmall)
            KkokText("오늘도 꼭꼭 지키는 중 · 모의 계정", style = KkokTypography.LabelMedium, color = KkokColors.Gray500)
        }
        Box(
            Modifier.size(40.dp).background(KkokColors.White, CircleShape)
                .semantics { contentDescription = "설정 화면은 아직 준비되지 않았어요"; disabled() },
            contentAlignment = Alignment.Center,
        ) { SettingsIcon() }
    }
}

@Composable
private fun StatsCard(overview: ProfileOverview) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.White).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ProfileStat(overview.photoChecks.toString(), "사진 검사", Modifier.weight(1f))
        ProfileStat(overview.protectionEdits.toString(), "보호 편집", Modifier.weight(1f))
        ProfileStat(overview.snsChecks.toString(), "SNS 분석", Modifier.weight(1f))
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        KkokText(value, style = KkokTypography.HeadlineSmall, color = KkokColors.Primary)
        KkokText(label, style = KkokTypography.LabelMedium, color = KkokColors.Gray500)
    }
}

@Composable
private fun SensitivePlaceChip(place: ProfilePlace, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(KkokColors.White)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = "${place.name}, 민감 위치 관리" }
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(26.dp).background(KkokColors.Secondary, CircleShape), contentAlignment = Alignment.Center) {
            if (place.kind == ProfilePlaceKind.HOME) FigmaIcon(R.drawable.figma_home, Modifier.size(14.dp)) else SchoolIcon()
        }
        KkokText(place.name, style = KkokTypography.BodyMedium)
    }
}

@Composable
private fun SchoolIcon() {
    Canvas(Modifier.size(14.dp)) {
        val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cap = Path().apply {
            moveTo(size.width * .06f, size.height * .34f)
            lineTo(size.width * .5f, size.height * .12f)
            lineTo(size.width * .94f, size.height * .34f)
            lineTo(size.width * .5f, size.height * .56f)
            close()
        }
        drawPath(cap, KkokColors.Primary, style = stroke)
        drawLine(KkokColors.Primary, Offset(size.width * .26f, size.height * .47f), Offset(size.width * .26f, size.height * .7f), stroke.width)
        drawLine(KkokColors.Primary, Offset(size.width * .74f, size.height * .47f), Offset(size.width * .74f, size.height * .7f), stroke.width)
        drawLine(KkokColors.Primary, Offset(size.width * .26f, size.height * .7f), Offset(size.width * .74f, size.height * .7f), stroke.width)
        drawLine(KkokColors.Primary, Offset(size.width * .5f, size.height * .55f), Offset(size.width * .5f, size.height * .9f), stroke.width)
        drawCircle(KkokColors.Primary, radius = size.width * .05f, center = Offset(size.width * .5f, size.height * .91f))
    }
}

@Composable
private fun AddPlaceChip(onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).drawBehind {
            val strokeWidth = 1.dp.toPx()
            drawRoundRect(
                color = KkokColors.Gray400,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                cornerRadius = CornerRadius(size.height / 2),
                style = Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
            )
        }
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = "민감 위치 추가" }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KkokText("＋", style = KkokTypography.LabelLarge, color = KkokColors.Gray500)
        KkokText("추가", style = KkokTypography.LabelLarge, color = KkokColors.Gray500)
    }
}

@Composable
private fun ProfileMenuCard(onInspectionHistory: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.White).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileMenuRow("SNS 분석 이력", R.drawable.figma_layers, null)
        ProfileMenuRow("검사 이력", R.drawable.figma_clock, onInspectionHistory)
        ProfileMenuRow("알림 설정", R.drawable.figma_bell, null)
    }
}

@Composable
private fun ProfileMenuRow(label: String, icon: Int, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().height(24.dp)
            .then(if (onClick == null) Modifier.semantics { disabled() } else Modifier.clickable(role = Role.Button, onClick = onClick))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (onClick == null) disabled()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FigmaIcon(icon, Modifier.size(20.dp))
        KkokText(label, Modifier.weight(1f), KkokTypography.BodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp))
        ChevronRight()
    }
}

@Composable
private fun ProfileDataCard(overview: ProfileOverview, onToggleAutoDelete: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.White).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        KkokText("내 데이터", style = KkokTypography.TitleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KkokText("원본 사진 분석 후 바로 삭제", Modifier.weight(1f), KkokTypography.BodyMedium)
            ProfileToggle(overview.autoDeleteOriginals, onToggleAutoDelete)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            KkokText("분석 기록 보관 기간", style = KkokTypography.BodyMedium)
            KkokText("${overview.retentionDays}일", style = KkokTypography.BodyMedium, color = KkokColors.Primary)
        }
        KkokText(
            "모든 데이터 삭제",
            Modifier.semantics { contentDescription = "모의 화면에서는 데이터를 삭제할 수 없어요"; disabled() },
            KkokTypography.BodyMedium,
            KkokColors.High,
        )
    }
}

@Composable
private fun ProfileToggle(checked: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier.size(width = 44.dp, height = 26.dp).clip(CircleShape)
            .background(if (checked) KkokColors.Primary else KkokColors.Gray200)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { onToggle() },
            ).semantics {
                contentDescription = "원본 사진 분석 후 바로 삭제"
                stateDescription = if (checked) "켜짐 · 모의 설정" else "꺼짐 · 모의 설정"
            },
    ) {
        Box(
            Modifier.align(if (checked) Alignment.CenterEnd else Alignment.CenterStart).padding(2.dp)
                .size(22.dp).background(KkokColors.White, CircleShape),
        )
    }
}

@Composable
private fun ProfileLoadError(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KkokText("마이 화면을 불러오지 못했어요", style = KkokTypography.TitleMedium)
        KkokText("다시 시도해 주세요", color = KkokColors.Gray500)
        KkokText(
            "다시 불러오기",
            Modifier.clip(CircleShape).background(KkokColors.Secondary).clickable(role = Role.Button, onClick = onRetry)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            KkokTypography.LabelLarge,
            KkokColors.Primary,
        )
    }
}

@Composable
private fun ProfileTabBar(onHome: () -> Unit, onInspectionHistory: () -> Unit, onProtectionHistory: () -> Unit) {
    val bottom = 18.dp
    Row(
        Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(horizontal = 28.dp).padding(top = 10.dp, bottom = bottom),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ProfileTab(R.drawable.figma_home, "홈", false, onHome)
        ProfileTab(R.drawable.figma_clock, "검사 기록", false, onInspectionHistory)
        ProfileTab(R.drawable.figma_pencil, "보호 기록", false, onProtectionHistory)
        ProfileTab(R.drawable.figma_user, "마이", true, {})
    }
}

@Composable
private fun ProfileTab(icon: Int, label: String, active: Boolean, onClick: () -> Unit) {
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

@Composable
private fun ChevronRight() {
    Canvas(Modifier.size(18.dp)) {
        val path = Path().apply {
            moveTo(size.width * .38f, size.height * .2f)
            lineTo(size.width * .68f, size.height * .5f)
            lineTo(size.width * .38f, size.height * .8f)
        }
        drawPath(path, KkokColors.Gray400, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun SettingsIcon() {
    Canvas(Modifier.size(20.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val radius = size.minDimension * .4f
        val points = Path().apply {
            repeat(16) { index ->
                val angle = Math.PI * index / 8.0 - Math.PI / 2
                val r = if (index % 2 == 0) radius else radius * .78f
                val x = center.x + kotlin.math.cos(angle).toFloat() * r
                val y = center.y + kotlin.math.sin(angle).toFloat() * r
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(points, KkokColors.Ink, style = stroke)
        drawCircle(KkokColors.Ink, radius = size.minDimension * .12f, center = center, style = stroke)
    }
}
