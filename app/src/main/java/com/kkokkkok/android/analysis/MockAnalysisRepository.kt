package com.kkokkkok.android.analysis

import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Simulates progress only. Never reads image bytes, EXIF, location, or past posts. */
class MockAnalysisRepository(private val stepDelayMillis: Long = 700) : AnalysisRepository {
    init { require(stepDelayMillis >= 0) }

    override fun analyze(photo: Uri): Flow<AnalysisState> = flow {
        AnalysisStep.entries.forEachIndexed { index, step ->
            emit(AnalysisState.Running(step, index * 100 / AnalysisStep.entries.size))
            delay(stepDelayMillis)
        }
        // Fixed synthetic examples, unrelated to the chosen image's actual contents.
        emit(AnalysisState.Completed(AnalysisResult(
            score = 72,
            isMock = true,
            clues = listOf(
                AnalysisClue(
                    "mock-text", "예시 간판 단서", "모의 예시입니다. 실제 사진 속 글자를 읽은 결과가 아니에요.",
                    ClueKind.TEXT, ClueRisk.HIGH,
                    NormalizedRegion(.28f, .34f, .52f, .46f),
                ),
                AnalysisClue(
                    "mock-building", "예시 건물 단서", "모의 예시입니다. 실제 건물이나 위치를 찾지 않았어요.",
                    ClueKind.BUILDING, ClueRisk.CAUTION,
                    NormalizedRegion(.12f, .08f, .66f, .62f),
                ),
                AnalysisClue(
                    "mock-metadata", "예시 위치 정보", "모의 예시입니다. 사진의 GPS 위치 정보를 분석하지 않았어요.",
                    ClueKind.METADATA, ClueRisk.HIGH,
                ),
            ),
        )))
    }
}
