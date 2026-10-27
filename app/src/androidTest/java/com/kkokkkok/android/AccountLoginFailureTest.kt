package com.kkokkkok.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.unit.dp
import com.kkokkkok.android.account.AccountUiState
import com.kkokkkok.android.account.SignInProvider
import com.kkokkkok.android.ui.account.AccountLoginScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountLoginFailureTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun mockLoginFailureShowsFigmaToastAndDismissesAfterThreeSeconds() {
        var state by mutableStateOf(
            AccountUiState(
                provider = SignInProvider.KAKAO,
                message = "카카오 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.",
            ),
        )
        compose.setContent {
            AccountLoginScreen(
                state = state,
                onProvider = {},
                onDismissMessage = { state = state.copy(message = null) },
            )
        }

        compose.onNodeWithText("카카오 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.").assertIsDisplayed()
        compose.onNodeWithTag("account-login-toast").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithText("카카오로 시작하기").assertIsDisplayed()
        compose.onNodeWithText("네이버로 시작하기").assertIsDisplayed()

        compose.mainClock.advanceTimeBy(3_500)
        compose.waitForIdle()
        compose.onNodeWithText("카카오 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.").assertDoesNotExist()
    }
}
