package com.kkokkkok.android

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.analysis.*
import com.kkokkkok.android.history.*
import com.kkokkkok.android.ui.analysis.AnalysisFlowScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FinishInspectionTest {
    @get:Rule val compose=createComposeRule()
    private fun photo():File {
        val file=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"synthetic-finish.png")
        val bitmap=Bitmap.createBitmap(40,60,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        return file
    }
    @Test fun finishFromStoredResultGoesHomeAndKeepsRecordAvailable() {
        val file=photo()
        val record=HistoryRecord("synthetic",HistoryKind.PHOTO,"오늘","합성 검사","합성 설명",0,10,risk=HistoryRisk.LOW,
            photoUri=Uri.fromFile(file),result=AnalysisResult(10,emptyList(),false,inspectionId="synthetic"))
        var stored = listOf(record)
        val model=HistoryViewModel(object:HistoryRepository {
            override suspend fun getRecords(kind:HistoryKind)=stored
        })
        try {
            compose.setContent { KkokKkokApp(history=model) }
            compose.onNodeWithText("검사 기록").performClick()
            compose.onNodeWithContentDescription("합성 검사 기록").performClick()
            compose.runOnIdle { stored = listOf(record.copy(title="보관된 합성 검사")) }
            compose.onNodeWithText("수정 없이 마치기").performClick()
            compose.onNodeWithText("올리기 전에, 꼭꼭 확인해요!").assertIsDisplayed()
            assertTrue(file.exists())
            compose.onNodeWithText("검사 기록").performClick()
            compose.onNodeWithContentDescription("보관된 합성 검사 기록").assertIsDisplayed().performClick()
            compose.onNodeWithText("수정 없이 마치기").assertIsDisplayed()
            compose.onNodeWithText("그래도 저장").assertDoesNotExist()
        } finally { file.delete() }
    }
    @Test fun finishIsWiredFromSecondPhotoOfBatchWithoutSaveDialog() {
        val file=photo();var finished by mutableStateOf(false);var page by mutableIntStateOf(0)
        val result=AnalysisResult(80,emptyList(),false,inspectionId="synthetic")
        try {
            compose.setContent {
                if(finished) com.kkokkkok.android.ui.design.KkokText("홈 도착")
                else AnalysisFlowScreen(Uri.fromFile(file),AnalysisState.Completed(result),{}, {finished=true}, {}, {},
                    batchStates=listOf(AnalysisState.Completed(result),AnalysisState.Completed(result)),
                    batchPhotos=listOf(Uri.fromFile(file),Uri.fromFile(file)),batchIndex=page,onBatchPage={page=it})
            }
            compose.onNodeWithText("다음 사진").performClick()
            compose.onNodeWithText("사진 2 / 2").assertIsDisplayed()
            compose.onNodeWithText("수정 없이 마치기").performClick()
            compose.onNodeWithText("홈 도착").assertIsDisplayed()
            compose.onNodeWithText("그래도 저장").assertDoesNotExist()
            assertTrue(file.exists())
        } finally { file.delete() }
    }
}
