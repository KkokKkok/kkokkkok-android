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
