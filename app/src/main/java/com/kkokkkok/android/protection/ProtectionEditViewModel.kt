package com.kkokkkok.android.protection

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProtectionStep { LOCATION, TARGETS, REQUEST, RESULT }
enum class EditMode { AUTOMATIC, MANUAL }

data class ProtectionEditState(
    val step: ProtectionStep = ProtectionStep.LOCATION,
    val photo: Uri? = null,
    val location: String = "꼭꼭로 24, 2층 카페",
    val locationCategory: String = "자주 가는 곳",
    val mode: EditMode = EditMode.MANUAL,
    val selectedTargets: Set<String> = setOf("창밖 간판", "창밖 건물 · 동 번호", "위치 정보(GPS)"),
    val strength: Float = .5f,
    val request: String = "음료랑 케이크는 그대로 두고, 카페 분위기도 유지해줘",
    val isCreating: Boolean = false,
    val error: Boolean = false,
    val preview: ProtectionPreview? = null,
)

/** Holds one in-memory edit session. Photo and location are never sent to the repository. */
class ProtectionEditViewModel(
    private val repository: ProtectionEditRepository = MockProtectionEditRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProtectionEditState())
    val state = mutableState.asStateFlow()

    fun begin(photo: Uri) {
        mutableState.value = ProtectionEditState(photo = photo)
    }

    fun leave() { mutableState.value = ProtectionEditState() }

    fun updateLocation(value: String) {
        mutableState.value = mutableState.value.copy(location = value.take(60), locationCategory = "")
    }

    fun selectLocationCategory(value: String) {
        val exampleAddress = when (value) {
            "학교" -> "꼭꼭대학교 정문 (예시)"
            "직장" -> "꼭꼭빌딩 3층 (예시)"
            else -> "꼭꼭로 24, 2층 카페 (예시)"
        }
        mutableState.value = mutableState.value.copy(location = exampleAddress, locationCategory = value)
    }

    fun continueToTargets() {
        mutableState.value = mutableState.value.copy(step = ProtectionStep.TARGETS, error = false)
    }

    fun toggleMode() {
        val current = mutableState.value
        mutableState.value = current.copy(mode = if (current.mode == EditMode.MANUAL) EditMode.AUTOMATIC else EditMode.MANUAL)
    }

    fun toggleTarget(target: String) {
        val current = mutableState.value
        val selected = if (target in current.selectedTargets) current.selectedTargets - target else current.selectedTargets + target
        mutableState.value = current.copy(selectedTargets = selected)
    }

    fun updateStrength(value: Float) {
        mutableState.value = mutableState.value.copy(strength = value)
    }

    fun continueToRequest() {
        mutableState.value = mutableState.value.copy(step = ProtectionStep.REQUEST, error = false)
    }

    fun updateRequest(value: String) {
        mutableState.value = mutableState.value.copy(request = value.take(200))
    }

    fun useSuggestion(value: String) { updateRequest(value) }

    fun createPreview() {
        if (mutableState.value.isCreating) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isCreating = true, error = false)
            try {
                val preview = repository.createMockPreview()
                mutableState.value = mutableState.value.copy(
                    step = ProtectionStep.RESULT,
                    isCreating = false,
                    preview = preview,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(isCreating = false, error = true)
            }
        }
    }

    fun retryPreview() = createPreview()

    fun back() {
        val current = mutableState.value
        mutableState.value = when (current.step) {
            ProtectionStep.LOCATION -> ProtectionEditState()
            ProtectionStep.TARGETS -> current.copy(step = ProtectionStep.LOCATION)
            ProtectionStep.REQUEST -> current.copy(step = ProtectionStep.TARGETS)
            ProtectionStep.RESULT -> current.copy(step = ProtectionStep.REQUEST, preview = null)
        }
    }
}
