package com.kkokkkok.android.analysis

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisResultParserTest {
    @Test
    fun keepsUnassessedScoreAndClueRiskUnsetWhileReadingBbox() {
        val response = JSONObject()
            .put("mock", false)
            .put("riskSummary", JSONObject().put("score", JSONObject.NULL).put("level", "pending"))
            .put("riskFactors", JSONArray().put(
                JSONObject()
                    .put("id", "cue-1")
                    .put("type", "sign_text")
                    .put("title", "간판 글자")
                    .put("description", "문자를 읽을 수 있어요.")
                    .put("modelRisk", JSONObject.NULL)
                    .put("humanRisk", JSONObject.NULL)
                    .put("bbox", JSONArray(listOf(0.1, 0.2, 0.5, 0.6)))
                    .put("coordinateSpace", "analysis-normalized"),
            ))

        val result = parseAnalysisResult(response)

        assertNull(result.score)
        assertFalse(result.isMock)
        assertEquals(1, result.clues.size)
        assertNull(result.clues.single().risk)
        assertEquals(NormalizedRegion(0.1f, 0.2f, 0.5f, 0.6f), result.clues.single().region)
    }

    @Test
    fun keepsExistingScoreAndUsesHumanReviewedRiskWhenPresent() {
        val response = JSONObject()
            .put("mock", false)
            .put("riskSummary", JSONObject().put("score", 72).put("level", "high"))
            .put("riskFactors", JSONArray().put(
                JSONObject().put("type", "sign_text").put("modelRisk", "caution").put("humanRisk", "high"),
            ))

        val result = parseAnalysisResult(response)

        assertEquals(72, result.score)
        assertEquals(ClueRisk.HIGH, result.clues.single().risk)
    }

    @Test
    fun showsObservationRegionAndModelReasonFromAnalysisServer() {
        val response = JSONObject()
            .put("mock", false)
            .put("riskSummary", JSONObject().put("score", JSONObject.NULL))
            .put("riskFactors", JSONArray().put(
                JSONObject()
                    .put("title", "간판")
                    .put("description", "CAFE PINE 글자가 보입니다.")
                    .put("detail", "사진 오른쪽 위")
                    .put("modelReason", "읽을 수 있는 상호가 있습니다.")
                    .put("modelRisk", "medium")
                    .put("bbox", JSONObject.NULL),
            ))

        val clue = parseAnalysisResult(response).clues.single()

        assertEquals("CAFE PINE 글자가 보입니다.\n영역: 사진 오른쪽 위\n판단 근거: 읽을 수 있는 상호가 있습니다.", clue.description)
        assertEquals(ClueRisk.CAUTION, clue.risk)
        assertNull(clue.region)
    }

    @Test
    fun keepsCluesFromFailedInspectionAsPartialButKeepsEmptyFailureFailed() {
        val partialResponse = JSONObject()
            .put("status", "failed")
            .put("riskFactors", JSONArray().put(JSONObject().put("title", "간판 글자")))

        val partial = parseInspectionState(partialResponse) as AnalysisState.Completed

        assertTrue(partial.result.isPartial)
        assertNull(partial.result.score)
        assertEquals("간판 글자", partial.result.clues.single().title)
        assertEquals(AnalysisState.Failed, parseInspectionState(JSONObject().put("status", "failed")))
    }
}
