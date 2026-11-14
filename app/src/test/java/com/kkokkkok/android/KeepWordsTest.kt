package com.kkokkkok.android

import com.kkokkkok.android.ui.design.keepWords
import org.junit.Assert.assertEquals
import org.junit.Test

class KeepWordsTest {
    @Test fun joinsLettersInsideWordsOnly() {
        assertEquals("최⁠근 점⁠수", keepWords("최근 점수"))
        assertEquals("최근 점수", keepWords("최근 점수").replace("⁠", ""))
    }
}
