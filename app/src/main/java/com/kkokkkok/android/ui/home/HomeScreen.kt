package com.kkokkkok.android.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import com.kkokkkok.android.R
import com.kkokkkok.android.ui.design.*

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onSelectPhoto: (() -> Unit)? = null,
    onProtectPhoto: (() -> Unit)? = null,
    onOpenAccount: (() -> Unit)? = null,
    onOpenHistory: (() -> Unit)? = null,
    onOpenProtectionHistory: (() -> Unit)? = null,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(44.dp)
    Column(
        modifier.fillMaxSize().background(KkokColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(top = statusTop).testTag("home"),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = KkokSpacing.Page).padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(KkokSpacing.Section),
        ) {
            HomeHeader()
            Greeting()
            ProtectCard(onProtectPhoto)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KkokSpacing.Item)) {
                Shortcut(
                    R.drawable.figma_layers, R.string.home_sns_title, R.string.home_sns_description,
                    Modifier.weight(1f), isSns = true,
                )
                Shortcut(
                    R.drawable.figma_scan, R.string.home_scan_title, R.string.home_scan_description,
                    Modifier.weight(1f), isSns = false, onClick = onSelectPhoto,
                )
            }
        }
        HomeTabBar(onOpenAccount, onOpenHistory, onOpenProtectionHistory)
    }
}

@Composable
private fun HomeHeader() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KkokSpacing.Small)) {
            FigmaIcon(R.drawable.figma_mascot)
            KkokText(stringResource(R.string.app_name), style = KkokTypography.HeadlineMedium, color = KkokColors.Primary)
        }
        val label = stringResource(R.string.home_notifications)
        IconBadge(R.drawable.figma_bell, 44.dp, KkokColors.White, Modifier.unavailable(label))
    }
}

@Composable
private fun Greeting() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KkokText(stringResource(R.string.home_greeting), style = KkokTypography.HeadlineSmall)
        KkokText(stringResource(R.string.home_description), color = KkokColors.Gray600)
    }
}

@Composable
private fun ProtectCard(onClick: (() -> Unit)?) {
    val label = stringResource(R.string.home_protect_title)
    Column(
        Modifier.fillMaxWidth()
            .then(if (onClick == null) Modifier.unavailable() else Modifier.clickable(role = Role.Button, onClick = onClick))
            .dropShadow(RoundedCornerShape(32.dp), Shadow(radius = 24.dp, color = KkokColors.Primary.copy(alpha = .16f), offset = DpOffset(0.dp, 8.dp)))
            .background(KkokColors.White, RoundedCornerShape(32.dp)).padding(KkokSpacing.Card),
        verticalArrangement = Arrangement.spacedBy(KkokSpacing.Card),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 216.dp).clip(RoundedCornerShape(24.dp))
                .background(KkokColors.Secondary).dashedOutline()
                .padding(horizontal = 8.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KkokSpacing.Item, Alignment.CenterVertically),
        ) {
            Box(
                Modifier.size(96.dp).background(KkokColors.White, CircleShape)
                    .border(4.dp, KkokColors.Primary, CircleShape), contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(74.dp).background(KkokColors.Primary, CircleShape), contentAlignment = Alignment.Center) {
                    FigmaIcon(R.drawable.figma_shield)
                }
            }
            KkokText(label, style = KkokTypography.TitleLarge)
            KkokText(stringResource(R.string.home_protect_description), style = KkokTypography.LabelMedium, color = KkokColors.Gray500)
        }
        Column(
            Modifier.fillMaxWidth().background(KkokColors.Background, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                KkokText(stringResource(R.string.home_step_title), Modifier.weight(1f), KkokTypography.TitleMedium)
                KkokText(stringResource(R.string.home_step_count), style = KkokTypography.LabelLarge, color = KkokColors.Primary)
            }
            KkokText(stringResource(R.string.home_step_description), style = KkokTypography.BodySmall, color = KkokColors.Gray600)
            Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    Box(Modifier.weight(1f).height(6.dp).background(if (index == 0) KkokColors.Primary else KkokColors.Gray200, RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}

@Composable
private fun Shortcut(
    @DrawableRes icon: Int,
    titleResource: Int,
    descriptionResource: Int,
    modifier: Modifier,
    isSns: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val title = stringResource(titleResource)
    Column(
        modifier.clip(RoundedCornerShape(24.dp))
            .background(if (isSns) KkokColors.Point else KkokColors.White)
            .then(if (onClick == null) Modifier.unavailable() else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(KkokSpacing.Card),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBadge(icon, 36.dp, if (isSns) KkokColors.White else KkokColors.Secondary)
        KkokText(title, style = KkokTypography.TitleMedium)
        KkokText(stringResource(descriptionResource), style = KkokTypography.BodySmall, color = if (isSns) KkokColors.Ink else KkokColors.Gray600)
    }
}

@Composable
private fun HomeTabBar(
    onOpenAccount: (() -> Unit)?,
    onOpenHistory: (() -> Unit)?,
    onOpenProtectionHistory: (() -> Unit)?,
) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(18.dp)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val tabMaxWidth = (maxWidth - 56.dp) / 4
        Row(
            Modifier.fillMaxWidth().dropShadow(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), Shadow(radius = 10.dp, color = KkokColors.Primary.copy(alpha = .18f), offset = DpOffset(0.dp, (-4).dp)))
                .background(KkokColors.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(horizontal = 28.dp).padding(top = 10.dp, bottom = bottom).selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HomeTab(R.drawable.figma_home, R.string.home_tab_home, Modifier.widthIn(max = tabMaxWidth), true)
            HomeTab(R.drawable.figma_clock, R.string.home_tab_history, Modifier.widthIn(max = tabMaxWidth), onClick = onOpenHistory)
            HomeTab(R.drawable.figma_pencil, R.string.home_tab_protection, Modifier.widthIn(max = tabMaxWidth), onClick = onOpenProtectionHistory)
            HomeTab(R.drawable.figma_user, R.string.home_tab_my, Modifier.widthIn(max = tabMaxWidth), onClick = onOpenAccount)
        }
    }
}

@Composable
private fun HomeTab(
    @DrawableRes icon: Int,
    labelResource: Int,
    modifier: Modifier,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val label = stringResource(labelResource)
    Column(
        modifier.then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Tab, onClick = onClick))
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = active
                if (onClick == null && !active) disabled()
            },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.background(if (active) KkokColors.Secondary else KkokColors.White, CircleShape).padding(horizontal = 14.dp, vertical = 4.dp),
        ) { FigmaIcon(icon) }
        KkokText(label, style = KkokTypography.LabelMedium, color = if (active) KkokColors.Primary else KkokColors.Gray400)
    }
}

private fun Modifier.unavailable(label: String? = null): Modifier = semantics(mergeDescendants = true) {
    if (label != null) contentDescription = label
    role = Role.Button
    disabled()
}

private fun Modifier.dashedOutline(): Modifier = drawBehind {
    val width = 2.dp.toPx()
    drawRoundRect(
        color = KkokColors.Primary,
        topLeft = Offset(width / 2, width / 2),
        size = Size(size.width - width, size.height - width),
        cornerRadius = CornerRadius(24.dp.toPx()),
        style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
    )
}
