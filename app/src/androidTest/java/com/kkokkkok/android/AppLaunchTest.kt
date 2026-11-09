package com.kkokkkok.android

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import com.kkokkkok.android.ui.account.SignupCompletionScreen
import com.kkokkkok.android.snscheck.SnsCumulativeRepository
import com.kkokkkok.android.snscheck.SnsCumulativeViewModel
import com.kkokkkok.android.snscheck.SnsPlaceOption
import com.kkokkkok.android.snscheck.SnsPlaceKind
import com.kkokkkok.android.snscheck.SnsPostPreview
import com.kkokkkok.android.snscheck.SnsExposureResult
import com.kkokkkok.android.snscheck.SnsExposureCue
import com.kkokkkok.android.ui.snscheck.SnsCumulativeFlow
import com.kkokkkok.android.location.SensitiveLocationViewModel
import com.kkokkkok.android.location.MockSensitiveLocationRepository
import com.kkokkkok.android.ui.location.SensitiveLocationFlow
import com.kkokkkok.android.history.HistoryViewModel
import com.kkokkkok.android.history.MockHistoryRepository
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun savedRealInspectionStartsAtEditableTargets() {
        val viewModel = com.kkokkkok.android.protection.ProtectionEditViewModel()
        val cue = com.kkokkkok.android.analysis.AnalysisClue("cue", "합성 간판", "합성 설명",
            com.kkokkkok.android.analysis.ClueKind.TEXT, com.kkokkkok.android.analysis.ClueRisk.CAUTION,
            com.kkokkkok.android.analysis.NormalizedRegion(.1f, .2f, .3f, .4f))
        viewModel.begin(android.net.Uri.parse("file:///synthetic.png"),
            com.kkokkkok.android.analysis.AnalysisResult(null, listOf(cue), false, inspectionId = "synthetic-inspection"))
        assertEquals(com.kkokkkok.android.protection.ProtectionStep.TARGETS, viewModel.state.value.step)
        assertEquals(setOf("cue"), viewModel.state.value.selectedTargets)
        assertEquals("", viewModel.state.value.request)
    }

    @Test
    fun launcherActivityDisplaysComposeRoot() {
        compose.onRoot().assertIsDisplayed()
    }

    @Test
    fun homeShowsDesignCopyAndSelectedHomeTab() {
        compose.onNodeWithText("올리기 전에, 꼭꼭 확인해요!").assertIsDisplayed()
        compose.onNodeWithText("1단계 · 사진을 골라주세요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("홈").assertIsSelected()
    }

    @Test
    fun snsFlowAndAccountEntryAreEnabledWhileAlertsStayDisabled() {
        compose.onNodeWithContentDescription("알림").assertIsNotEnabled()
        compose.onNodeWithText("SNS 누적 검사").assertIsEnabled().assertHasClickAction().performClick()
        compose.onNodeWithText("누적 검사").assertIsDisplayed()
        compose.onNodeWithText("서비스에 저장된 사진").assertIsDisplayed()
        compose.onNodeWithText("실제 저장 사진과 완료된 분석 기록을 사용해요. SNS 계정 연결은 제외했어요.").assertIsDisplayed()
        compose.onAllNodesWithText("인스타그램 계정 연결").assertCountEquals(0)
    }

    @Test
    fun shortcutsRemainReachableByScrolling() {
        compose.onNodeWithText("위치 노출 검사").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("SNS 누적 검사").assertIsDisplayed()
    }

    @Test
    fun photoAccessInfoCanBeDismissedWithoutOpeningPicker() {
        compose.onNodeWithText("위치 노출 검사").performScrollTo().performClick()
        compose.onNodeWithText("사진을 선택할게요").assertIsDisplayed()
        compose.onNodeWithText("시스템 사진 선택 화면에서\n검사할 사진을 최대 20장까지 선택해요.\n선택한 사진만 검사에 사용해요.").assertIsDisplayed()
        compose.onNodeWithText("나중에").performClick()
        compose.onNodeWithText("올리기 전에, 꼭꼭 확인해요!").assertIsDisplayed()
    }

    @Test
    fun cumulativeFlowStartsWithSavedPhotosAndNoSnsLogin() {
        val repository = object : SnsCumulativeRepository {
            override suspend fun getPlaceOptions() = listOf(SnsPlaceOption("place-1", "집 주변", "반경 300m", "등록한 장소", SnsPlaceKind.HOME))
            override suspend fun getPostPreviews(placeId: String) = emptyList<SnsPostPreview>()
            override suspend fun getLinkedAssetIds(placeId: String) = emptySet<String>()
            override suspend fun getExposureResult(placeId: String, selectedPostIds: Set<String>, manualLocations: Map<String, Pair<String, String>>) =
                SnsExposureResult(null, "미산정", selectedPostIds.size, emptyList(), emptyList(), false,
                    "cumulative-score-v1-provisional", "not_assessed", "테스트 결과")
        }
        compose.activity.setContent {
            SnsCumulativeFlow(remember { SnsCumulativeViewModel(repository) }, onClose = {}, onAddPlace = {}, onMessage = {})
        }
        compose.onNodeWithText("누적 검사").assertIsDisplayed()
        compose.onAllNodesWithText("인스타그램 계정 연결").assertCountEquals(0)
        compose.onNodeWithText("시작하기").performClick()
        compose.onNodeWithText("지킬 장소 고르기").assertIsDisplayed()
    }

    @Test
    fun cumulativeFlowRequestsMissingPhotoLocationThenShowsResult() {
        val submittedLocation = AtomicReference<Pair<String, String>?>(null)
        val photosLoaded = AtomicReference(false)
        val repository = object : SnsCumulativeRepository {
            override suspend fun getPlaceOptions() = listOf(SnsPlaceOption("place-1", "집 주변", "반경 300m", "등록한 장소", SnsPlaceKind.HOME))
            override suspend fun getPostPreviews(placeId: String): List<SnsPostPreview> {
                photosLoaded.set(true)
                return listOf(SnsPostPreview(id = "asset-1", locationStatus = "unavailable", title = "테스트 사진"))
            }
            override suspend fun getLinkedAssetIds(placeId: String) = emptySet<String>()
            override suspend fun getExposureResult(
                placeId: String,
                selectedPostIds: Set<String>,
                manualLocations: Map<String, Pair<String, String>>,
            ): SnsExposureResult {
                submittedLocation.set(manualLocations["asset-1"])
                return SnsExposureResult(
                    62, "주의", selectedPostIds.size,
                    listOf(SnsExposureCue("group-1", "아파트 외벽", 1, "1일 방문")),
                    listOf(SnsPostPreview(id = "asset-1", title = "테스트 사진")), false,
                    "cumulative-score-v1-provisional", "calculated", "초기 규칙 기반 상대 점수예요.",
                )
            }
        }
        compose.activity.setContent {
            SnsCumulativeFlow(remember { SnsCumulativeViewModel(repository) }, onClose = {}, onAddPlace = {}, onMessage = {})
        }
        compose.onNodeWithText("시작하기").performClick()
        compose.waitUntil(5_000) { photosLoaded.get() }
        compose.onNodeWithText("다음 · 게시물 올리기").performClick()
        compose.onNodeWithContentDescription("선택하지 않은 게시물").performClick()
        compose.onNodeWithTag("cumulative-latitude-asset-1").performTextInput("37.5665")
        compose.onNodeWithTag("cumulative-longitude-asset-1").performTextInput("126.9780")
        compose.onNodeWithText("1개 누적 분석하기").performClick()
        compose.waitUntil(5_000) { submittedLocation.get() != null }
        assertEquals("37.5665" to "126.9780", submittedLocation.get())
        compose.onNodeWithText("누적 노출 결과").assertIsDisplayed()
        compose.onNodeWithText("cumulative-score-v1-provisional", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun profileDeleteDialogCanBeCancelledWithoutChangingData() {
        compose.onNodeWithText("마이").performClick()
        compose.onNodeWithText("모든 데이터 삭제").performScrollTo().performClick()
        compose.onNodeWithTag("delete-data-dialog").assertIsDisplayed()
        compose.onNodeWithText("모의 화면 · 실제 데이터는 삭제되지 않아요").assertIsDisplayed()
        compose.onNodeWithText("모두 삭제").assertIsNotEnabled()
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithTag("delete-data-dialog").assertDoesNotExist()
        compose.onNodeWithText("모든 데이터 삭제").performScrollTo().performClick()
        compose.onNodeWithTag("delete-data-dialog").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("delete-data-dialog").assertDoesNotExist()
        compose.onNodeWithText("테스터님").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("12", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("관리 〉").performClick()
        compose.onNodeWithText("내 민감 위치").assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로 가기").performClick()
        compose.onNodeWithText("검사 이력").performScrollTo().performClick()
        assertEquals(2, compose.onAllNodesWithText("검사 기록").fetchSemanticsNodes().size)
    }

    @Test
    fun signupCompletionShowsMockTesterAndItsNavigationActions() {
        val selectedAction = AtomicReference<String?>(null)
        compose.activity.setContent {
            SignupCompletionScreen(
                onBack = { selectedAction.set("back") },
                onRegisterPlace = { selectedAction.set("place") },
                onStart = { selectedAction.set("start") },
                onLater = { selectedAction.set("later") },
            )
        }
        compose.onNodeWithTag("signup-completion").assertIsDisplayed()
        compose.onNodeWithText("반가워요, 테스터님!").assertIsDisplayed()
        compose.onNodeWithText("장소 등록하기").performClick()
        assertEquals("place", selectedAction.get())
        compose.onNodeWithText("꼭꼭 시작하기").performClick()
        assertEquals("start", selectedAction.get())
        compose.onNodeWithText("나중에 할게요").performClick()
        assertEquals("later", selectedAction.get())
        compose.onNodeWithContentDescription("뒤로 가기").performClick()
        assertEquals("back", selectedAction.get())
    }

    @Test
    fun sensitiveLocationEditorAcceptsCoordinatesAndOpensPlace() {
        compose.activity.setContent {
            SensitiveLocationFlow(
                viewModel = remember { SensitiveLocationViewModel(MockSensitiveLocationRepository()) },
                onBack = {}, onCumulative = {}, onMessage = {},
            )
        }
        compose.onNodeWithText("내 민감 위치").assertIsDisplayed()
        compose.onNodeWithText("＋ 장소 추가하기").performClick()
        compose.onNodeWithTag("location-name").performTextInput("공원")
        compose.onNodeWithText("학교").performClick()
        compose.onNodeWithText("500m").performClick()
        compose.onNodeWithTag("sensitive-place-latitude").performTextInput("37.5665")
        compose.onNodeWithTag("sensitive-place-longitude").performTextInput("126.9780")
        compose.onNodeWithText("저장하기").performClick()
        compose.onNodeWithText("공원").performClick()
        compose.onNodeWithText("단서가 보인 게시물").assertIsDisplayed()
        compose.onNodeWithText("아직 연결된 사진이 없어요. 누적 분석에서 저장 사진을 골라 연결할 수 있어요.").assertIsDisplayed()
    }

    @Test
    fun historyTabShowsMockRecordsAndSearchFiltersThem() {
        compose.activity.setContent { KkokKkokApp(history = remember { HistoryViewModel(MockHistoryRepository()) }) }
        compose.onNodeWithText("검사 기록").performClick()
        compose.onNodeWithText("카페 창가 사진").assertIsDisplayed()
        compose.onNodeWithText("모의 자료").assertIsDisplayed()
        compose.onNodeWithContentDescription("기록 검색").performClick()
        compose.onNodeWithTag("history-search").performTextInput("카페")
        compose.onNodeWithText("카페 창가 사진").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("등굣길 셀피").fetchSemanticsNodes().size)
        compose.onNodeWithText("SNS 분석").performClick()
        compose.onNodeWithText("주말 카페 게시물").assertIsDisplayed()
        compose.onNodeWithText("사진 검사").performClick()
        compose.onNodeWithContentDescription("카페 창가 사진 기록").performClick()
        compose.onNodeWithText("검사 결과").assertIsDisplayed()
        compose.onNodeWithText("예시 결과예요. 실제 위치·GPS를 분석하지 않았어요.").assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로 가기").performClick()
        compose.onNodeWithText("카페 창가 사진").assertIsDisplayed()
    }

    @Test
    fun protectionHistoryShowsMockPairsAndConnectsBottomNavigation() {
        compose.activity.setContent { KkokKkokApp(
            history = remember { HistoryViewModel(MockHistoryRepository()) },
            protectionHistory = remember { com.kkokkkok.android.protectionhistory.ProtectionHistoryViewModel(com.kkokkkok.android.protectionhistory.MockProtectionHistoryRepository()) },
        ) }
        compose.onNodeWithText("보호 기록").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("72 → 18", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("72 → 18", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("10월 11일", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("모의 예시예요 · 실제 사진은 저장하지 않아요").assertIsDisplayed()
        compose.onNodeWithText("검사 기록").performClick()
        compose.onNodeWithText("카페 창가 사진").assertIsDisplayed()
        compose.onNodeWithText("보호 기록").performClick()
        compose.onNodeWithText("55 → 12", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("홈").performClick()
        compose.onNodeWithText("올리기 전에, 꼭꼭 확인해요!").assertIsDisplayed()
    }
}
