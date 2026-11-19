package com.kkokkkok.android.analysis

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClueShapeTest {
    @Test
    fun maskRunsAlternateFromUnsetAndAreReadRowByRow() {
        // 4 wide, 2 high:  . . X X
        //                  X . . .
        val mask = ClueShape.Mask(width = 4, height = 2, runs = listOf(2, 3, 3))
        assertArrayEquals(intArrayOf(0, 0, 7, 7, 7, 0, 0, 0), mask.toPixels(7))
    }

    @Test
    fun maskStartingSetHasAnEmptyFirstRun() {
        val mask = ClueShape.Mask(width = 3, height = 1, runs = listOf(0, 2, 1))
        assertArrayEquals(intArrayOf(9, 9, 0), mask.toPixels(9))
    }

    @Test
    fun maskRimIsDrawnInTheEdgeColorAndOnlyTheInsideInTheFill() {
        // 5 x 5 with a 3 x 3 block in the middle: its 8 outer cells are the rim, the centre the inside.
        val mask = ClueShape.Mask(5, 5, listOf(6, 3, 2, 3, 2, 3, 6))
        val e = 2
        val f = 1
        assertArrayEquals(
            intArrayOf(
                0, 0, 0, 0, 0,
                0, e, e, e, 0,
                0, e, f, e, 0,
                0, e, e, e, 0,
                0, 0, 0, 0, 0,
            ),
            mask.toPixels(fill = f, edge = e),
        )
        // A mask reaching the photo's edge is closed there too.
        assertArrayEquals(intArrayOf(e, e, e), ClueShape.Mask(3, 1, listOf(0, 3)).toPixels(f, e))
    }

    @Test
    fun maskWhoseRunsDoNotFillItIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { ClueShape.Mask(4, 2, listOf(2, 3)) }
        assertThrows(IllegalArgumentException::class.java) { ClueShape.Mask(4, 2, listOf(9, -1)) }
    }

    @Test
    fun outlineNeedsAtLeastThreePoints() {
        assertThrows(IllegalArgumentException::class.java) {
            ClueShape.Outline(listOf(NormalizedPoint(0f, 0f), NormalizedPoint(1f, 1f)))
        }
    }
}
