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
import com.kkokkkok.android.snscheck.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred

@RunWith(AndroidJUnit4::class)
class CumulativeProtectionTest {
    @get:Rule val compose = createComposeRule()
    private fun analysis(id: String) = AnalysisResult(60,listOf(AnalysisClue(id,"합성 단서 $id","합성 설명",
        ClueKind.TEXT,ClueRisk.CAUTION,NormalizedRegion(.1f,.1f,.6f,.6f))),false,inspectionId=id)

    @Test fun resultOpensExistingInspectionsAndEditsTwoPhotosInOriginalOrder() {
        val file=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"synthetic-cumulative-edit.png")
        val bitmap=Bitmap.createBitmap(40,60,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        val photo=Uri.fromFile(file)
        val sources=listOf(ProtectionSource("a",photo,analysis("a")),ProtectionSource("b",photo,analysis("b")))
        val repository=FakeCumulative(sources)
        val cumulative=SnsCumulativeViewModel(repository)
        val requests=mutableListOf<String>()
        val edit=ProtectionEditViewModel(object:ProtectionEditRepository {
            override suspend fun createEdit(inspectionId:String,cueIds:List<String>,preservePrompt:String,editStrength:Int,
                idempotencyKey:String,sourcePhoto:Uri):ProtectionPreview {
                assertEquals(listOf(inspectionId),cueIds);assertEquals(photo,sourcePhoto)
                requests.add(inspectionId)
                return ProtectionPreview(photo,false,null,"synthetic","completed",10,0)
            }
        })
        try {
            compose.setContent { KkokKkokApp(snsCumulative=cumulative,protection=edit) }
            compose.onNodeWithText("SNS 누적 검사").performScrollTo().performClick()
            compose.onNodeWithText("시작하기").performClick()
            compose.onNodeWithText("다음 · 게시물 올리기").performClick()
            compose.onNodeWithText("2개 누적 분석하기").performClick()
            compose.onNodeWithText("이 게시물들 보호 편집하기").performScrollTo().performClick()
            compose.onNodeWithText("사진 1 / 2").assertIsDisplayed()
            compose.onNodeWithText("합성 단서 a").assertIsDisplayed()
            assertTrue(requests.isEmpty())
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithText("누적 노출 결과").assertIsDisplayed()
            compose.onNodeWithText("이 게시물들 보호 편집하기").performScrollTo().performClick()
            compose.onNodeWithText("다음 · 요청 입력").performClick()
            compose.onNodeWithText("보호 이미지 만들기").performClick()
            compose.onNodeWithText("다음 사진 편집").assertIsDisplayed().performClick()
            compose.onNodeWithText("사진 2 / 2").assertIsDisplayed()
            compose.onNodeWithText("합성 단서 b").assertIsDisplayed()
            assertEquals("",edit.state.value.request)
            assertEquals(listOf("a"),requests)
            compose.onNodeWithText("다음 · 요청 입력").performClick()
            compose.onNodeWithText("보호 이미지 만들기").performClick()
            compose.onNodeWithText("누적 결과로 돌아가기").performClick()
            compose.onNodeWithText("누적 노출 결과").assertIsDisplayed()
            assertEquals(listOf("a","b"),requests)
        } finally { file.delete() }
    }

    @Test fun preparationCanRetrySkipUnavailablePhotoAndCancelLateResult() {
        val source=ProtectionSource("a",Uri.parse("file:///synthetic.png"),analysis("a"))
        val repository=FakeCumulative(listOf(source))
        val model=SnsCumulativeViewModel(repository)
        val calls=AtomicInteger()
        compose.setContent { com.kkokkkok.android.ui.design.KkokText("합성 검증") }
        compose.runOnIdle { repository.fail=true; model.prepareProtection(repository.result) { calls.incrementAndGet() } }
        compose.waitUntil(5_000) { !model.state.value.preparingProtection }
        assertNotNull(model.state.value.protectionMessage);assertEquals(0,calls.get())
        compose.runOnIdle { repository.fail=false; model.prepareProtection(repository.result) { assertEquals(listOf("a"),it.map { p -> p.assetId });calls.incrementAndGet() } }
        compose.waitUntil(5_000) { calls.get()==1 }
        assertTrue(model.state.value.protectionMessage.orEmpty().contains("2장 중 편집 가능한 1장"))
        val gate=CompletableDeferred<Unit>()
        compose.runOnIdle { repository.gate=gate; model.prepareProtection(repository.result) { calls.incrementAndGet() } }
        compose.waitUntil(5_000) { model.state.value.preparingProtection }
        compose.runOnIdle { model.cancelProtectionPreparation();gate.complete(Unit) }
        compose.waitForIdle();assertEquals(1,calls.get())
    }

    private class FakeCumulative(val sources:List<ProtectionSource>):SnsCumulativeRepository {
        var fail=false;var gate:CompletableDeferred<Unit>?=null
        val posts=listOf(SnsPostPreview("a",title="합성 A",locationStatus="available"),SnsPostPreview("b",title="합성 B",locationStatus="available"))
        val result=SnsExposureResult(60,"주의",2,listOf(SnsExposureCue("group","합성 단서",2)),posts,false,"synthetic","calculated","합성 테스트")
        override suspend fun getPlaceOptions()=listOf(SnsPlaceOption("place","합성 장소","반경300m","합성",SnsPlaceKind.HOME))
        override suspend fun getPostPreviews(placeId:String)=posts
        override suspend fun getLinkedAssetIds(placeId:String)=setOf("a","b")
        override suspend fun getExposureResult(placeId:String,selectedPostIds:Set<String>,manualLocations:Map<String,Pair<String,String>>)=result
        override suspend fun getProtectionSources(posts:List<SnsPostPreview>):List<ProtectionSource> {
            gate?.await()
            if(fail) throw java.io.IOException("synthetic failure")
            return sources.reversed() // The app must follow the result's photo order.
        }
    }
}
