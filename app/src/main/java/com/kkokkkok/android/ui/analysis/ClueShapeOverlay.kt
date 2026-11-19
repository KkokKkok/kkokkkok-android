package com.kkokkkok.android.ui.analysis

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kkokkkok.android.analysis.ClueShape
import kotlin.math.roundToInt

/**
 * Draws a clue's exact area over the photo: an object's mask and a text line both as an outline with a light fill.
 * [modifier] must give this the displayed image's own bounds, since shapes are fractions of the image.
 */
@Composable
internal fun ClueShapeOverlay(
    shapes: List<ClueShape>,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.5.dp,
) {
    val masks = remember(shapes, color) {
        val tint = color.copy(alpha = MASK_FILL_ALPHA).toArgb()
        shapes.filterIsInstance<ClueShape.Mask>().map { mask ->
            Bitmap.createBitmap(
                mask.toPixels(tint, color.toArgb(), MASK_EDGE_CELLS), mask.width, mask.height, Bitmap.Config.ARGB_8888,
            ).asImageBitmap()
        }
    }
    val outlines = remember(shapes) { shapes.filterIsInstance<ClueShape.Outline>() }
    Canvas(modifier) {
        val target = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
        // The mask is smaller than the photo on screen; smoothing keeps its edge from looking blocky.
        masks.forEach { drawImage(it, dstSize = target, filterQuality = FilterQuality.Low) }
        if (outlines.isNotEmpty()) {
            // One path for all lines: OCR often reads the same line twice a pixel apart, and separate
            // fills would pile up into an opaque band over the very text being pointed at.
            val path = Path().apply {
                outlines.forEach { outline ->
                    outline.points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x * size.width, point.y * size.height)
                        else lineTo(point.x * size.width, point.y * size.height)
                    }
                    close()
                }
            }
            drawPath(path, color.copy(alpha = OUTLINE_FILL_ALPHA))
            drawPath(path, color, style = Stroke(strokeWidth.toPx(), join = StrokeJoin.Round))
        }
    }
}

// An object is shown by its outline; the inside stays 85% see-through so the object itself is visible.
private const val MASK_FILL_ALPHA = .15f
// The mask grid is coarser than the screen (long side 512): 2 cells come out about as wide as a text outline.
private const val MASK_EDGE_CELLS = 2
// Text lines are small on screen: a light fill and a thin line keep the text readable.
private const val OUTLINE_FILL_ALPHA = .1f
