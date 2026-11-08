package com.kkokkkok.android.ui.snscheck

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kkokkkok.android.R
import com.kkokkkok.android.snscheck.SnsCumulativeState
import com.kkokkkok.android.snscheck.SnsCumulativeViewModel
import com.kkokkkok.android.snscheck.SnsCumulativeStep
import com.kkokkkok.android.snscheck.SnsPlaceKind
import com.kkokkkok.android.snscheck.SnsPlaceOption
import com.kkokkkok.android.snscheck.SnsPostPeriod
import com.kkokkkok.android.snscheck.SnsPostPreview
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.analysis.PhotoImage
import com.kkokkkok.android.ui.analysis.rememberPhotoImage
import com.kkokkkok.android.ui.design.KkokTypography

private enum class PostFilter(val label: String) { ALL("전체"), RECENT("최근 3개월"), YEAR("올해") }

@Composable
fun SnsCumulativeFlow(
    viewModel: SnsCumulativeViewModel,
    onClose: () -> Unit,
    onAddPlace: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler {
        when (state.step) {
            SnsCumulativeStep.RESULT -> viewModel.setStep(SnsCumulativeStep.POSTS)
            SnsCumulativeStep.POSTS -> viewModel.setStep(SnsCumulativeStep.PLACE)
            SnsCumulativeStep.PLACE -> viewModel.setStep(SnsCumulativeStep.INTRO)
            SnsCumulativeStep.INTRO -> {
                viewModel.setStep(SnsCumulativeStep.INTRO)
                onClose()
            }
        }
    }
    when (state.step) {
        SnsCumulativeStep.INTRO -> IntroScreen(onBack = onClose, onStart = { viewModel.setStep(SnsCumulativeStep.PLACE) })
        SnsCumulativeStep.PLACE -> PlaceScreen(
            state = state,
            onBack = { viewModel.setStep(SnsCumulativeStep.INTRO) },
            onSelect = viewModel::selectPlace,
            onAddPlace = onAddPlace,
            onNext = { viewModel.setStep(SnsCumulativeStep.POSTS) },
            onRetry = viewModel::retry,
        )
        SnsCumulativeStep.POSTS -> PostsScreen(
            state = state,
            onBack = { viewModel.setStep(SnsCumulativeStep.PLACE) },
            onToggle = viewModel::togglePost,
            onSelectAll = viewModel::selectAllVisible,
            onLatitude = viewModel::updateManualLatitude,
            onLongitude = viewModel::updateManualLongitude,
            onAnalyze = {
                if (state.selectedPostCount == 0) onMessage("게시물을 한 개 이상 선택해 주세요.")
                else {
                    val missing = state.posts.filter { it.id in state.selectedPostIds && it.locationStatus != "available" && it.locationStatus != "manual" }
                    val incomplete = missing.firstOrNull { state.manualLocations[it.id]?.first.isNullOrBlank() || state.manualLocations[it.id]?.second.isNullOrBlank() }
                    if (incomplete != null) onMessage("${incomplete.title}: 촬영 위도와 경도를 입력해 주세요.")
                    else viewModel.showResult()
                }
            },
            onRetry = viewModel::retry,
        )
            SnsCumulativeStep.RESULT -> if (state.isAnalyzing) {
                SnsFrame("누적 분석", onBack = { viewModel.setStep(SnsCumulativeStep.POSTS) }, footer = {}) {
                    KkokText("저장된 사진의 분석 결과와 촬영 위치를 살펴보고 있어요…", style = KkokTypography.TitleLarge)
                }
            } else if (state.resultError) {
                SnsFrame("누적 분석", onBack = { viewModel.setStep(SnsCumulativeStep.POSTS) },
                    footer = { SnsPrimaryButton("다시 시도", onClick = viewModel::showResult) }) {
                    KkokText("누적 분석을 완료하지 못했어요. 서버 연결과 선택한 사진을 확인해 주세요.", color = KkokColors.Gray600)
                }
            } else state.result?.let { result ->
            CumulativeResultScreen(
                result = result,
                place = state.places.firstOrNull { it.id == state.selectedPlaceId },
                onBack = { viewModel.setStep(SnsCumulativeStep.POSTS) },
                onOtherPlace = { viewModel.setStep(SnsCumulativeStep.PLACE) },
                onConfirm = {
                    viewModel.setStep(SnsCumulativeStep.INTRO)
                    onClose()
                },
            )
        }
    }
}

