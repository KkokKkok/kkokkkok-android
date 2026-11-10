package com.kkokkkok.android

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.analysis.*
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import com.kkokkkok.android.ui.analysis.AnalysisResultScreen
import com.kkokkkok.android.ui.analysis.PhotoImage
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
    private val lifecycleOwner = ResumedAnalysisLifecycleOwner()

    @Test
    fun partialAnalysisResultShowsNoticeAndAvailableClues() {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(45, 180, 120))
        val result = AnalysisResult(
            score = null,
            clues = listOf(AnalysisClue("cue-1", "간판 글자", "문자를 읽을 수 있어요.", ClueKind.TEXT, ClueRisk.CAUTION)),
            isMock = false,
            isPartial = true,
        )
        try {
            compose.setContent {
                AnalysisResultScreen(PhotoImage.Loaded(bitmap.asImageBitmap()), result, onBack = {}, onProtect = {})
            }

            compose.onNodeWithText("일부 분석을 마치지 못했어요. 아래에는 확인된 단서만 표시해요.").assertIsDisplayed()
            compose.onNodeWithText("미산정").assertIsDisplayed()
            val save = compose.onNodeWithText("수정 없이 마치기").fetchSemanticsNode().boundsInRoot
            val edit = compose.onNodeWithText("위험 요소 수정").fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertEquals(save.height, edit.height, 2f)
            org.junit.Assert.assertEquals(save.top, edit.top, 2f)
            compose.onNodeWithText("간판 글자").performScrollTo().assertIsDisplayed()
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun unassessedPipelineResultShowsCluesWithoutInventingRisk() {
        val fixture = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "synthetic-unassessed.png")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(45, 180, 120))
        fixture.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val result = AnalysisResult(
            score = null,
            clues = listOf(
                AnalysisClue(
                    "cue-1", "간판 글자", "사진에서 문자가 확인됐어요.", ClueKind.TEXT,
                    risk = ClueRisk.CAUTION, region = NormalizedRegion(.1f, .2f, .6f, .7f),
                ),
                AnalysisClue(
                    "cue-2", "일반적인 창문", "위치 구분에 도움이 적은 단서예요.", ClueKind.BUILDING,
                    risk = ClueRisk.LOW, region = NormalizedRegion(.6f, .2f, .9f, .7f),
                ),
            ),
            isMock = false,
        )
        val repository = object : AnalysisRepository {
            override suspend fun hasExifGps(photo: Uri) = false
            override fun analyze(photo: Uri, location: String) = flow {
                emit(AnalysisState.Completed(result))
            }
        }
        val model = PhotoSelectionViewModel(repository)
        try {
            compose.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) { KkokKkokApp(model) }
            }
            compose.runOnIdle { model.onPhotoPicked(Uri.fromFile(fixture)) }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("촬영 위치 입력").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("photo-location-field").performTextInput("서울 예시 위치")
            compose.onNodeWithTag("photo-location-submit").performClick()
            compose.waitUntil(5_000) { model.analysisState.value is AnalysisState.Completed }

            compose.onNodeWithText("미산정").assertIsDisplayed()
            compose.onNodeWithText("위험 점수 기준이 아직 정해지지 않았어요. 0점이나 낮은 위험을 뜻하지 않아요.").assertIsDisplayed()
            compose.onNodeWithText("발견한 단서 1개").assertIsDisplayed()
            compose.onNodeWithText("간판 글자").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("일반적인 창문").assertDoesNotExist()
            compose.onNodeWithContentDescription("선택한 사진 · 분석에서 찾은 단서 영역").assertIsDisplayed()
            compose.onAllNodesWithText("모의").assertCountEquals(0)
        } finally {
            compose.runOnIdle { model.cancelAnalysis() }
            fixture.delete()
        }
    }

    @Test
    fun selectedPhotoCanCancelThenFinishMockResultAndReturnHome() {
        val fixture = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "synthetic-flow.png")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(45, 180, 120))
        fixture.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val finish = CompletableDeferred<Unit>()
        val model = PhotoSelectionViewModel(object : AnalysisRepository {
            override suspend fun hasExifGps(photo: Uri) = false
            override fun analyze(photo: Uri, location: String) = flow {
                emit(AnalysisState.Running(AnalysisStep.TEXT, 25))
                finish.await()
                emit(MockAnalysisRepository(0).analyze(photo, location).last())
            }
        })
        try {
            compose.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) { KkokKkokApp(model) }
            }
            compose.runOnIdle { model.onPhotoPicked(Uri.fromFile(fixture)) }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("촬영 위치 입력").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("촬영 위치 입력").assertIsDisplayed()
            compose.onNodeWithTag("photo-location-field").performTextInput("서울 테스트 카페")
            compose.onNodeWithTag("photo-location-submit").performClick()
            compose.onNodeWithText("검사 취소").performClick()
            compose.runOnIdle {
                assertEquals(AnalysisState.Idle, model.analysisState.value)
                assertEquals(Uri.fromFile(fixture), model.selectedPhoto.value)
            }
            compose.onNodeWithText("위치 노출 검사").performScrollTo().assertIsDisplayed()
            compose.runOnIdle { model.onPhotoPicked(Uri.fromFile(fixture)) }
            compose.waitUntil(5_000) { model.needsLocationInput.value }
            compose.onNodeWithText("촬영 위치 입력").assertIsDisplayed()
            compose.onNodeWithTag("photo-location-field").performTextInput("서울 테스트 카페")
            compose.onNodeWithTag("photo-location-submit").performClick()
            compose.onNodeWithText("사진을 살펴보고 있어요").assertIsDisplayed()
            finish.complete(Unit)
            compose.waitUntil(5_000) { model.analysisState.value is AnalysisState.Completed }
            compose.onNodeWithText("모의 단서 3개를 표시했어요").assertIsDisplayed()
            compose.onNodeWithText("위험 요소 수정").performClick()
            compose.onNodeWithText("어디에서 찍은 사진인가요?").assertIsDisplayed()
            compose.onNodeWithContentDescription("뒤로").performClick()
            compose.onNodeWithText("모의 단서 3개를 표시했어요").assertIsDisplayed()
            compose.onNodeWithText("수정 없이 마치기").performClick()
            compose.onNodeWithText("올리기 전에, 꼭꼭 확인해요!").assertIsDisplayed()
            assertEquals(null, model.selectedPhoto.value)
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
            compose.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) { KkokKkokApp(model) }
            }
            compose.runOnIdle { model.onPhotoPicked(Uri.fromFile(fixture)) }
            compose.waitUntil(5_000) { model.needsLocationInput.value }
            compose.runOnIdle {
                model.updateLocation("서울 예시 위치")
                model.startAnalysis()
            }
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

private class ResumedAnalysisLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    override val lifecycle: Lifecycle get() = registry
}
