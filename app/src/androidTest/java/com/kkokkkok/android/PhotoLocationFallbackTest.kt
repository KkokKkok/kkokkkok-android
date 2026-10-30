package com.kkokkkok.android

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.analysis.AnalysisRepository
import com.kkokkkok.android.analysis.AnalysisStep
import com.kkokkkok.android.analysis.AnalysisState
import com.kkokkkok.android.analysis.MockAnalysisRepository
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoLocationFallbackTest {
    private val photo = Uri.parse("content://synthetic.test/photo")

    @Test
    fun missingExifRequiresAndForwardsManualLocation() = runBlocking {
        val submittedLocation = CompletableDeferred<String>()
        val model = PhotoSelectionViewModel(repository(hasGps = false) { location ->
            submittedLocation.complete(location)
        })

        onMain { model.onPhotoPicked(photo) }
        withTimeout(2_000) { model.locationState.first { it.requiresManualLocation } }
        assertEquals("", model.locationState.value.location)
        onMain { assertFalse(model.startAnalysis()) }
        onMain { model.updateLocation("서울시 영등포구 예시로 12"); assertTrue(model.startAnalysis()) }

        assertEquals("서울시 영등포구 예시로 12", withTimeout(2_000) { submittedLocation.await() })
        withTimeout(2_000) { model.analysisState.first { it is AnalysisState.Completed } }
        Unit
    }

    @Test
    fun availableExifStartsAnalysisWithoutManualLocation() = runBlocking {
        val submittedLocation = CompletableDeferred<String>()
        val model = PhotoSelectionViewModel(repository(hasGps = true) { location ->
            submittedLocation.complete(location)
        })

        onMain { model.onPhotoPicked(photo) }
        assertEquals("", withTimeout(2_000) { submittedLocation.await() })
        withTimeout(2_000) { model.analysisState.first { it is AnalysisState.Completed } }
        Unit
    }

    @Test
    fun cancellingExifBasedAnalysisAllowsManualFallback() = runBlocking {
        val analysisStarted = CompletableDeferred<Unit>()
        val model = PhotoSelectionViewModel(object : AnalysisRepository {
            override suspend fun hasExifGps(photo: Uri): Boolean = true
            override fun analyze(photo: Uri, location: String) = flow<AnalysisState> {
                analysisStarted.complete(Unit)
                emit(AnalysisState.Running(AnalysisStep.METADATA, 10))
                awaitCancellation()
            }
        })

        onMain { model.onPhotoPicked(photo) }
        withTimeout(2_000) { analysisStarted.await() }
        withTimeout(2_000) { model.analysisState.first { it is AnalysisState.Running } }
        onMain { model.cancelAnalysis() }

        assertTrue(model.locationState.value.requiresManualLocation)
        assertEquals("", model.locationState.value.location)
        assertEquals(AnalysisState.Idle, model.analysisState.value)
    }

    private fun repository(hasGps: Boolean, onSubmit: (String) -> Unit) = object : AnalysisRepository {
        override suspend fun hasExifGps(photo: Uri): Boolean = hasGps

        override fun analyze(photo: Uri, location: String) = flow {
            onSubmit(location)
            emit(MockAnalysisRepository(stepDelayMillis = 0).analyze(photo, location).last())
        }
    }

    private fun onMain(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }
}
