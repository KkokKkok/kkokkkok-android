package com.kkokkkok.android.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kkokkkok.android.R
import com.kkokkkok.android.ui.design.*

@Composable
internal fun AnalysisFrame(
    title: String,
    onBack: () -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(40.dp)
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(30.dp)
    val backDescription = stringResource(R.string.analysis_back)
    Column(Modifier.fillMaxSize().background(KkokColors.Background)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).padding(top = top)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Box(Modifier.padding(start = 8.dp).size(48.dp).align(Alignment.CenterStart)
                .clip(CircleShape).clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = backDescription }, contentAlignment = Alignment.Center) {
                IconBadge(R.drawable.figma_back, 40.dp, KkokColors.White)
            }
            KkokText(title, Modifier.align(Alignment.Center).padding(horizontal = 64.dp), KkokTypography.TitleLarge)
            KkokText(stringResource(R.string.analysis_mock_label), Modifier.align(Alignment.CenterEnd).padding(end = 20.dp),
                KkokTypography.LabelLarge, KkokColors.Primary)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(10.dp), content = footer)
    }
}

@Composable
internal fun AnalysisButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(modifier.fillMaxWidth().heightIn(min = if (primary) 60.dp else 63.dp)
        .clip(CircleShape).background(if (primary) KkokColors.Primary else KkokColors.White)
        .border(if (primary) 0.dp else 1.5.dp, if (primary) KkokColors.Primary else KkokColors.Gray200, CircleShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
        KkokText(label, style = KkokTypography.TitleLarge,
            color = if (primary) KkokColors.White else KkokColors.Ink)
    }
}
