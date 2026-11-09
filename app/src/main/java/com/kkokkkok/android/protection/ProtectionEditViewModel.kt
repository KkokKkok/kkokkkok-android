package com.kkokkkok.android.protection

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kkokkkok.android.analysis.AnalysisClue
import com.kkokkkok.android.analysis.AnalysisResult
import com.kkokkkok.android.analysis.ClueKind
import com.kkokkkok.android.analysis.ClueRisk
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class ProtectionStep { LOCATION, TARGETS, REQUEST, RESULT }
enum class EditMode { AUTOMATIC, MANUAL }

data class ProtectionEditState(
    val step: ProtectionStep = ProtectionStep.LOCATION,
    val photo: Uri? = null,
    val inspectionId: String? = null,
    val availableClues: List<AnalysisClue> = emptyList(),
    val location: String = "꼭꼭로 24, 2층 카페",
    val locationCategory: String = "자주 가는 곳",
    val mode: EditMode = EditMode.MANUAL,
    val selectedTargets: Set<String> = emptySet(),
    val strength: Float = .5f,
    val request: String = "",
    val isCreating: Boolean = false,
    val error: Boolean = false,
    val errorMessage: String? = null,
    val preview: ProtectionPreview? = null,
    val idempotencyKey: String? = null,
)

/** One in-memory editing session tied to the completed backend inspection. */
class ProtectionEditViewModel(
    private val repository: ProtectionEditRepository = MockProtectionEditRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProtectionEditState())
    val state = mutableState.asStateFlow()

    private var verificationJob: Job? = null
    private var createJob: Job? = null

    fun begin(photo: Uri, result: AnalysisResult? = null) {
        verificationJob?.cancel()
        createJob?.cancel()
        val editableClues = result?.takeUnless { it.isMock }?.clues.orEmpty()
            .filter { it.risk == ClueRisk.HIGH || it.risk == ClueRisk.CAUTION }
            .filter { it.kind != ClueKind.METADATA }
            .filter { it.region != null }
        mutableState.value = ProtectionEditState(
            photo = photo,
            step = if (result?.isMock == false && !result.inspectionId.isNullOrBlank()) ProtectionStep.TARGETS else ProtectionStep.LOCATION,
            inspectionId = result?.takeUnless { it.isMock }?.inspectionId,
            availableClues = editableClues,
            selectedTargets = editableClues.map { it.id }.toSet(),
        )
    }

    fun leave() { createJob?.cancel(); verificationJob?.cancel(); mutableState.value = ProtectionEditState() }

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
        val mode = if (current.mode == EditMode.MANUAL) EditMode.AUTOMATIC else EditMode.MANUAL
        mutableState.value = current.copy(
            mode = mode,
            selectedTargets = if (mode == EditMode.AUTOMATIC) current.availableClues.map { it.id }.toSet() else current.selectedTargets,
        )
    }

    fun toggleTarget(targetId: String) {
        val current = mutableState.value
        val selected = if (targetId in current.selectedTargets) current.selectedTargets - targetId else current.selectedTargets + targetId
        mutableState.value = current.copy(selectedTargets = selected, idempotencyKey = null, error = false, errorMessage = null)
    }

    fun updateStrength(value: Float) {
        mutableState.value = mutableState.value.copy(strength = value.coerceIn(0f, 1f), idempotencyKey = null)
    }

    fun continueToRequest() {
        mutableState.value = mutableState.value.copy(step = ProtectionStep.REQUEST, error = false)
    }

    fun updateRequest(value: String) {
        mutableState.value = mutableState.value.copy(request = value.take(200), idempotencyKey = null, error = false, errorMessage = null)
    }

    fun useSuggestion(value: String) { updateRequest(value) }

    fun createPreview() {
        val current = mutableState.value
        if (current.isCreating) return
        val photo = current.photo
        val inspectionId = current.inspectionId
        val selectedClues = current.availableClues.filter { it.id in current.selectedTargets }
        if (photo == null || inspectionId.isNullOrBlank() || selectedClues.isEmpty()) {
            mutableState.value = current.copy(
                error = true,
                errorMessage = if (inspectionId.isNullOrBlank()) "먼저 실제 사진 검사를 완료한 뒤 보호 편집을 시작해 주세요."
                else "편집할 단서를 하나 이상 선택해 주세요.",
            )
            return
        }
        val requestKey = current.idempotencyKey ?: UUID.randomUUID().toString()
        createJob = viewModelScope.launch {
            mutableState.value = current.copy(isCreating = true, error = false, errorMessage = null, idempotencyKey = requestKey)
            try {
                val preview = repository.createEdit(
                    inspectionId = inspectionId,
                    cueIds = selectedClues.map { it.id },
                    preservePrompt = current.request,
                    editStrength = (current.strength * 100).toInt(),
                    idempotencyKey = requestKey,
                    sourcePhoto = photo,
                )
                mutableState.value = mutableState.value.copy(
                    step = ProtectionStep.RESULT,
                    isCreating = false,
                    preview = preview,
                )
                watchVerification(preview)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = mutableState.value.copy(
                    isCreating = false,
                    error = true,
                    errorMessage = failure.message ?: "사진을 편집하지 못했어요. 다시 시도해 주세요.",
                )
            }
        }
    }

    private fun watchVerification(initial: ProtectionPreview) {
        verificationJob?.cancel()
        if (initial.isMock || initial.verificationInspectionId == null || initial.verificationStatus !in setOf("queued", "processing", "submitting")) return
        verificationJob = viewModelScope.launch {
            var latest = initial
            repeat(150) {
                delay(2_000)
                if (mutableState.value.step != ProtectionStep.RESULT || mutableState.value.preview?.verificationInspectionId != initial.verificationInspectionId) return@launch
                try {
                    latest = repository.refreshVerification(latest)
                    mutableState.value = mutableState.value.copy(preview = latest, errorMessage = null)
                    if (latest.verificationStatus !in setOf("queued", "processing", "submitting")) return@launch
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    mutableState.value = mutableState.value.copy(errorMessage = "재검사 상태를 불러오지 못했어요. 검사 기록에서 다시 확인해 주세요.")
                    return@launch
                }
            }
            mutableState.value = mutableState.value.copy(errorMessage = "재검사에 시간이 걸리고 있어요. 잠시 후 검사 기록에서 확인해 주세요.")
        }
    }

    fun retryPreview() = createPreview()

    fun back() {
        createJob?.cancel()
        verificationJob?.cancel()
        val current = mutableState.value.copy(isCreating = false, error = false, errorMessage = null)
        mutableState.value = when (current.step) {
            ProtectionStep.LOCATION -> ProtectionEditState()
            ProtectionStep.TARGETS -> current.copy(step = ProtectionStep.LOCATION)
            ProtectionStep.REQUEST -> current.copy(step = ProtectionStep.TARGETS)
            ProtectionStep.RESULT -> current.copy(step = ProtectionStep.REQUEST, preview = null)
        }
    }
}

internal class ProtectionEditViewModelFactory(
    private val repository: ProtectionEditRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ProtectionEditViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return ProtectionEditViewModel(repository) as T
    }
}
