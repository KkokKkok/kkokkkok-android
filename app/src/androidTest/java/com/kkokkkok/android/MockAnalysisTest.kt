package com.kkokkkok.android

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.analysis.AnalysisRepository
import com.kkokkkok.android.analysis.AnalysisState
import com.kkokkkok.android.analysis.AnalysisStep
import com.kkokkkok.android.analysis.MockAnalysisRepository
import com.kkokkkok.android.ui.photo.PhotoSelectionViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MockAnalysisTest {
    @Test
    fun mockCompletesAndCancellingStopsRepositoryWork() = runBlocking {
        val photo = Uri.parse("content://synthetic.test/photo/sample")
        val completed = PhotoSelectionViewModel(MockAnalysisRepository(stepDelayMillis = 0))
        onMain { completed.onPhotoPicked(photo); completed.startAnalysis() }
        val result = withTimeout(2_000) {
            completed.analysisState.first { it is AnalysisState.Completed }
        } as AnalysisState.Completed
        assertTrue(result.result.isMock)
        assertTrue(result.result.clues.isNotEmpty())

        val cancelled = CompletableDeferred<Unit>()
        val repository = object : AnalysisRepository {
            override fun analyze(photo: Uri) = flow {
                try {
                    emit(AnalysisState.Running(AnalysisStep.TEXT, 25))
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        }
        val running = PhotoSelectionViewModel(repository)
        onMain { running.onPhotoPicked(photo); running.startAnalysis() }
        withTimeout(2_000) { running.analysisState.first { it is AnalysisState.Running } }
        onMain { running.cancelAnalysis() }
        withTimeout(2_000) { cancelled.await() }
        assertEquals(AnalysisState.Idle, running.analysisState.value)
    }

    private fun onMain(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }
}
