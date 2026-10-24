package com.kkokkkok.android.ui.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.kkokkkok.android.R
import com.kkokkkok.android.account.AccountUiState
import com.kkokkkok.android.account.Agreement
import com.kkokkkok.android.account.AgreementState
import com.kkokkkok.android.account.SignInProvider
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokSpacing
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

@Composable
fun AccountLoginScreen(
    state: AccountUiState,
    onProvider: (SignInProvider) -> Unit,
    onDismissMessage: () -> Unit,
) {
    AccountFrame(state.message, onDismissMessage, Modifier.testTag("account-login")) {
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(40.dp) - 8.dp
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp)
        Column(
            Modifier.fillMaxSize().background(KkokColors.Secondary).padding(top = statusTop, bottom = navBottom),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = KkokSpacing.Page).padding(top = 124.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FigmaIcon(R.drawable.figma_mascot, Modifier.size(112.dp))
                KkokText("꼭꼭", style = KkokTypography.Score.copy(fontSize = 52.sp, lineHeight = 60.sp), color = KkokColors.Primary)
                KkokText("올리기 전에, 꼭꼭 확인해요", style = KkokTypography.TitleLarge)
                KkokText("사진 속 위치 단서를 미리 찾아 지워 드려요", style = KkokTypography.LabelLarge, color = KkokColors.Gray600)
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = KkokSpacing.Page),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProviderButton(
                    "카카오로 시작하기", R.drawable.figma_kakao, KkokColors.Kakao,
                    KkokColors.Ink, Modifier.testTag("account-kakao"),
                ) { onProvider(SignInProvider.KAKAO) }
                ProviderButton(
                    "네이버로 시작하기", R.drawable.figma_naver, KkokColors.Naver,
                    KkokColors.White, Modifier.testTag("account-naver"),
                ) { onProvider(SignInProvider.NAVER) }
            }
        }
        AccountToast(state.message)
    }
}
@Composable
fun AccountTermsScreen(
    state: AccountUiState,
    onBack: () -> Unit,
    onToggleAll: () -> Unit,
    onToggle: (Agreement) -> Unit,
    onOpenTerms: () -> Unit,
    onContinue: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    AccountFrame(state.message, onDismissMessage, Modifier.testTag("account-terms")) {
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(40.dp) - 8.dp
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(18.dp)
        Column(Modifier.fillMaxSize().background(KkokColors.Background).padding(top = statusTop)) {
            AccountHeader(onBack)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = KkokSpacing.Page).padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                KkokText(
                    "꼭꼭을 시작하기 전에\n약관에 동의해 주세요",
                    style = KkokTypography.HeadlineSmall,
                )
                Row(
                    Modifier.clip(CircleShape).background(KkokColors.White).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(18.dp).background(KkokColors.Kakao, CircleShape))
                    KkokText("${state.provider?.displayName ?: "카카오"} 연결 화면 미리보기", style = KkokTypography.BodySmall, color = KkokColors.Gray600)
                }
                Row(
                    Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(20.dp))
                        .background(KkokColors.White).border(2.dp, KkokColors.Primary, RoundedCornerShape(20.dp))
                        .clickable(role = Role.Checkbox, onClick = onToggleAll)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AgreementCheck(state.agreements.requiredAccepted, large = true)
                    KkokText("전체 동의", style = KkokTypography.TitleLarge)
                }
                AgreementCard(state.agreements, onToggle, onOpenTerms)
                KkokText(
                    "실제 계정 연결이나 약관 동의는 처리되지 않는 화면 미리보기예요.",
                    style = KkokTypography.LabelSmall,
                    color = KkokColors.Gray500,
                )
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = KkokSpacing.Page).padding(top = 12.dp, bottom = navBottom)) {
                PrimaryButton(
                    "동의하고 계속하기",
                    enabled = state.agreements.requiredAccepted,
                    modifier = Modifier.fillMaxWidth().testTag("account-continue"),
                    onClick = onContinue,
                )
            }
        }
        AccountToast(state.message)
    }
}

@Composable
private fun AccountFrame(
    message: String?,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        content()
        LaunchedEffect(message) {
            if (message != null) {
                delay(3_000)
                onDismissMessage()
            }
        }
    }
}