@Composable
internal fun SnsFrame(
    title: String,
    right: String? = null,
    contentSpacing: androidx.compose.ui.unit.Dp = 14.dp,
    centerTitle: Boolean = false,
    onBack: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val top = 40.dp
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(18.dp)
    Column(Modifier.fillMaxSize().background(KkokColors.Background)
        .padding(top = top)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 12.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(KkokColors.White)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "뒤로 가기" }, contentAlignment = Alignment.Center) {
                FigmaIcon(R.drawable.figma_back)
            }
            KkokText(title, Modifier.weight(1f).padding(horizontal = 8.dp), KkokTypography.TitleLarge.copy(
                textAlign = if (centerTitle) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
            ), color = KkokColors.Ink)
            if (right != null) KkokText(right, Modifier.widthIn(min = 34.dp), KkokTypography.LabelLarge, KkokColors.Primary)
            else Spacer(Modifier.width(40.dp))
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing), content = content,
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = bottom), content = footer)
    }
}

@Composable
internal fun SnsPrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(60.dp).clip(CircleShape)
            .background(if (enabled) KkokColors.Primary else KkokColors.Gray200)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        KkokText(label, style = KkokTypography.TitleLarge, color = if (enabled) Color.White else KkokColors.Gray500)
    }
}

@Composable
private fun IntroScreen(onBack: () -> Unit, onStart: () -> Unit) {
    SnsFrame(
        title = "누적 검사", contentSpacing = 12.dp, onBack = onBack,
        footer = { SnsPrimaryButton("시작하기", onClick = onStart) },
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(KkokColors.Point)
            .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(40.dp).background(KkokColors.White, CircleShape), contentAlignment = Alignment.Center) {
                FigmaIcon(R.drawable.figma_mascot, Modifier.size(24.dp))
            }
            KkokText(
                "쌓인 게시물 속에 생활권이 드러나 있진 않을까요?",
                style = KkokTypography.HeadlineSmall.copy(lineBreak = LineBreak.Simple),
            )
            KkokText("같은 배경이 반복되면 사진 한 장보다 더 많은 걸 알려줄 수 있어요.", style = KkokTypography.BodySmall)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.White).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StepLine("1", "지킬 장소 1곳 고르기", "내 민감 위치 중 하나를 골라요")
            StepLine("2", "저장된 사진 고르기", "단독 검사 때 저장한 사진을 골라요")
            StepLine("3", "반복되는 단서 분석", "이 장소가 얼마나 드러나는지 알려드려요")
        }
        KkokText("검사에 쓰는 사진", style = KkokTypography.TitleLarge)
        ImportMethodCard(
            icon = R.drawable.figma_upload, title = "서비스에 저장된 사진", badge = "사용 중",
            detail = "단독 검사 때 저장한 원본 사진을 골라요", selected = true,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FigmaIcon(R.drawable.figma_lock)
            KkokText("실제 저장 사진과 완료된 분석 기록을 사용해요. SNS 계정 연결은 제외했어요.", style = KkokTypography.BodySmall, color = KkokColors.Gray500)
        }
    }
}

@Composable
private fun StepLine(number: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).background(KkokColors.Secondary, CircleShape), contentAlignment = Alignment.Center) {
            KkokText(number, style = KkokTypography.LabelLarge, color = KkokColors.Primary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            KkokText(title, style = KkokTypography.TitleMedium)
            KkokText(detail, style = KkokTypography.BodySmall, color = KkokColors.Gray500)
        }
    }
}

@Composable
private fun ImportMethodCard(
    @androidx.annotation.DrawableRes icon: Int,
    title: String,
    badge: String? = null,
    detail: String,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(if (selected) 80.dp else 74.dp).clip(RoundedCornerShape(22.dp))
            .background(if (selected) KkokColors.Secondary else KkokColors.White)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) KkokColors.Primary else KkokColors.Gray200, RoundedCornerShape(22.dp))
            .then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(if (selected) KkokColors.Primary else KkokColors.Gray100, CircleShape), contentAlignment = Alignment.Center) {
            FigmaIcon(icon)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KkokText(title, style = KkokTypography.TitleMedium)
                if (badge != null) Box(Modifier.clip(CircleShape).background(KkokColors.Primary).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    KkokText(badge, style = KkokTypography.LabelSmall, color = Color.White)
                }
            }
            KkokText(detail, style = KkokTypography.BodySmall, color = KkokColors.Gray500)
        }
    }
}

