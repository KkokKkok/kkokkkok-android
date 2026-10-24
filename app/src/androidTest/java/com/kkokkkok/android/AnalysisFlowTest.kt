package com.kkokkkok.android

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
            override fun analyze(photo: Uri) = flow {
                emit(AnalysisState.Running(AnalysisStep.TEXT, 25))
                finish.await()
                emit(MockAnalysisRepository(0).analyze(photo).last())
            }
        })
        try {
            compose.setContent { KkokKkokApp(model) }
            compose.runOnIdle { model.onPhotoPicked(Uri.fromFile(fixture)); model.startAnalysis() }
            compose.onNodeWithText("검사 취소").performClick()
            compose.onNodeWithText("올리기 전에, 꼭꼭 확인해요!").assertIsDisplayed()
            compose.runOnIdle { model.startAnalysis() }
            compose.onNodeWithText("모의로 살펴보는 중이에요").assertIsDisplayed()
            finish.complete(Unit)
            compose.waitUntil(5_000) { model.analysisState.value is AnalysisState.Completed }
            compose.onNodeWithText("모의 단서 3개를 표시했어요").assertIsDisplayed()
            compose.onNodeWithText("이대로 저장").assertIsNotEnabled()
            compose.onNodeWithText("위험 요소 수정").assertIsNotEnabled()
            compose.onNodeWithContentDescription("뒤로 가기").performClick()
            compose.onNodeWithText("올리기 전에, 꼭꼭 확인해요!").assertIsDisplayed()
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
