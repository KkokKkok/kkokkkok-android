package com.kkokkkok.android.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SensitiveLocationState(
    val locations: List<SensitiveLocation> = emptyList(),
    val isLoading: Boolean = true,
    val editing: SensitiveLocation? = null,
    val errorMessage: String? = null,
)

class SensitiveLocationViewModel(
    private val repository: SensitiveLocationRepository = MockSensitiveLocationRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(SensitiveLocationState())
    val state = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
        try {
            mutableState.value = mutableState.value.copy(locations = repository.getLocations(), isLoading = false)
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(isLoading = false, errorMessage = "장소를 불러오지 못했어요. 다시 시도해 주세요.")
        }
    }

    fun beginAdd() { mutableState.value = mutableState.value.copy(errorMessage = null, editing = SensitiveLocation("new-${UUID.randomUUID()}", "", LocationType.HOME, "위도와 경도를 입력해 주세요", 300)) }
    fun beginEdit(location: SensitiveLocation) { mutableState.value = mutableState.value.copy(errorMessage = null, editing = location) }
    fun cancelEdit() { mutableState.value = mutableState.value.copy(editing = null, errorMessage = null) }
    fun updateName(name: String) { mutableState.value = mutableState.value.copy(editing = mutableState.value.editing?.copy(name = name.take(24))) }
    fun updateType(type: LocationType) { mutableState.value = mutableState.value.copy(editing = mutableState.value.editing?.copy(type = type)) }
    fun updateLatitude(value: String) { mutableState.value = mutableState.value.copy(editing = mutableState.value.editing?.copy(latitude = value.filter { it.isDigit() || it in ".-" }.take(12))) }
    fun updateLongitude(value: String) { mutableState.value = mutableState.value.copy(editing = mutableState.value.editing?.copy(longitude = value.filter { it.isDigit() || it in ".-" }.take(13))) }
    fun updateRadius(meters: Int) { mutableState.value = mutableState.value.copy(editing = mutableState.value.editing?.copy(radiusMeters = meters)) }

    fun save() = viewModelScope.launch {
        val location = mutableState.value.editing ?: return@launch
        val latitude = location.latitude.toDoubleOrNull()
        val longitude = location.longitude.toDoubleOrNull()
        if (location.name.isBlank()) {
            mutableState.value = mutableState.value.copy(errorMessage = "장소 이름을 입력해 주세요.")
            return@launch
        }
        if (latitude == null || latitude !in -90.0..90.0 || longitude == null || longitude !in -180.0..180.0) {
            mutableState.value = mutableState.value.copy(errorMessage = "유효한 위도·경도를 입력해 주세요.")
            return@launch
        }
        try {
            repository.save(location.copy(name = location.name.trim()))
            mutableState.value = mutableState.value.copy(locations = repository.getLocations(), editing = null, errorMessage = null)
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(errorMessage = "장소를 저장하지 못했어요. 다시 시도해 주세요.")
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        try {
            repository.delete(id)
            mutableState.value = mutableState.value.copy(locations = repository.getLocations(), editing = null, errorMessage = null)
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(errorMessage = "장소를 삭제하지 못했어요. 다시 시도해 주세요.")
        }
    }
}

internal class SensitiveLocationViewModelFactory(
    private val repository: SensitiveLocationRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(SensitiveLocationViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return SensitiveLocationViewModel(repository) as T
    }
}
