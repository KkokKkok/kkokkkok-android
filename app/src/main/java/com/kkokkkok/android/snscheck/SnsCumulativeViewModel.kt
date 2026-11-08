package com.kkokkkok.android.snscheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SnsCumulativeState(
    val places: List<SnsPlaceOption> = emptyList(),
    val posts: List<SnsPostPreview> = emptyList(),
    val selectedPlaceId: String = "home",
    val selectedPostIds: Set<String> = emptySet(),
    val manualLocations: Map<String, Pair<String, String>> = emptyMap(),
    val step: SnsCumulativeStep = SnsCumulativeStep.INTRO,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val result: SnsExposureResult? = null,
    val isAnalyzing: Boolean = false,
    val resultError: Boolean = false,
) {
    val selectedPostCount get() = selectedPostIds.size
}

enum class SnsCumulativeStep { INTRO, PLACE, POSTS, RESULT }

class SnsCumulativeViewModel(
    private val repository: SnsCumulativeRepository = PreviewSnsCumulativeRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SnsCumulativeState())
    val state = mutableState.asStateFlow()
    private var resultJob: Job? = null

    fun setStep(step: SnsCumulativeStep) {
        resultJob?.cancel()
        mutableState.value = mutableState.value.copy(step = step, isAnalyzing = false, resultError = false)
    }

    init { load() }

    fun selectPlace(id: String) {
        if (mutableState.value.places.none { it.id == id } || mutableState.value.selectedPlaceId == id) return
        mutableState.value = mutableState.value.copy(
            selectedPlaceId = id, posts = emptyList(), selectedPostIds = emptySet(),
            manualLocations = emptyMap(), isLoading = true, hasError = false,
        )
        viewModelScope.launch { loadPosts(id) }
    }

    fun refreshPlaces() = load()

    fun beginForPlace(placeId: String) {
        mutableState.value = mutableState.value.copy(step = SnsCumulativeStep.PLACE, selectedPlaceId = placeId)
        load()
    }

    fun updateManualLatitude(id: String, value: String) = updateManualLocation(id, latitude = value)
    fun updateManualLongitude(id: String, value: String) = updateManualLocation(id, longitude = value)

    private fun updateManualLocation(id: String, latitude: String? = null, longitude: String? = null) {
        val current = mutableState.value.manualLocations[id] ?: ("" to "")
        mutableState.value = mutableState.value.copy(manualLocations = mutableState.value.manualLocations +
            (id to ((latitude ?: current.first) to (longitude ?: current.second))))
    }

    fun togglePost(id: String) {
        val state = mutableState.value
        if (state.posts.none { it.id == id }) return
        if (id !in state.selectedPostIds && state.selectedPostIds.size >= 100) return
        mutableState.value = state.copy(
            selectedPostIds = if (id in state.selectedPostIds) state.selectedPostIds - id
            else state.selectedPostIds + id,
        )
    }

    fun selectAllVisible(postIds: List<String>) {
        val state = mutableState.value
        val selected = state.selectedPostIds.toMutableSet()
        val allVisibleSelected = postIds.isNotEmpty() && postIds.all { it in selected }
        if (allVisibleSelected) selected.removeAll(postIds.toSet())
        else postIds.filterNot { it in selected }.take((100 - selected.size).coerceAtLeast(0)).forEach(selected::add)
        mutableState.value = state.copy(selectedPostIds = selected)
    }

    fun showResult() {
        val snapshot = mutableState.value
        if (snapshot.isAnalyzing || snapshot.selectedPostIds.isEmpty()) return
        resultJob = viewModelScope.launch {
            mutableState.value = snapshot.copy(isAnalyzing = true, resultError = false, result = null)
            try {
                val result = repository.getExposureResult(
                    placeId = snapshot.selectedPlaceId,
                    selectedPostIds = snapshot.selectedPostIds,
                    manualLocations = snapshot.manualLocations,
                )
                mutableState.value = mutableState.value.copy(
                    result = result, step = SnsCumulativeStep.RESULT, isAnalyzing = false,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    step = SnsCumulativeStep.RESULT,
                    isAnalyzing = false,
                    resultError = true,
                )
            }
        }
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            val currentStep = mutableState.value.step
            mutableState.value = mutableState.value.copy(isLoading = true, hasError = false)
            try {
                val places = repository.getPlaceOptions()
                val selectedPlace = mutableState.value.selectedPlaceId.takeIf { id -> places.any { it.id == id } }
                    ?: places.firstOrNull()?.id.orEmpty()
                mutableState.value = SnsCumulativeState(
                    places = places,
                    posts = emptyList(),
                    step = currentStep,
                    selectedPlaceId = selectedPlace,
                    isLoading = selectedPlace.isNotBlank(),
                )
                if (selectedPlace.isNotBlank()) loadPosts(selectedPlace)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(isLoading = false, hasError = true)
            }
        }
    }

    private suspend fun loadPosts(placeId: String) {
        try {
            val posts = repository.getPostPreviews(placeId)
            val linked = repository.getLinkedAssetIds(placeId)
            mutableState.value = mutableState.value.copy(
                posts = posts,
                selectedPostIds = linked,
                isLoading = false,
                hasError = false,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(isLoading = false, hasError = true)
        }
    }
}

internal class SnsCumulativeViewModelFactory(
    private val repository: SnsCumulativeRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(SnsCumulativeViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return SnsCumulativeViewModel(repository) as T
    }
}

/** Empty fallback for isolated UI previews and tests. MainActivity injects the backend repository. */
private object PreviewSnsCumulativeRepository : SnsCumulativeRepository {
    override suspend fun getPlaceOptions() = emptyList<SnsPlaceOption>()
    override suspend fun getPostPreviews(placeId: String) = emptyList<SnsPostPreview>()
    override suspend fun getLinkedAssetIds(placeId: String) = emptySet<String>()
    override suspend fun getExposureResult(
        placeId: String,
        selectedPostIds: Set<String>,
        manualLocations: Map<String, Pair<String, String>>,
    ): SnsExposureResult = throw IllegalStateException("Preview repository cannot run analysis")
}
