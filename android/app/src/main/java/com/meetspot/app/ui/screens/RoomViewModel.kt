package com.meetspot.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.meetspot.app.data.LocationHelper
import com.meetspot.app.data.model.MeetingRoom
import com.meetspot.app.data.model.Participant
import com.meetspot.app.data.model.ParticipantProfileSnapshot
import com.meetspot.app.data.remote.dto.ParticipantInput
import com.meetspot.app.data.remote.dto.ParticipantProfileInput
import com.meetspot.app.data.repository.GroupRoomRepository
import com.meetspot.app.data.repository.ProfileRepository
import com.meetspot.app.data.repository.RecommendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomUiState(
    val loading: Boolean = true,
    val room: MeetingRoom? = null,
    val error: String? = null,
    val guestName: String = "",
    val guestLocation: String = "",
    val guestPreference: String = "",
    val locating: Boolean = false,
    val joining: Boolean = false,
    val statusMessage: String? = null,
)

/** Mirrors `loadGroupRoom` in public/app.js: live room state + the join handler. */
class RoomViewModel(
    private val roomId: String,
    private val groupRoomRepository: GroupRoomRepository,
    private val recommendRepository: RecommendRepository,
    private val profileRepository: ProfileRepository,
    private val locationHelper: LocationHelper,
    private val currentUserProvider: () -> FirebaseUser?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            groupRoomRepository.observeRoom(roomId).collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { room -> _uiState.value.copy(loading = false, room = room, error = null) },
                    onFailure = { _uiState.value.copy(loading = false, error = it.message ?: "Room could not load.") },
                )
            }
        }
    }

    fun onGuestNameChange(v: String) { _uiState.value = _uiState.value.copy(guestName = v) }
    fun onGuestLocationChange(v: String) { _uiState.value = _uiState.value.copy(guestLocation = v) }
    fun onGuestPreferenceChange(v: String) { _uiState.value = _uiState.value.copy(guestPreference = v) }

    fun useCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(locating = true)
            val result = locationHelper.currentLocationString()
            _uiState.value = result.fold(
                onSuccess = { _uiState.value.copy(guestLocation = it, locating = false) },
                onFailure = { _uiState.value.copy(locating = false, error = it.message) },
            )
        }
    }

    fun join() {
        val state = _uiState.value
        val room = state.room ?: return
        val user = currentUserProvider() ?: run {
            _uiState.value = state.copy(error = "Sign in with Google to join this room.")
            return
        }
        if (room.participants.any { it.uid == user.uid }) {
            _uiState.value = state.copy(error = "You have already joined this room.")
            return
        }
        if (state.guestName.isBlank() || state.guestLocation.isBlank() || state.guestPreference.isBlank()) {
            _uiState.value = state.copy(error = "Add your name, location, and what you would like to do.")
            return
        }
        if (room.participants.size >= room.expectedParticipants) {
            _uiState.value = state.copy(error = "This room already has everyone it was created for.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(joining = true, error = null, statusMessage = "Adding your vote and comparing the whole group…")
            val profile = profileRepository.loadProfile(user.uid) ?: ParticipantProfileSnapshot()
            val participant = Participant(user.uid, state.guestName, state.guestLocation, state.guestPreference, profile)

            val addResult = groupRoomRepository.addParticipant(roomId, participant)
            if (addResult.isFailure) {
                _uiState.value = _uiState.value.copy(joining = false, error = addResult.exceptionOrNull()?.message, statusMessage = null)
                return@launch
            }

            val updatedParticipants = room.participants + participant
            if (updatedParticipants.size < room.expectedParticipants) {
                val remaining = room.expectedParticipants - updatedParticipants.size
                _uiState.value = _uiState.value.copy(
                    joining = false,
                    statusMessage = "You joined. Waiting for $remaining more participant${if (remaining == 1) "" else "s"}…",
                )
                return@launch
            }

            val combinedPreference = updatedParticipants.map { it.preference }.distinct().joinToString(" or ")
            val recommendResult = recommendRepository.recommendForGroup(
                participants = updatedParticipants.map { it.toRecommendInput() },
                query = combinedPreference,
                meetingTimeIso = room.meetingTime,
                travelMode = room.travelMode,
                maxMinutes = room.maxMinutes,
            )
            if (recommendResult.isFailure) {
                _uiState.value = _uiState.value.copy(joining = false, error = recommendResult.exceptionOrNull()?.message, statusMessage = null)
                return@launch
            }
            val results = recommendResult.getOrThrow()
            groupRoomRepository.saveResults(roomId, results, combinedPreference)
            _uiState.value = _uiState.value.copy(joining = false, statusMessage = null)
        }
    }
}

private fun Participant.toRecommendInput() = ParticipantInput(
    name = name,
    location = location,
    preference = preference,
    profile = profile?.let { ParticipantProfileInput(it.preferences, it.timelineSummary) },
)
