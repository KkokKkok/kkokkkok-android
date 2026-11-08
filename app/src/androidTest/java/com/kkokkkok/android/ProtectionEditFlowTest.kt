package com.kkokkkok.android

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kkokkkok.android.protection.ProtectionEditViewModel
import com.kkokkkok.android.analysis.AnalysisClue
import com.kkokkkok.android.analysis.AnalysisResult
import com.kkokkkok.android.analysis.ClueKind
import com.kkokkkok.android.analysis.ClueRisk
import com.kkokkkok.android.analysis.NormalizedRegion
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class ProtectionEditFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun protectionEditShowsActualRequestFlowAndMarksTestEditAsMock() {
        val edit = ProtectionEditViewModel()
        compose.setContent { KkokKkokApp(PhotoSelectionViewModel(), edit) }
        compose.runOnIdle {
            edit.begin(
                Uri.parse("content://synthetic/photo"),
                AnalysisResult(
                    score = 60,
                    clues = listOf(AnalysisClue("clue-1", "창밖 간판", "버스 노선이 보여요", ClueKind.TEXT,
                        ClueRisk.CAUTION, NormalizedRegion(.1f, .1f, .5f, .4f))),
                    isMock = false,
                    inspectionId = "inspection-1",
                ),
            )
        }

        compose.onNodeWithText("어디에서 찍은 사진인가요?").assertIsDisplayed()
        compose.onNodeWithText("분석하고 수정하기").performClick()
        compose.onNodeWithText("다음 · 요청 입력").performClick()
        compose.onNodeWithText("보호 이미지 만들기").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("모의 편집입니다. 사진 내용은 실제로 바뀌지 않았어요.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("서비스에 저장됨").assertIsDisplayed()
        compose.onNodeWithText("다시 수정").performClick()
        compose.onNodeWithText("보호 이미지 만들기").assertIsDisplayed()
    }

    @Test
    fun protectionTargetsIncludeOnlyRiskyCluesWithCoordinates() {
        val edit = ProtectionEditViewModel()
        compose.setContent { KkokKkokApp(PhotoSelectionViewModel(), edit) }
        compose.runOnIdle {
            edit.begin(
                Uri.parse("content://synthetic/photo"),
                AnalysisResult(
                    score = 60,
                    clues = listOf(
                        AnalysisClue("risk-with-box", "주의 간판", "위험 단서", ClueKind.TEXT,
                            ClueRisk.CAUTION, NormalizedRegion(.1f, .1f, .5f, .4f)),
                        AnalysisClue("risk-no-box", "좌표 없는 위험 간판", "위치 없음", ClueKind.TEXT,
                            ClueRisk.HIGH, null),
                        AnalysisClue("raw-ocr", "일반 OCR 관찰", "아직 위험 판정 전", ClueKind.TEXT,
                            null, NormalizedRegion(.2f, .2f, .4f, .3f)),
                        AnalysisClue("low-box", "낮은 위험 단서", "낮음", ClueKind.TEXT,
                            ClueRisk.LOW, NormalizedRegion(.3f, .3f, .5f, .4f)),
                        AnalysisClue("metadata", "GPS 위치", "메타데이터", ClueKind.METADATA,
                            ClueRisk.HIGH, NormalizedRegion(.1f, .1f, .2f, .2f)),
                    ),
                    isMock = false,
                    inspectionId = "inspection-2",
                ),
            )
            edit.continueToTargets()
        }

        compose.onNodeWithText("주의 간판").assertIsDisplayed()
        listOf("좌표 없는 위험 간판", "일반 OCR 관찰", "낮은 위험 단서", "GPS 위치").forEach { text ->
            assertTrue(compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
        }
    }
}
