package com.kkokkkok.android.ui.history

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import com.kkokkkok.android.analysis.AnalysisClue
import com.kkokkkok.android.analysis.AnalysisResult
import com.kkokkkok.android.analysis.ClueKind
import com.kkokkkok.android.analysis.ClueRisk
import com.kkokkkok.android.analysis.NormalizedRegion
import com.kkokkkok.android.history.HistoryRecord
import com.kkokkkok.android.ui.analysis.AnalysisResultScreen
import com.kkokkkok.android.ui.analysis.PhotoImage
import com.kkokkkok.android.ui.analysis.rememberPhotoImage
import com.kkokkkok.android.ui.analysis.AnalysisFrame
import com.kkokkkok.android.ui.analysis.AnalysisButton
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

/** Shows the existing result layout with synthetic artwork and local example content. */
@Composable
internal fun HistoryRecordResultScreen(record: HistoryRecord, onProtect: () -> Unit = {}, onCumulativeProtect: (() -> Unit)? = null,
    preparingProtection: Boolean = false, protectionMessage: String? = null, onFinish: (() -> Unit)? = null, onBack: () -> Unit) {
    if (record.cumulativeResult != null) {
        com.kkokkkok.android.ui.snscheck.CumulativeResultScreen(record.cumulativeResult, record.cumulativePlace, onBack, onBack, onBack,
            onProtect = onCumulativeProtect, preparingProtection = preparingProtection, protectionMessage = protectionMessage)
        return
    }
    val uri = record.photoUri
    if (uri != null && record.result != null) {
        when (val image = rememberPhotoImage(uri, 0)) {
            is PhotoImage.Loaded -> AnalysisResultScreen(image, record.result, onBack, onProtect, onFinish = onFinish ?: onBack)
            PhotoImage.Loading -> HistoryDetailMessage(record, onBack, "사진을 불러오고 있어요…")
            PhotoImage.Failed -> HistoryDetailMessage(record, onBack, "사진을 불러오지 못했어요")
        }
    } else if (record.isMock) {
        MockHistoryResultScreen(record, onBack, onFinish ?: onBack)
    } else {
        val message = when (record.inspectionStatus) {
            "not_started" -> "이 사진은 아직 검사하지 않았어요."
            "queued", "processing", "submitting" -> "검사를 처리하고 있어요. 잠시 후 기록을 새로 불러와 주세요."
            else -> "검사 결과를 불러오지 못했어요."
        }
        HistoryDetailMessage(record, onBack, message)
    }
}

@Composable
private fun HistoryDetailMessage(record: HistoryRecord, onBack: () -> Unit, message: String) {
    AnalysisFrame("검사 기록", onBack, showMockLabel = record.result?.isMock == true,
        footer = { AnalysisButton("확인", primary = true, onClick = onBack) }) {
        val uri = record.photoUri
        if (uri != null) {
            when (val photo = rememberPhotoImage(uri, 0)) {
                is PhotoImage.Loaded -> Image(photo.bitmap, null,
                    Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit)
                PhotoImage.Loading, PhotoImage.Failed -> Unit
            }
        }
        Column(Modifier.fillMaxWidth().background(KkokColors.White, RoundedCornerShape(24.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KkokText(record.title, style = KkokTypography.TitleLarge)
            KkokText(record.detail, style = KkokTypography.LabelMedium, color = KkokColors.Gray500)
            KkokText(message, style = KkokTypography.BodyMedium, color = KkokColors.Gray600)
        }
    }
}

@Composable
private fun MockHistoryResultScreen(record: HistoryRecord, onBack: () -> Unit, onFinish: () -> Unit) {
    val photo = remember(record.id) { PhotoImage.Loaded(createSyntheticImage().asImageBitmap()) }
    val clues = remember(record.id, record.clueCount) { syntheticClues.take(record.clueCount.coerceIn(1, syntheticClues.size)) }
    val result = remember(record.id, record.score, record.protectedScore, clues) {
        AnalysisResult(
            score = if (record.protected) record.protectedScore ?: record.score else record.score,
            clues = clues,
            isMock = true,
        )
    }
    AnalysisResultScreen(photo, result, onBack, onFinish = onFinish)
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