@Composable
private fun PlaceScreen(
    state: SnsCumulativeState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onAddPlace: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
) {
    SnsFrame("지킬 장소 고르기", "1 / 3", onBack = onBack,
        footer = { SnsPrimaryButton("다음 · 게시물 올리기", enabled = !state.isLoading && !state.hasError && state.selectedPlaceId.isNotBlank(), onClick = onNext) },
    ) {
        KkokText("어떤 장소를 지켜볼까요?", style = KkokTypography.HeadlineSmall)
        KkokText("한 번에 한 장소씩 검사해요. 그 장소와 관련된 게시물을 여러 장 올려 주세요.", style = KkokTypography.BodyMedium, color = KkokColors.Gray500)
        when {
            state.isLoading -> KkokText("장소를 불러오고 있어요…", Modifier.padding(vertical = 20.dp), color = KkokColors.Gray600)
            state.hasError -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KkokText("장소를 불러오지 못했어요. 다시 시도해 주세요.", color = KkokColors.Gray600)
                SnsPrimaryButton("다시 불러오기", onClick = onRetry)
            }
            else -> {
                state.places.forEach { place -> PlaceCard(place, state.selectedPlaceId == place.id, onClick = { onSelect(place.id) }) }
                AddPlaceOption(onAddPlace)
            }
        }
    }
}

@Composable
private fun PlaceCard(place: SnsPlaceOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(24.dp))
            .background(if (selected) KkokColors.Secondary else KkokColors.White)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) KkokColors.Primary else KkokColors.Gray200, RoundedCornerShape(24.dp))
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(22.dp).border(2.dp, if (selected) KkokColors.Primary else KkokColors.Gray400, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(12.dp).background(KkokColors.Primary, CircleShape))
        }
        Box(Modifier.size(40.dp).background(if (selected) KkokColors.Primary else KkokColors.Gray100, CircleShape), contentAlignment = Alignment.Center) {
            FigmaIcon(when (place.kind) {
                SnsPlaceKind.HOME -> R.drawable.figma_place_home
                SnsPlaceKind.SCHOOL -> R.drawable.figma_place_school
                SnsPlaceKind.OTHER -> R.drawable.figma_scan
            })
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            KkokText(place.name, style = KkokTypography.TitleMedium)
            KkokText(place.detail, style = KkokTypography.BodySmall, color = KkokColors.Gray500)
            KkokText(place.metadata, style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
        }
    }
}

@Composable
private fun AddPlaceOption(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(24.dp)).dashedPlaceBorder()
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        FigmaIcon(R.drawable.figma_plus)
        KkokText("새 장소 추가 (위치 입력)", style = KkokTypography.TitleMedium, color = KkokColors.Gray500)
    }
}

private fun Modifier.dashedPlaceBorder() = drawBehind {
    val stroke = 1.5.dp.toPx()
    drawRoundRect(
        color = KkokColors.Gray400,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(24.dp.toPx()),
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
    )
}

@Composable
private fun PostsScreen(
    state: SnsCumulativeState,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onLatitude: (String, String) -> Unit,
    onLongitude: (String, String) -> Unit,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
) {
    var filter by remember { mutableStateOf(PostFilter.ALL) }
    val placeName = state.places.firstOrNull { it.id == state.selectedPlaceId }?.name ?: "선택 장소"
    val visiblePosts = when (filter) {
        PostFilter.ALL -> state.posts
        PostFilter.RECENT -> state.posts.filter { post ->
            runCatching { java.time.Instant.parse(post.createdAt).isAfter(java.time.Instant.now().minusSeconds(90L * 24 * 60 * 60)) }
                .getOrDefault(false)
        }
        PostFilter.YEAR -> state.posts.filter { post ->
            runCatching { java.time.Instant.parse(post.createdAt).atZone(java.time.ZoneId.systemDefault()).year == java.time.Year.now().value }
                .getOrDefault(false)
        }
    }
    val selectedOrder = state.posts.filter { it.id in state.selectedPostIds }.mapIndexed { index, post -> post.id to index + 1 }.toMap()
    SnsFrame(
        title = "게시물 선택", right = "2 / 3", onBack = onBack,
        footer = { SnsPrimaryButton(if (state.isAnalyzing) "분석 중…" else "${state.selectedPostCount}개 누적 분석하기", enabled = !state.isLoading && !state.hasError && !state.isAnalyzing, onClick = onAnalyze) },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KkokText("$placeName · ${state.selectedPostCount}개 선택", Modifier.weight(1f), KkokTypography.TitleMedium, KkokColors.Primary)
            Box(Modifier.clip(CircleShape).background(KkokColors.Ink)
                .clickable(role = Role.Button, onClick = { onSelectAll(visiblePosts.map { it.id }) })
                .padding(horizontal = 12.dp, vertical = 7.dp)) {
                KkokText(if (visiblePosts.isNotEmpty() && visiblePosts.all { it.id in state.selectedPostIds }) "전체 해제" else "전체 선택 ✓",
                    style = KkokTypography.LabelSmall, color = Color.White)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PostFilter.entries.forEach { option ->
                val active = option == filter
                Box(Modifier.clip(CircleShape)
                    .background(if (active) KkokColors.Secondary else KkokColors.White)
                    .border(1.5.dp, if (active) KkokColors.Primary else KkokColors.Gray200, CircleShape)
                    .clickable(role = Role.RadioButton, onClick = { filter = option })
                    .padding(horizontal = 12.dp, vertical = 7.dp)) {
                    KkokText(option.label, style = KkokTypography.LabelSmall, color = if (active) KkokColors.Primary else KkokColors.Gray500)
                }
            }
        }
        when {
            state.isLoading -> KkokText("게시물을 불러오고 있어요…", Modifier.padding(vertical = 20.dp), color = KkokColors.Gray600)
            state.hasError -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KkokText("게시물을 불러오지 못했어요. 다시 시도해 주세요.", color = KkokColors.Gray600)
                SnsPrimaryButton("다시 불러오기", onClick = onRetry)
            }
            state.posts.isEmpty() -> KkokText("서비스에 저장된 사진이 없어요. 먼저 사진을 올리고 단독 검사를 완료해 주세요.", Modifier.padding(vertical = 20.dp), color = KkokColors.Gray600)
            visiblePosts.isEmpty() -> KkokText("이 기간에 선택할 사진이 없어요.", Modifier.padding(vertical = 20.dp), color = KkokColors.Gray600)
            else -> PostGrid(visiblePosts, state.selectedPostIds, selectedOrder, state.manualLocations, onToggle, onLatitude, onLongitude)
        }
    }
}

