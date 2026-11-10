package com.kkokkkok.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kkokkkok.android.profile.*
import com.kkokkkok.android.ui.profile.MyProfileScreen
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDeletionFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun confirmationCancellationFailureAndRetryRefreshRealProfile() {
        val calls = AtomicInteger(); val finished = AtomicBoolean()
        val repository = object : ProfileRepository {
            override suspend fun getOverview() = ProfileOverview("테스터", 2, 1, 0, emptyList(), false, 0, false)
            override suspend fun deleteAll() {
                if (calls.incrementAndGet() == 1) throw java.io.IOException("합성 오류 · 다시 시도해 주세요.")
            }
        }
        val model = ProfileViewModel(repository)
        compose.setContent { MyProfileScreen(model, {}, {}, {}, {}, onDataDeleted = { finished.set(true) }) }
        compose.onNodeWithText("모든 데이터 삭제").performScrollTo().performClick()
        compose.onNodeWithText("취소").performClick()
        assertEquals(0, calls.get())
        compose.onNodeWithText("모든 데이터 삭제").performScrollTo().performClick()
        compose.onNodeWithText("모두 삭제").assertIsEnabled()
        if (androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("captureUi") == "true") {
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            java.io.File(context.cacheDir,"delete-dialog-qa.png").outputStream().use {
                compose.onNodeWithTag("delete-data-dialog").captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            }
        }
        compose.onNodeWithText("모두 삭제").performClick()
        compose.onNodeWithText("합성 오류 · 다시 시도해 주세요.").assertIsDisplayed()
        assertFalse(finished.get())
        compose.onNodeWithText("모두 삭제").performClick()
        compose.waitUntil(5_000) { finished.get() }
        assertEquals(2,calls.get()); assertTrue(model.state.value.deleted)
        compose.onNodeWithTag("delete-data-dialog").assertDoesNotExist()
    }
}
