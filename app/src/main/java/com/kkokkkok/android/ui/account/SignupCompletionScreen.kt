package com.kkokkkok.android.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kkokkkok.android.R
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokSpacing
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

@Composable
fun SignupCompletionScreen(
    onBack: () -> Unit,
    onRegisterPlace: () -> Unit,
    onStart: () -> Unit,
    onLater: () -> Unit,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(40.dp) - 8.dp
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(20.dp)
    Column(
        Modifier.fillMaxSize().background(KkokColors.Background).padding(top = statusTop),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 12.dp, end = KkokSpacing.Page),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(KkokColors.White)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "뒤로 가기"; role = Role.Button },
                contentAlignment = Alignment.Center,
            ) { FigmaIcon(R.drawable.figma_back) }
            KkokText("회원가입", style = KkokTypography.TitleLarge)
            KkokText("2 / 2", style = KkokTypography.LabelLarge, color = KkokColors.Primary)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = KkokSpacing.Page).padding(top = 4.dp, bottom = 16.dp)
                .testTag("signup-completion")
                .semantics { stateDescription = "모의 가입 화면이며 계정이나 위치 정보는 저장되지 않습니다." },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(96.dp).background(KkokColors.Secondary, CircleShape), contentAlignment = Alignment.Center) {
                    FigmaIcon(R.drawable.figma_signup_mascot, Modifier.size(72.dp))
                }
                KkokText("반가워요, 테스터님!", style = KkokTypography.HeadlineSmall)
                KkokText("이제 사진을 올리기 전에 꼭꼭 확인할 수 있어요", style = KkokTypography.BodyMedium, color = KkokColors.Gray600)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KkokText("닉네임", style = KkokTypography.LabelLarge, color = KkokColors.Gray600)
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(KkokColors.White)
                        .border(1.5.dp, KkokColors.Gray200, RoundedCornerShape(16.dp))
                        .height(54.dp).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { KkokText("테스터", style = KkokTypography.BodyMedium) }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.Point).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(36.dp).background(KkokColors.White, CircleShape), contentAlignment = Alignment.Center) {
                        FigmaIcon(R.drawable.figma_location_pin, Modifier.size(18.dp))
                    }
                    KkokText("첫 민감 위치를 등록해 볼까요?", Modifier.weight(1f), KkokTypography.TitleMedium)
                }
                KkokText(
                    "집 주변이나 학교를 등록해 두면, 그 장소가 드러나는 사진을 더 꼼꼼히 찾아요.",
                    style = KkokTypography.BodySmall,
                    color = KkokColors.Ink,
                )
                Box(
                    Modifier.fillMaxWidth().clip(CircleShape).background(KkokColors.White)
                        .clickable(role = Role.Button, onClick = onRegisterPlace)
                        .semantics { role = Role.Button }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { KkokText("장소 등록하기", style = KkokTypography.TitleLarge) }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = KkokSpacing.Page).padding(top = 8.dp, bottom = navBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().clip(CircleShape).background(KkokColors.Primary)
                    .clickable(role = Role.Button, onClick = onStart)
                    .semantics { role = Role.Button }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) { KkokText("꼭꼭 시작하기", style = KkokTypography.TitleLarge, color = KkokColors.White) }
            KkokText(
                "나중에 할게요",
                Modifier.clickable(role = Role.Button, onClick = onLater).semantics { role = Role.Button },
                KkokTypography.LabelLarge,
                KkokColors.Gray500,
            )
        }
    }
}
