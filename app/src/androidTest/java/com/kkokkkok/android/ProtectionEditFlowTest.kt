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
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class ProtectionEditFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun backendVerificationAcceptsProcessingThenCompletedAndRejectsMockAssessment() {
        val server = java.net.ServerSocket(0, 3, java.net.InetAddress.getByName("127.0.0.1"))
        server.soTimeout = 10_000
        val bodies = listOf(
            """{"status":"processing","mock":false}""",
            """{"inspectionId":"synthetic","status":"completed","mock":false,"riskSummary":{"score":null},"riskFactors":[]}""",
            """{"status":"completed","mock":true,"riskSummary":{"score":99},"riskFactors":[]}""",
        )
        val worker = Thread {
            try {
                for (body in bodies) server.accept().use { socket ->
                    val input = socket.getInputStream().bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) { /* consume headers */ }
                    val bytes = body.toByteArray()
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray() + bytes)
                }
            } finally { server.close() }
        }
        worker.start()
        val repository = com.kkokkkok.android.protection.BackendProtectionEditRepository(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext, "http://127.0.0.1:${server.localPort}")
        val initial = com.kkokkkok.android.protection.ProtectionPreview(Uri.parse("file:///synthetic.png"), false, null, "synthetic", "queued", null, null, "synthetic")
        try {
            kotlinx.coroutines.runBlocking {
                val processing = repository.refreshVerification(initial)
                assertEquals("processing", processing.verificationStatus)
                assertEquals(null, processing.verificationCueCount)
                val completed = repository.refreshVerification(processing)
                assertEquals("completed", completed.verificationStatus)
                assertEquals(0, completed.verificationCueCount)
                val mock = repository.refreshVerification(completed)
                assertEquals("mock", mock.verificationStatus)
                assertEquals(null, mock.verificationScore)
            }
        } finally { server.close(); worker.join(2_000) }
    }

    @Test
    fun verificationUpdatesWithoutCreatingAnotherEditAndStopsOnLeave() {
        val waiting = java.util.concurrent.atomic.AtomicBoolean(false)
        val hold = java.util.concurrent.atomic.AtomicBoolean(false)
        val finish = kotlinx.coroutines.CompletableDeferred<com.kkokkkok.android.protection.ProtectionPreview>()
        val creates = java.util.concurrent.atomic.AtomicInteger(0)
        val initial = com.kkokkkok.android.protection.ProtectionPreview(Uri.parse("file:///synthetic.png"), false, null, "synthetic", "processing", null, null, "verify-synthetic")
        val repository = object : com.kkokkkok.android.protection.ProtectionEditRepository {
            override suspend fun createEdit(inspectionId: String, cueIds: List<String>, preservePrompt: String, editStrength: Int, idempotencyKey: String, sourcePhoto: Uri): com.kkokkkok.android.protection.ProtectionPreview {
                assertEquals("", preservePrompt)
                creates.incrementAndGet()
                return initial
            }
            override suspend fun refreshVerification(preview: com.kkokkkok.android.protection.ProtectionPreview): com.kkokkkok.android.protection.ProtectionPreview {
                if (hold.get()) { waiting.set(true); return finish.await() }
                return preview.copy(verificationStatus = "completed", verificationCueCount = 1)
            }
        }
        val edit = ProtectionEditViewModel(repository)
        fun begin() {
            edit.begin(Uri.parse("file:///synthetic.png"), AnalysisResult(null,
                listOf(AnalysisClue("cue", "합성 간판", "합성 설명", ClueKind.TEXT, ClueRisk.CAUTION, NormalizedRegion(.1f, .1f, .5f, .4f))), false, inspectionId = "synthetic"))
            edit.createPreview()
        }
        compose.runOnIdle { begin() }
        compose.waitUntil(6_000) { edit.state.value.preview?.verificationStatus == "completed" }
        assertEquals(1, creates.get())
        compose.runOnIdle { hold.set(true); begin() }
        compose.waitUntil(6_000) { waiting.get() }
        compose.runOnIdle { edit.leave(); finish.complete(initial.copy(verificationStatus = "completed")) }
        compose.waitForIdle()
        assertEquals(null, edit.state.value.photo)
        assertEquals(null, edit.state.value.preview)
        assertEquals(2, creates.get())
    }

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

        compose.onNodeWithText("창밖 간판").assertIsDisplayed()
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
