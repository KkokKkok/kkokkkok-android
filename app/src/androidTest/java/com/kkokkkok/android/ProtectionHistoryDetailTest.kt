package com.kkokkkok.android

import android.graphics.Bitmap
import android.net.Uri
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.analysis.*
import com.kkokkkok.android.protection.*
import com.kkokkkok.android.protectionhistory.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProtectionHistoryDetailTest {
    @get:Rule val compose = createComposeRule()
    @Test fun savedRecordOpensWithoutEditingAndReeditReturnsToSavedDetail() {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"synthetic-history-detail.png")
        val bitmap = Bitmap.createBitmap(40,60,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
        val photo = Uri.fromFile(file)
        val result = AnalysisResult(70,listOf(AnalysisClue("synthetic-cue","합성 단서","합성 설명",ClueKind.TEXT,
            ClueRisk.CAUTION,NormalizedRegion(.1f,.1f,.5f,.5f))),false,inspectionId="synthetic-inspection")
        val record = ProtectionRecord("synthetic-edit",70,10,"2026-01-01",0,photo,photo,false,result,
            ProtectionPreview(photo,false,null,null,"completed",10,0))
        val history = ProtectionHistoryViewModel(object : ProtectionHistoryRepository {
            override suspend fun getRecords() = listOf(record)
        })
        val editing = ProtectionEditViewModel(object : ProtectionEditRepository {
            override suspend fun createEdit(inspectionId:String,cueIds:List<String>,preservePrompt:String,editStrength:Int,
                idempotencyKey:String,sourcePhoto:Uri):ProtectionPreview = error("Opening saved history must not create an edit")
        })
        try {
            compose.setContent { KkokKkokApp(protectionHistory=history,protection=editing) }
            compose.onNodeWithText("보호 기록").performClick()
            compose.onNodeWithContentDescription("보호 기록 70에서 10, 2026-01-01").performClick()
            compose.onNodeWithText("수정 결과").assertIsDisplayed()
            compose.onNodeWithText("재검사 점수 · 10").assertIsDisplayed()
            compose.onNodeWithText("갤러리에 저장").assertIsEnabled()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithContentDescription("보호 기록 70에서 10, 2026-01-01").assertIsDisplayed().performClick()
            compose.onNodeWithText("다시 수정").performClick()
            compose.onNodeWithText("합성 단서").assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithText("수정 결과").assertIsDisplayed()
        } finally { file.delete() }
    }
}
