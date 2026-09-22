package com.meetspot.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.meetspot.app.data.LocationHelper
import com.meetspot.app.data.model.Participant
import com.meetspot.app.data.model.ParticipantProfileSnapshot
import com.meetspot.app.data.repository.GroupRoomRepository
import com.meetspot.app.data.repository.ProfileRepository
import com.meetspot.app.ui.defaultMeetingTime
import com.meetspot.app.ui.toIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class GroupUiState(
    val organizerName: String = "",
    val location: String = "",
    val preference: String = "",
    val groupSize: Int = 3,
    val meetingTime: ZonedDateTime = defaultMeetingTime(),
    val travelMode: String = "TRANSIT",
    val maxMinutes: Int = 30,
    val locating: Boolean = false,
    val creating: Boolean = false,
    val error: String? = null,
    val createdRoomId: String? = null,
)

/** Mirrors the "Group room" creation form + `#create-meeting` handler in public/app.js. */
class GroupViewModel(
    private val groupRoomRepository: GroupRoomRepository,
    private val profileRepository: ProfileRepository,
    private val locationHelper: LocationHelper,
    private val currentUserProvider: () -> FirebaseUser?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    fun onOrganizerNameChange(v: String) { _uiState.value = _uiState.value.copy(organizerName = v) }
    fun onLocationChange(v: String) { _uiState.value = _uiState.value.copy(location = v) }
    fun onPreferenceChange(v: String) { _uiState.value = _uiState.value.copy(preference = v) }
    fun onGroupSizeChange(v: Int) { _uiState.value = _uiState.value.copy(groupSize = v) }
    fun onMeetingTimeChange(v: ZonedDateTime) { _uiState.value = _uiState.value.copy(meetingTime = v) }
    fun onTravelModeChange(v: String) { _uiState.value = _uiState.value.copy(travelMode = v) }
    fun onMaxMinutesChange(v: Int) { _uiState.value = _uiState.value.copy(maxMinutes = v) }

    fun useCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(locating = true)
            val result = locationHelper.currentLocationString()
            _uiState.value = result.fold(
                onSuccess = { _uiState.value.copy(location = it, locating = false) },
                onFailure = { _uiState.value.copy(locating = false, error = it.message) },
            )
        }
    }

    /** Requires the caller to already be signed in — mirrors app.js prompting sign-in first if needed. */
    fun createRoom() {
        val state = _uiState.value
        val user = currentUserProvider()
        if (state.organizerName.isBlank() || state.location.isBlank() || state.preference.isBlank()) {
            _uiState.value = state.copy(error = "Add your name, your location, the request, and meeting time first.")
            return
        }
        if (user == null) {
            _uiState.value = state.copy(error = "Sign in with Google first so the group room can be saved.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(creating = true, error = null)
            val profile = profileRepository.loadProfile(user.uid) ?: ParticipantProfileSnapshot()
            val organizer = Participant(
                uid = user.uid,
                name = state.organizerName,
                location = state.location,
                preference = state.preference,
                profile = profile,
            )
            val result = groupRoomRepository.createRoom(
                organizerUid = user.uid,
                organizerName = state.organizerName,
                meetingTimeIso = state.meetingTime.toIso(),
                travelMode = state.travelMode,
                maxMinutes = state.maxMinutes,
                expectedParticipants = state.groupSize,
                organizer = organizer,
            )
            _uiState.value = result.fold(
                onSuccess = { id -> _uiState.value.copy(creating = false, createdRoomId = id) },
                onFailure = { _uiState.value.copy(creating = false, error = it.message ?: "Could not create the room.") },
            )
        }
    }

    fun consumeCreatedRoomId() {
        _uiState.value = _uiState.value.copy(createdRoomId = null)
    }
}
