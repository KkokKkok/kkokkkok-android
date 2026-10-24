package com.kkokkkok.android

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kkokkkok.android.protection.ProtectionEditViewModel
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProtectionEditFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun protectionEditShowsExplicitlyMockedBeforeAndAfterResult() {
        val edit = ProtectionEditViewModel()
        compose.setContent { KkokKkokApp(PhotoSelectionViewModel(), edit) }
        compose.runOnIdle { edit.begin(Uri.parse("content://synthetic/photo")) }

        compose.onNodeWithText("어디에서 찍은 사진인가요?").assertIsDisplayed()
        compose.onNodeWithText("분석하고 수정하기").performClick()
        compose.onNodeWithText("다음 · 요청 입력").performClick()
        compose.onNodeWithText("보호 이미지 만들기").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("모의").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Figma 예시 · 선택한 사진은 수정되지 않았어요.")
            .assertIsDisplayed()
        compose.onNodeWithText("사진 저장하기").assertIsDisplayed()
        compose.onNodeWithText("다시 수정").performClick()
        compose.onNodeWithText("보호 이미지 만들기").assertIsDisplayed()
    }
}
