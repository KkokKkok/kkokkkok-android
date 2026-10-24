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
        compose.runOnIdle { assertEquals(first, model.selectedPhoto.value); model.cancelAnalysis() }
        action.performScrollTo().performClick()
        compose.runOnIdle { assertEquals(first, model.selectedPhoto.value); model.cancelAnalysis() }
        action.performScrollTo().performClick()
        compose.runOnIdle { assertEquals(second, model.selectedPhoto.value); model.cancelAnalysis() }
    }

    @Test
    fun cancelWithoutSelectionKeepsEmptyState() {
        val model = PhotoSelectionViewModel()
        model.onPhotoPicked(null)
        assertNull(model.selectedPhoto.value)
    }
}
