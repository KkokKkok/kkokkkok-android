package com.kkokkkok.android.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SensitiveLocationState(
    val locations: List<SensitiveLocation> = emptyList(),
    val isLoading: Boolean = true,
    val editing: SensitiveLocation? = null,
)

class SensitiveLocationViewModel(
    private val repository: SensitiveLocationRepository = MockSensitiveLocationRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(SensitiveLocationState())
    val state = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.value = mutableState.value.copy(isLoading = true)
        mutableState.value = mutableState.value.copy(locations = repository.getLocations(), isLoading = false)
    }

    fun beginAdd() { mutableState.value = mutableState.value.copy(editing = SensitiveLocation("new-${UUID.randomUUID()}", "", LocationType.HOME, "꼭꼭로 12", 300)) }
    fun beginEdit(location: SensitiveLocation) { mutableState.value = mutableState.value.copy(editing = location) }
    fun cancelEdit() { mutableState.value = mutableState.value.copy(editing = null) }
    fun updateName(name: String) { mutableState.value = mutableState.value.copy(editing = mutableState.value.editing?.copy(name = name.take(24))) }
    fun updateType(type: LocationType) {
        val address = when (type) {
            LocationType.HOME -> "꼭꼭로 12"
            LocationType.SCHOOL -> "꼭꼭고등학교 정문"
            LocationType.WORK -> "꼭꼭빌딩 3층"
            LocationType.FREQUENT -> "꼭꼭로 24"
        }
        mutableState.value = mutableState.value.copy(editing = mutableState.value.editing?.copy(type = type, address = address))
    }
    fun updateRadius(meters: Int) { mutableState.value = mutableState.value.copy(editing = mutableState.value.editing?.copy(radiusMeters = meters)) }

    fun save() = viewModelScope.launch {
        val location = mutableState.value.editing ?: return@launch
        if (location.name.isBlank()) return@launch
        repository.save(location.copy(name = location.name.trim()))
        mutableState.value = mutableState.value.copy(locations = repository.getLocations(), editing = null)
    }

    fun delete(id: String) = viewModelScope.launch {
        repository.delete(id)
        mutableState.value = mutableState.value.copy(locations = repository.getLocations(), editing = null)
    }
}
