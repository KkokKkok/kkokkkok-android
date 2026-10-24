package com.kkokkkok.android.analysis

// Temporary app-internal models. These do not define a backend API contract.
enum class AnalysisStep {
    METADATA, TEXT, SIGNS, BUILDINGS,
}
enum class ClueKind { TEXT, BUILDING, METADATA }
enum class ClueRisk { HIGH, CAUTION, LOW }

/** Coordinates relative to the whole input image, independent of display size/crop. */
data class NormalizedRegion(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(left in 0f..1f && right in 0f..1f && left < right)
        require(top in 0f..1f && bottom in 0f..1f && top < bottom)
    }
}

data class AnalysisClue(
    val id: String,
    val title: String,
    val description: String,
    val kind: ClueKind,
    val risk: ClueRisk,
    val region: NormalizedRegion? = null,
)

data class AnalysisResult(val score: Int, val clues: List<AnalysisClue>, val isMock: Boolean) {
    init { require(score in 0..100) }
}

sealed interface AnalysisState {
    data object Idle : AnalysisState
    data class Running(val step: AnalysisStep, val percent: Int) : AnalysisState
    data class Completed(val result: AnalysisResult) : AnalysisState
    data object Failed : AnalysisState
}
