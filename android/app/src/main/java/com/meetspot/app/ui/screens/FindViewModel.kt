package com.meetspot.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meetspot.app.data.LocationHelper
import com.meetspot.app.data.remote.dto.RecommendResponse
import com.meetspot.app.data.repository.RecommendRepository
import com.meetspot.app.ui.defaultMeetingTime
import com.meetspot.app.ui.toIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

enum class LocationField { PERSON_A, PERSON_B }

data class FindUiState(
    val personA: String = "",
    val personB: String = "",
    val query: String = "",
    val meetingTime: ZonedDateTime = defaultMeetingTime(),
    val travelMode: String = "TRANSIT",
    val maxMinutes: Int = 30,
    val submitting: Boolean = false,
    val error: String? = null,
    val results: RecommendResponse? = null,
    val locatingField: LocationField? = null,
)

/** Mirrors the "Find a spot" form + submit handler in public/app.js. */
class FindViewModel(
    private val repository: RecommendRepository,
    private val locationHelper: LocationHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FindUiState())
    val uiState: StateFlow<FindUiState> = _uiState.asStateFlow()

    fun onPersonAChange(value: String) { _uiState.value = _uiState.value.copy(personA = value) }
    fun onPersonBChange(value: String) { _uiState.value = _uiState.value.copy(personB = value) }
    fun onQueryChange(value: String) { _uiState.value = _uiState.value.copy(query = value) }
    fun onMeetingTimeChange(value: ZonedDateTime) { _uiState.value = _uiState.value.copy(meetingTime = value) }
    fun onTravelModeChange(value: String) { _uiState.value = _uiState.value.copy(travelMode = value) }
    fun onMaxMinutesChange(value: Int) { _uiState.value = _uiState.value.copy(maxMinutes = value) }

    fun useCurrentLocation(field: LocationField) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(locatingField = field)
            val result = locationHelper.currentLocationString()
            _uiState.value = result.fold(
                onSuccess = { location ->
                    when (field) {
                        LocationField.PERSON_A -> _uiState.value.copy(personA = location, locatingField = null)
                        LocationField.PERSON_B -> _uiState.value.copy(personB = location, locatingField = null)
                    }
                },
                onFailure = { _uiState.value.copy(locatingField = null, error = it.message) },
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.personA.isBlank() || state.personB.isBlank() || state.query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(submitting = true, error = null, results = null)
            val result = repository.recommendForPair(
                personA = state.personA,
                personB = state.personB,
                query = state.query,
                meetingTimeIso = state.meetingTime.toIso(),
                travelMode = state.travelMode,
                maxMinutes = state.maxMinutes,
            )
            _uiState.value = result.fold(
                onSuccess = { _uiState.value.copy(submitting = false, results = it) },
                onFailure = { _uiState.value.copy(submitting = false, error = it.message ?: "The search failed.") },
            )
        }
    }
}