@Composable
private fun PostGrid(
    posts: List<SnsPostPreview>,
    selected: Set<String>,
    selectedOrder: Map<String, Int>,
    manualLocations: Map<String, Pair<String, String>>,
    onToggle: (String) -> Unit,
    onLatitude: (String, String) -> Unit,
    onLongitude: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        posts.chunked(3).forEach { rowPosts ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowPosts.forEach { post ->
                    val isSelected = post.id in selected
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        Box(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                                .border(if (isSelected) 3.dp else 0.dp, if (isSelected) KkokColors.Primary else Color.Transparent, RoundedCornerShape(16.dp))
                                .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle(post.id) })
                                .semantics {
                                    contentDescription = if (isSelected) "선택된 게시물 ${selectedOrder[post.id]}" else "선택하지 않은 게시물"
                                },
                        ) {
                            val photo = post.imageUri?.let { rememberPhotoImage(it, 0) }
                            when (photo) {
                                is PhotoImage.Loaded -> Image(photo.bitmap, contentDescription = post.title,
                                    modifier = Modifier.fillMaxSize().padding(if (isSelected) 3.dp else 0.dp).clip(RoundedCornerShape(14.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                else -> Box(Modifier.fillMaxSize().background(KkokColors.Gray100), contentAlignment = Alignment.Center) {
                                    KkokText(if (photo == PhotoImage.Failed) "사진을 불러오지 못했어요" else "불러오는 중…", color = KkokColors.Gray500, style = KkokTypography.LabelSmall)
                                }
                            }
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(5.dp).size(26.dp)
                                    .background(if (isSelected) KkokColors.Primary else Color.White.copy(alpha = .9f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) KkokText(selectedOrder[post.id].toString(), style = KkokTypography.LabelSmall, color = Color.White)
                            }
                        }
                    }
                }
                repeat(3 - rowPosts.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        posts.filter { it.id in selected && it.locationStatus != "available" && it.locationStatus != "manual" }.forEach { post ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(KkokColors.White).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KkokText("${post.title}: 촬영 위치가 없어 직접 입력해 주세요.", style = KkokTypography.LabelSmall, color = KkokColors.Gray600)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ManualCoordinateField("위도", manualLocations[post.id]?.first.orEmpty(), { onLatitude(post.id, it) }, Modifier.weight(1f), "cumulative-latitude-${post.id}")
                    ManualCoordinateField("경도", manualLocations[post.id]?.second.orEmpty(), { onLongitude(post.id, it) }, Modifier.weight(1f), "cumulative-longitude-${post.id}")
                }
            }
        }
    }
}

@Composable
private fun ManualCoordinateField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier, tag: String) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        KkokText(label, style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
        BasicTextField(value, onValue, modifier = Modifier.testTag(tag), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = KkokTypography.BodySmall.copy(color = KkokColors.Ink),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(KkokColors.Background).padding(8.dp)) {
                    if (value.isEmpty()) KkokText("예: 37.5665", style = KkokTypography.LabelSmall, color = KkokColors.Gray400)
                    inner()
                }
            })
    }
}