@Composable
private fun AccountToast(message: String?) {
    if (message != null) {
        Box(
            Modifier.fillMaxSize().padding(start = KkokSpacing.Page, end = KkokSpacing.Page, bottom = 196.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(KkokColors.Ink)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FigmaIcon(R.drawable.figma_account_alert, Modifier.size(20.dp))
                KkokText(message, Modifier.weight(1f), KkokTypography.BodySmall, KkokColors.White)
            }
        }
    }
}

@Composable
private fun ProviderButton(
    text: String,
    @DrawableRes icon: Int,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(14.dp)).background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FigmaIcon(icon)
        Spacer(Modifier.width(10.dp))
        KkokText(text, style = KkokTypography.TitleLarge, color = foreground)
    }
}

@Composable
private fun AccountHeader(onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(40.dp)
                .background(KkokColors.White, CircleShape).clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "뒤로 가기"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) { FigmaIcon(R.drawable.figma_back) }
        KkokText("회원가입", Modifier.align(Alignment.Center), KkokTypography.TitleLarge)
        KkokText("1 / 2", Modifier.align(Alignment.CenterEnd).padding(end = KkokSpacing.Page), KkokTypography.LabelLarge, KkokColors.Primary)
    }
}

@Composable
private fun AgreementCard(
    state: AgreementState,
    onToggle: (Agreement) -> Unit,
    onOpenTerms: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(KkokColors.White)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AgreementRow("[필수]", "서비스 이용약관", state.serviceTerms, Agreement.SERVICE_TERMS, onToggle, onOpenTerms)
        AgreementRow("[필수]", "개인정보 수집·이용 동의", state.privacy, Agreement.PRIVACY, onToggle, onOpenTerms)
        AgreementRow(
            "[필수]", "위치정보 이용 동의", state.location, Agreement.LOCATION, onToggle, onOpenTerms,
            detail = "입력한 위치는 위험 판단에만 사용해요",
        )
        AgreementRow(
            "[선택]", "알림 수신 동의", state.marketing, Agreement.MARKETING, onToggle, onOpenTerms,
            detail = "반복 노출 경고 등",
        )
    }
}

@Composable
private fun AgreementRow(
    prefix: String,
    label: String,
    checked: Boolean,
    agreement: Agreement,
    onToggle: (Agreement) -> Unit,
    onOpenTerms: () -> Unit,
    detail: String? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 26.dp).clickable(role = Role.Checkbox) { onToggle(agreement) }
            .semantics(mergeDescendants = true) {
                contentDescription = "$prefix $label"
                role = Role.Checkbox
                if (checked) stateDescription = "선택됨" else stateDescription = "선택 안 됨"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (checked) KkokColors.Primary else KkokColors.Gray100),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) FigmaIcon(R.drawable.figma_terms_check_small, Modifier.size(13.dp))
            else FigmaIcon(R.drawable.figma_terms_check_muted, Modifier.size(13.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KkokText(prefix, style = KkokTypography.BodySmall, color = if (checked) KkokColors.Primary else KkokColors.Gray500)
                Spacer(Modifier.width(4.dp))
                KkokText(label, style = KkokTypography.BodySmall, color = KkokColors.Ink)
            }
            if (detail != null) KkokText(detail, style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
        }
        KkokText(
            "보기",
            Modifier.clickable(role = Role.Button, onClick = onOpenTerms).padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                .semantics { role = Role.Button; contentDescription = "$label 보기" },
            style = KkokTypography.LabelSmall,
            color = KkokColors.Gray400,
        )
    }
}

@Composable
private fun AgreementCheck(checked: Boolean, large: Boolean) {
    val diameter = if (large) 28.dp else 22.dp
    Box(
        Modifier.size(diameter).background(
            when {
                checked -> KkokColors.Primary
                large -> KkokColors.White
                else -> KkokColors.Gray100
            }, CircleShape,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) FigmaIcon(R.drawable.figma_terms_check, Modifier.size(16.dp))
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.height(60.dp).clip(CircleShape)
            .background(if (enabled) KkokColors.Primary else KkokColors.Gray200)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!enabled) stateDescription = "필수 항목에 동의해야 계속할 수 있어요"
            },
        contentAlignment = Alignment.Center,
    ) {
        KkokText(text, style = KkokTypography.TitleLarge, color = KkokColors.White)
    }
}
