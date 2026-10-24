package com.kkokkkok.android.ui.history

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import com.kkokkkok.android.analysis.AnalysisClue
import com.kkokkkok.android.analysis.AnalysisResult
import com.kkokkkok.android.analysis.ClueKind
import com.kkokkkok.android.analysis.ClueRisk
import com.kkokkkok.android.analysis.NormalizedRegion
import com.kkokkkok.android.history.HistoryRecord
import com.kkokkkok.android.ui.analysis.AnalysisResultScreen
import com.kkokkkok.android.ui.analysis.PhotoImage

/** Shows the existing result layout with synthetic artwork and local example content. */
@Composable
internal fun HistoryMockResultScreen(record: HistoryRecord, onBack: () -> Unit) {
    val photo = remember(record.id) { PhotoImage.Loaded(createSyntheticImage().asImageBitmap()) }
    val clues = remember(record.id, record.clueCount) { syntheticClues.take(record.clueCount.coerceIn(1, syntheticClues.size)) }
    val result = remember(record.id, record.score, record.protectedScore, clues) {
        AnalysisResult(
            score = if (record.protected) record.protectedScore ?: record.score else record.score,
            clues = clues,
            isMock = true,
        )
    }
    AnalysisResultScreen(photo, result, onBack)
}

private val syntheticClues = listOf(
    AnalysisClue(
        id = "mock-window-view",
        title = "창밖 풍경",
        description = "주변 건물 형태를 예시로 표시했어요. 실제 위치 분석 결과가 아니에요.",
        kind = ClueKind.BUILDING,
        risk = ClueRisk.LOW,
        region = NormalizedRegion(.58f, .2f, .9f, .56f),
    ),
    AnalysisClue(
        id = "mock-sign",
        title = "간판처럼 보이는 영역",
        description = "글자 단서를 보여 주기 위한 모의 예시예요.",
        kind = ClueKind.TEXT,
        risk = ClueRisk.CAUTION,
        region = NormalizedRegion(.12f, .64f, .43f, .78f),
    ),
    AnalysisClue(
        id = "mock-metadata",
        title = "사진 정보",
        description = "사진에 포함될 수 있는 정보의 예시예요. 이 화면은 사진 정보를 읽지 않았어요.",
        kind = ClueKind.METADATA,
        risk = ClueRisk.LOW,
    ),
    AnalysisClue(
        id = "mock-background",
        title = "배경 단서",
        description = "분석 결과 화면 구성을 위한 합성 예시예요.",
        kind = ClueKind.BUILDING,
        risk = ClueRisk.HIGH,
        region = NormalizedRegion(.48f, .42f, .82f, .62f),
    ),
)

private fun createSyntheticImage(): Bitmap {
    val bitmap = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    fun paint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawColor(android.graphics.Color.rgb(231, 238, 232))
    canvas.drawRect(0f, 380f, 480f, 640f, paint(android.graphics.Color.rgb(178, 199, 179)))
    canvas.drawRect(42f, 76f, 218f, 438f, paint(android.graphics.Color.rgb(155, 130, 105)))
    canvas.drawRect(244f, 118f, 420f, 368f, paint(android.graphics.Color.rgb(255, 248, 237)))
    canvas.drawRect(256f, 132f, 408f, 352f, paint(android.graphics.Color.rgb(188, 208, 211)))
    canvas.drawRect(270f, 286f, 392f, 352f, paint(android.graphics.Color.rgb(108, 143, 114)))
    canvas.drawRect(300f, 206f, 338f, 352f, paint(android.graphics.Color.rgb(133, 154, 130)))
    canvas.drawRoundRect(108f, 402f, 390f, 578f, 28f, 28f, paint(android.graphics.Color.rgb(246, 239, 224)))
    canvas.drawCircle(248f, 470f, 45f, paint(android.graphics.Color.rgb(216, 174, 125)))
    return bitmap
}
