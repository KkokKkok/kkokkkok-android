package com.kkokkkok.android.analysis

// Temporary app-internal models. These do not define a backend API contract.
enum class AnalysisStep {
    METADATA, TEXT, SIGNS, BUILDINGS,
}
enum class ClueKind { TEXT, BUILDING, OBJECT, METADATA }
enum class ClueRisk { HIGH, CAUTION, LOW }

/** Coordinates relative to the whole input image, independent of display size/crop. */
data class NormalizedRegion(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init {
        require(left in 0f..1f && right in 0f..1f && left < right)
        require(top in 0f..1f && bottom in 0f..1f && top < bottom)
    }
}

/** A point as fractions of the whole input image. */
data class NormalizedPoint(val x: Float, val y: Float)

/**
 * The exact area of a clue inside its [AnalysisClue.region], as the analysis server found it
 * (`riskFactors[].regions[]`): text is outlined, an object is covered by its mask.
 */
sealed interface ClueShape {
    /** A text line read by OCR: the corners of the line in drawing order. */
    data class Outline(val points: List<NormalizedPoint>) : ClueShape {
        init { require(points.size >= 3) }
    }

    /**
     * An object's shape over the whole photo at reduced size. [runs] are run lengths read row by row,
     * starting with a run of unset cells (which may be empty) and alternating from there.
     */
    data class Mask(val width: Int, val height: Int, val runs: List<Int>) : ClueShape {
        init {
            require(width > 0 && height > 0 && runs.all { it >= 0 })
            require(runs.sumOf { it.toLong() } == width.toLong() * height)
        }

        /** One entry per cell, row by row: [color] where the mask is set, 0 (transparent) elsewhere. */
        fun toPixels(color: Int): IntArray {
            val pixels = IntArray(width * height)
            var position = 0
            runs.forEachIndexed { index, length ->
                if (index % 2 == 1) pixels.fill(color, position, position + length)
                position += length
            }
            return pixels
        }

        /**
         * Like [toPixels], with the rim of each set area in [edge] and its inside in [fill]. The rim is
         * the set cells within [edgeWidth] cells of an unset cell or of the photo's edge.
         */
        fun toPixels(fill: Int, edge: Int, edgeWidth: Int = 1): IntArray {
            val pixels = toPixels(fill)
            val set = BooleanArray(pixels.size)
            var position = 0
            runs.forEachIndexed { index, length ->
                if (index % 2 == 1) set.fill(true, position, position + length)
                position += length
            }
            for (y in 0 until height) for (x in 0 until width) {
                if (!set[y * width + x]) continue
                var rim = false
                var dy = -edgeWidth
                while (!rim && dy <= edgeWidth) {
                    var dx = -edgeWidth
                    while (!rim && dx <= edgeWidth) {
                        val nx = x + dx
                        val ny = y + dy
                        rim = nx < 0 || ny < 0 || nx >= width || ny >= height || !set[ny * width + nx]
                        dx++
                    }
                    dy++
                }
                if (rim) pixels[y * width + x] = edge
            }
            return pixels
        }
    }
}

data class AnalysisClue(
    val id: String,
    val title: String,
    val description: String,
    val kind: ClueKind,
    /** Null when the pipeline has not assigned a clue risk level. */
    val risk: ClueRisk?,
    val region: NormalizedRegion? = null,
    /** Empty when the server gave only the box; the box is then what is shown. */
    val shapes: List<ClueShape> = emptyList(),
)

data class AnalysisResult(
    val score: Int?,
    val clues: List<AnalysisClue>,
    val isMock: Boolean,
    val isPartial: Boolean = false,
    /** Backend inspection ID needed by follow-up protection edits; absent for local examples. */
    val inspectionId: String? = null,
) {
    init { require(score == null || score in 0..100) }
}

sealed interface AnalysisState {
    data object Idle : AnalysisState
    data class Running(val step: AnalysisStep, val percent: Int) : AnalysisState
    data class Completed(val result: AnalysisResult) : AnalysisState
    data object Failed : AnalysisState
}
