package com.kkokkkok.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launcherActivityDisplaysComposeRoot() {
        compose.onRoot().assertIsDisplayed()
    }

    @Test
    fun homeShowsDesignCopyAndSelectedHomeTab() {
        compose.onNodeWithText("올리기 전에, 꼭꼭 확인해요!").assertIsDisplayed()
        compose.onNodeWithText("1단계 · 사진을 골라주세요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("홈").assertIsSelected()
    }

    @Test
    fun accountEntryIsEnabledAndUnavailableFeaturesStayDisabled() {
        compose.onNodeWithContentDescription("알림").assertIsNotEnabled()
        listOf("SNS 누적 검사", "검사 기록", "보호 기록").forEach {
            compose.onNodeWithText(it).assertIsNotEnabled()
        }
        compose.onNodeWithText("보호 편집").assertIsEnabled().assertHasClickAction()
        compose.onNodeWithText("위치 노출 검사").assertIsEnabled().assertHasClickAction()
        compose.onNodeWithText("마이").assertIsEnabled().assertHasClickAction().performClick()
        compose.onNodeWithText("카카오로 시작하기").assertIsDisplayed()
    }

    @Test
    fun shortcutsRemainReachableByScrolling() {
        compose.onNodeWithText("위치 노출 검사").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("SNS 누적 검사").assertIsDisplayed()
    }

    @Test
    fun sensitiveLocationPreviewCanAddAndOpenAMockPlace() {
        compose.onNodeWithText("마이").performClick()
        compose.onNodeWithText("민감 위치 화면 미리보기").performClick()
        compose.onNodeWithText("내 민감 위치").assertIsDisplayed()
        compose.onNodeWithText("＋ 장소 추가하기").performClick()
        compose.onNodeWithTag("location-name").performTextInput("공원")
        compose.onNodeWithText("학교").performClick()
        compose.onNodeWithText("500m").performClick()
        compose.onNodeWithText("저장하기").performClick()
        compose.onNodeWithText("공원").performClick()
        compose.onNodeWithText("단서가 보인 게시물").assertIsDisplayed()
        compose.onNodeWithText("모의 데이터").assertIsDisplayed()
    }
}
