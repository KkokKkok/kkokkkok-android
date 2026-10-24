package com.kkokkkok.android

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android picker verification, using only a temporary synthetic image on an emulator. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class SystemPhotoPickerTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun realPickerSelectsImageAndRetainsSelectionAcrossRecreation() {
        assumeTrue(Build.MODEL.contains("sdk"))
        val context = compose.activity
        // The system picker accessibility descriptions below are for our English test emulator.
        assumeTrue(context.resources.configuration.locales[0].language == "en")
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "kkok_synthetic_picker_test.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/KkokKkokTest")
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val fixture = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        try {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.rgb(45, 180, 120))
            try {
                requireNotNull(resolver.openOutputStream(fixture)).use {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally {
                bitmap.recycle()
            }
            resolver.update(fixture, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)

            val model = ViewModelProvider(context)[PhotoSelectionViewModel::class.java]
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val action = compose.onNodeWithText("위치 노출 검사")
            action.performScrollTo().performClick()
            automation.waitForIdle(500, 10_000)
            val deadline = SystemClock.uptimeMillis() + 10_000
            var image: AccessibilityNodeInfo? = null
            while (image == null && SystemClock.uptimeMillis() < deadline) {
                image = findPhoto(automation.rootInActiveWindow)
                if (image == null) SystemClock.sleep(100)
            }
            assertTrue("Synthetic image must appear in the real picker", image != null)
            var target = requireNotNull(image)
            while (!target.isClickable) target = requireNotNull(target.parent)
            assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            compose.waitUntil(10_000) { model.selectedPhoto.value != null }
            // Read on the instrumentation thread and verify the selected content, without logging a URI.
            val selected = requireNotNull(model.selectedPhoto.value)
            assertTrue("Only the inserted synthetic image should be selected", selected.lastPathSegment == fixture.lastPathSegment)
            requireNotNull(resolver.openInputStream(selected)).use {
                val decoded = requireNotNull(BitmapFactory.decodeStream(it))
                try {
                    assertEquals(Color.rgb(45, 180, 120), decoded.getPixel(0, 0))
                } finally {
                    decoded.recycle()
                }
            }
            compose.activityRule.scenario.recreate()
            val restored = ViewModelProvider(compose.activity)[PhotoSelectionViewModel::class.java]
            assertTrue(restored.selectedPhoto.value == selected)
            compose.onNodeWithContentDescription("뒤로 가기").performClick()
            compose.onNodeWithText("올리기 전에, 꼭꼭 확인해요!").assertIsDisplayed()
            assertTrue(restored.selectedPhoto.value == selected)
        } finally {
            // Remove only the image inserted by this test.
            resolver.delete(fixture, null, null)
        }
    }

    private fun findPhoto(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.contentDescription?.startsWith("Photo taken on") == true) return node
        repeat(node.childCount) { index ->
            findPhoto(node.getChild(index))?.let { return it }
        }
        return null
    }
}
