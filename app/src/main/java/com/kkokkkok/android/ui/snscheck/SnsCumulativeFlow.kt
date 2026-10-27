package com.kkokkkok.android.ui.snscheck

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
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
            SnsCumulativeStep.RESULT -> viewModel.setStep(SnsCumulativeStep.CLUES)
            SnsCumulativeStep.CLUES -> viewModel.setStep(SnsCumulativeStep.POSTS)
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
            onAnalyze = {
                if (state.selectedPostCount == 0) onMessage("게시물을 한 개 이상 선택해 주세요.")
                else viewModel.setStep(SnsCumulativeStep.CLUES)
            },
            onRetry = viewModel::retry,
        )
        SnsCumulativeStep.CLUES -> RepeatedCluesScreen(
            state = state,
            onBack = { viewModel.setStep(SnsCumulativeStep.POSTS) },
            onAnswer = viewModel::answerCue,
            onNext = viewModel::showResult,
        )
        SnsCumulativeStep.RESULT -> state.result?.let { result ->
            CumulativeResultScreen(
                result = result,
                place = state.places.firstOrNull { it.id == state.selectedPlaceId },
                onBack = { viewModel.setStep(SnsCumulativeStep.CLUES) },
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
    var showConnectionUnavailable by remember { mutableStateOf(false) }
    if (showConnectionUnavailable) {
        SnsConnectionUnavailableDialog(
            onDismiss = { showConnectionUnavailable = false },
            onManualUpload = {
                showConnectionUnavailable = false
                onStart()
            },
        )
    }
    SnsFrame(
        title = "SNS 누적 검사", contentSpacing = 12.dp, onBack = onBack,
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
            StepLine("2", "그 장소의 게시물 올리기", "여러 장을 한 번에 검사해요")
            StepLine("3", "반복되는 단서 분석", "이 장소가 얼마나 드러나는지 알려드려요")
        }
        KkokText("게시물 불러오는 방법", style = KkokTypography.TitleLarge)
        ImportMethodCard(
            icon = R.drawable.figma_upload, title = "사진 직접 올리기", badge = "추천",
            detail = "앨범에서 게시물 사진을 골라요", selected = true,
        )
        ImportMethodCard(
            icon = R.drawable.figma_link, title = "인스타그램 계정 연결",
            detail = "프로페셔널(비즈니스·크리에이터) 계정만 가능해요", selected = false,
            onClick = { showConnectionUnavailable = true },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FigmaIcon(R.drawable.figma_lock)
            KkokText("모의 게시물만 검사해요. 실제 SNS 계정은 연결되지 않아요.", style = KkokTypography.BodySmall, color = KkokColors.Gray500)
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
private fun SnsConnectionUnavailableDialog(onDismiss: () -> Unit, onManualUpload: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect { window.setDimAmount(0f) }
        Box(
            Modifier.fillMaxSize().background(KkokColors.Ink.copy(alpha = .45f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.offset(x = 15.dp, y = 32.dp).widthIn(max = 344.dp).fillMaxWidth().requiredHeight(300.dp)
                    .clip(RoundedCornerShape(28.dp)).background(KkokColors.White)
                    .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FigmaIcon(R.drawable.figma_mascot, Modifier.size(52.dp))
                KkokText(
                    "일반 계정은 연결할 수 없어요",
                    Modifier.fillMaxWidth(),
                    KkokTypography.TitleLarge.copy(textAlign = TextAlign.Center),
                )
                KkokText(
                    "인스타그램 정책상 프로페셔널\n(비즈니스·크리에이터) 계정만 연결돼요.\n게시물 사진을 직접 올려 검사할 수 있어요.",
                    Modifier.fillMaxWidth(),
                    KkokTypography.BodyMedium.copy(textAlign = TextAlign.Center, lineBreak = LineBreak.Simple),
                    KkokColors.Gray600,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SnsDialogButton("닫기", Modifier.weight(1f), primary = false, onClick = onDismiss)
                    SnsDialogButton(
                        "사진 직접 올리기",
                        Modifier.weight(1f).testTag("sns-manual-upload-button"),
                        primary = true,
                        onClick = onManualUpload,
                    )
                }
            }
        }
    }
}

@Composable
private fun SnsDialogButton(label: String, modifier: Modifier, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(CircleShape)
            .background(if (primary) KkokColors.Primary else KkokColors.White)
            .border(if (primary) 0.dp else 1.5.dp, KkokColors.Gray200, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KkokText(label, style = KkokTypography.TitleMedium, color = if (primary) Color.White else KkokColors.Ink)
    }
}

@Composable
private fun PostsScreen(
    state: SnsCumulativeState,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
) {
    var filter by remember { mutableStateOf(PostFilter.ALL) }
    val placeName = state.places.firstOrNull { it.id == state.selectedPlaceId }?.name ?: "선택 장소"
    val visiblePosts = when (filter) {
        PostFilter.ALL -> state.posts
        PostFilter.RECENT -> state.posts.filter { it.period == SnsPostPeriod.RECENT }
        PostFilter.YEAR -> state.posts.filter { it.period == SnsPostPeriod.THIS_YEAR }
    }
    val selectedOrder = state.posts.filter { it.id in state.selectedPostIds }.mapIndexed { index, post -> post.id to index + 1 }.toMap()
    SnsFrame(
        title = "게시물 선택", right = "2 / 3", onBack = onBack,
        footer = { SnsPrimaryButton("${state.selectedPostCount}개 분석하기", enabled = !state.isLoading && !state.hasError, onClick = onAnalyze) },
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
            visiblePosts.isEmpty() -> KkokText("이 기간에 선택할 게시물이 없어요.", Modifier.padding(vertical = 20.dp), color = KkokColors.Gray600)
            else -> PostGrid(visiblePosts, state.selectedPostIds, selectedOrder, onToggle)
        }
    }
}

@Composable
private fun PostGrid(
    posts: List<SnsPostPreview>,
    selected: Set<String>,
    selectedOrder: Map<String, Int>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Image(androidx.compose.ui.res.painterResource(post.thumbnail), contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(if (isSelected) 3.dp else 0.dp).clip(RoundedCornerShape(14.dp)))
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
    }
}
