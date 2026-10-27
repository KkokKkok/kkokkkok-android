package com.kkokkkok.android

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Fake Activity results only; real system UI is verified separately on the emulator. */
@RunWith(AndroidJUnit4::class)
class PhotoPickerResultTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun singleImageResultReachesViewModelAndCancelKeepsPreviousSelection() {
        val model = PhotoSelectionViewModel()
        val first = Uri.parse("content://synthetic.test/photo/first")
        val second = Uri.parse("content://synthetic.test/photo/second")
        val results = ArrayDeque<Uri?>().apply { add(first); add(null); add(second) }
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                assertTrue(contract is PickVisualMedia)
                assertSame(PickVisualMedia.ImageOnly, (input as PickVisualMediaRequest).mediaType)
                dispatchResult(requestCode, results.removeFirst())
            }
        }
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                KkokKkokApp(model)
            }
        }
        val action = compose.onNodeWithText("위치 노출 검사")
        action.performScrollTo().performClick()
        compose.runOnIdle { assertEquals(first, model.selectedPhoto.value) }
        compose.onNodeWithText("변경").performClick()
        compose.runOnIdle { assertEquals(first, model.selectedPhoto.value) }
        compose.onNodeWithText("변경").performClick()
        compose.runOnIdle { assertEquals(second, model.selectedPhoto.value) }
    }

    @Test
    fun cancelWithoutSelectionKeepsEmptyState() {
        val model = PhotoSelectionViewModel()
        model.onPhotoPicked(null)
        assertNull(model.selectedPhoto.value)
    }

    @Test
    fun locationIsRequiredAndCategoryCanFillIt() {
        val model = PhotoSelectionViewModel()
        val uri = Uri.parse("content://synthetic.test/photo/location")
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) { dispatchResult(requestCode, uri) }
        }
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                KkokKkokApp(model)
            }
        }
        compose.onNodeWithText("위치 노출 검사").performScrollTo().performClick()
        compose.onNodeWithText("촬영 위치 입력").assertIsDisplayed()
        compose.onNodeWithTag("photo-location-submit").assertIsEnabled()
        compose.onNodeWithTag("photo-location-field").performTextClearance()
        compose.onNodeWithText("위치를 입력해야 검사를 시작할 수 있어요").assertIsDisplayed()
        compose.onAllNodesWithText("모의 지도").assertCountEquals(0)
        compose.onNodeWithTag("photo-location-submit").assertIsNotEnabled()
        compose.onNodeWithText("학교").performClick()
        compose.onAllNodesWithText("꼭꼭대학교 정문 (예시)").assertCountEquals(2)
        compose.onNodeWithTag("photo-location-submit").assertIsEnabled()
    }
}
