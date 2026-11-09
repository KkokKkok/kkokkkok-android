package com.kkokkkok.android

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.analysis.*
import com.kkokkkok.android.ui.analysis.PhotoImage
import com.kkokkkok.android.ui.analysis.PhotoPreview
import com.kkokkkok.android.ui.protection.CluePhotoPreview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClueOverlayGeometryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun boxesTrackTheDisplayedImageInResultsAndEditingForBothAspectRatios() {
        val sample = mutableIntStateOf(0)
        val photos = listOf(60 to 120, 120 to 60).map { (w,h) ->
            PhotoImage.Loaded(Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.WHITE)
                for(y in h/4 until h*3/4) for(x in w/4 until w*3/4) setPixel(x,y,android.graphics.Color.BLUE)
            }.asImageBitmap())
        }
        val clue = AnalysisClue("synthetic", "합성 단서", "사진 가운데 파란 사각형", ClueKind.TEXT,
            ClueRisk.HIGH, NormalizedRegion(.25f,.25f,.75f,.75f))
        compose.setContent {
            Box(Modifier.width(300.dp)) {
                if(sample.intValue<2) PhotoPreview(photos[sample.intValue],listOf(clue),isMock=false)
                else CluePhotoPreview(photos[sample.intValue-2],listOf(clue),setOf(clue.id))
            }
        }
        for(index in 0..3) {
            compose.runOnIdle { sample.intValue=index }
            compose.waitForIdle()
            val protection=index>=2
            val description=if(protection) "검사 단서가 표시된 사진" else
                InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.analysis_photo_description_live)
            val image=compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
            val box=compose.onNodeWithTag((if(protection) "protection" else "analysis")+"-clue-synthetic").fetchSemanticsNode().boundsInRoot
            // Compare actual rendered bounds to the independently marked middle half of the displayed image.
            assertEquals("case $index center x",image.center.x,box.center.x,2f)
            assertEquals("case $index center y",image.center.y,box.center.y,2f)
            assertEquals(image.width/2,box.width,2f)
            assertEquals(image.height/2,box.height,2f)
        }
    }
}
