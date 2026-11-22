package com.kkokkkok.android.analysis

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic analysis responses only. */
class ResultDisplayParseTest {
    @Test fun literalNullDescriptionsAreNotShown() {
        val result = parseAnalysisResult(JSONObject("""{"status":"completed","riskSummary":{"score":null},
            "riskFactors":[{"id":"c1","type":"text","title":"합성 기관명","modelRisk":"high",
              "description":"건물에 합성 기관명이 보임","detail":"null","modelReason":"NULL",
              "bbox":[0.1,0.1,0.5,0.2],"coordinateSpace":"analysis-normalized"}]}"""))
        val description = result.clues.single().description
        assertEquals("건물에 합성 기관명이 보임", description)
        assertFalse(description.contains("null", ignoreCase = true))
    }

    @Test fun realDescriptionsStillAppear() {
        val result = parseAnalysisResult(JSONObject("""{"status":"completed","riskSummary":{"score":null},
            "riskFactors":[{"id":"c1","type":"text","modelRisk":"low","description":"합성 설명",
              "detail":"사진 위쪽 간판","modelReason":"합성 근거"}]}"""))
        val description = result.clues.single().description
        assertTrue(description.contains("영역: 사진 위쪽 간판"))
        assertTrue(description.contains("판단 근거: 합성 근거"))
    }

    @Test fun batchWaitGrowsWithPhotoCount() {
        assertEquals(1_500, batchPollBudget(photoCount = 1, singlePhotoPolls = 1_500))
        assertEquals(1_500, batchPollBudget(photoCount = 2, singlePhotoPolls = 1_500))
        assertEquals(12_000, batchPollBudget(photoCount = 20, singlePhotoPolls = 1_500))
    }
}
