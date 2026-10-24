package com.kkokkkok.android.snscheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SnsCumulativeState(
    val places: List<SnsPlaceOption> = emptyList(),
    val posts: List<SnsPostPreview> = emptyList(),
    val selectedPlaceId: String = "home",
    val selectedPostIds: Set<String> = emptySet(),
    val step: SnsCumulativeStep = SnsCumulativeStep.INTRO,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) {
    val selectedPostCount get() = selectedPostIds.size
}

enum class SnsCumulativeStep { INTRO, PLACE, POSTS }

class SnsCumulativeViewModel(
    private val repository: SnsCumulativeRepository = MockSnsCumulativeRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(SnsCumulativeState())
    val state = mutableState.asStateFlow()

    fun setStep(step: SnsCumulativeStep) {
        mutableState.value = mutableState.value.copy(step = step)
    }

    init { load() }

    fun selectPlace(id: String) {
        if (mutableState.value.places.any { it.id == id }) {
            mutableState.value = mutableState.value.copy(selectedPlaceId = id)
        }
    }

    fun setAdditionalPlaces(places: List<SnsPlaceOption>) {
        val state = mutableState.value
        val defaults = state.places.filter { it.id == "home" || it.id == "school" }
        val options = defaults + places
        mutableState.value = state.copy(
            places = options,
            selectedPlaceId = state.selectedPlaceId.takeIf { id -> options.any { it.id == id } }
                ?: options.firstOrNull()?.id.orEmpty(),
        )
    }

    fun togglePost(id: String) {
        val state = mutableState.value
        if (state.posts.none { it.id == id }) return
        mutableState.value = state.copy(
            selectedPostIds = if (id in state.selectedPostIds) state.selectedPostIds - id
            else state.selectedPostIds + id,
        )
    }

    fun selectAllVisible(postIds: List<String>) {
        val state = mutableState.value
        val selected = state.selectedPostIds.toMutableSet()
        val allVisibleSelected = postIds.isNotEmpty() && postIds.all { it in selected }
        if (allVisibleSelected) selected.removeAll(postIds.toSet()) else selected.addAll(postIds)
        mutableState.value = state.copy(selectedPostIds = selected)
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            val currentStep = mutableState.value.step
            mutableState.value = mutableState.value.copy(isLoading = true, hasError = false)
            try {
                val places = repository.getPlaceOptions()
                val posts = repository.getPostPreviews()
                mutableState.value = SnsCumulativeState(
                    places = places,
                    posts = posts,
                    step = currentStep,
                    selectedPlaceId = places.firstOrNull()?.id.orEmpty(),
                    selectedPostIds = posts.filter { it.initiallySelected }.mapTo(mutableSetOf()) { it.id },
                    isLoading = false,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(isLoading = false, hasError = true)
            }
        }
    }
}
