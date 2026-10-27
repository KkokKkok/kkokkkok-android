package com.kkokkkok.android

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.analysis.*
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import java.io.File

@RunWith(AndroidJUnit4::class)
class AnalysisFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun selectedPhotoCanCancelThenFinishMockResultAndReturnHome() {
        val fixture = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "synthetic-flow.png")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(45, 180, 120))
        fixture.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val finish = CompletableDeferred<Unit>()
        val model = PhotoSelectionViewModel(object : AnalysisRepository {
            override fun analyze(photo: Uri, location: String) = flow {
                emit(AnalysisState.Running(AnalysisStep.TEXT, 25))
                finish.await()
                emit(MockAnalysisRepository(0).analyze(photo, location).last())
            }
        })
        try {
            compose.setContent { KkokKkokApp(model) }
            compose.runOnIdle { model.onPhotoPicked(Uri.fromFile(fixture)) }
            compose.onNodeWithText("촬영 위치 입력").assertIsDisplayed()
            compose.onNodeWithTag("photo-location-submit").performClick()
            compose.onNodeWithText("검사 취소").performClick()
            compose.runOnIdle {
                assertEquals(AnalysisState.Idle, model.analysisState.value)
                assertEquals(Uri.fromFile(fixture), model.selectedPhoto.value)
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("촬영 위치 입력").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("촬영 위치 입력").assertIsDisplayed()
            compose.onNodeWithTag("photo-location-submit").performClick()
            compose.onNodeWithText("모의로 살펴보는 중이에요").assertIsDisplayed()
            finish.complete(Unit)
            compose.waitUntil(5_000) { model.analysisState.value is AnalysisState.Completed }
            compose.onNodeWithText("모의 단서 3개를 표시했어요").assertIsDisplayed()
            compose.onNodeWithText("이대로 저장").performClick()
            compose.onNodeWithText("아직 위험 단서가 남아 있어요").assertIsDisplayed()
            compose.onNodeWithText("그래도 저장").performClick()
            compose.onNodeWithText("모의 단서 3개를 표시했어요").assertIsDisplayed()
            compose.onNodeWithText("이대로 저장").performClick()
            compose.onNodeWithText("보호 편집하기").performClick()
            compose.onNodeWithText("어디에서 찍은 사진인가요?").assertIsDisplayed()
            compose.onNodeWithContentDescription("뒤로").performClick()
            compose.onNodeWithText("모의 단서 3개를 표시했어요").assertIsDisplayed()
            compose.onNodeWithContentDescription("뒤로 가기").performClick()
            compose.onNodeWithText("촬영 위치 입력").assertIsDisplayed()
        } finally {
            compose.runOnIdle { model.cancelAnalysis() }
            fixture.delete()
        }
    }
    @Test
    fun unreadablePhotoShowsFailureAndRetryCanRecover() {
        val fixture = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "synthetic-retry.png")
        fixture.delete()
        val model = PhotoSelectionViewModel(MockAnalysisRepository(0))
        try {
            compose.setContent { KkokKkokApp(model) }
            compose.runOnIdle { model.onPhotoPicked(Uri.fromFile(fixture)); model.startAnalysis() }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("앗, 검사를 끝내지 못했어요").fetchSemanticsNodes().isNotEmpty()
            }
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            fixture.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            compose.onNodeWithText("다시 시도").performClick()
            compose.onNodeWithText("모의 단서 3개를 표시했어요").assertIsDisplayed()
            compose.onNodeWithContentDescription("뒤로 가기").performClick()
        } finally {
            compose.runOnIdle { model.cancelAnalysis() }
            fixture.delete()
        }
    }

}
