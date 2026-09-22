package com.meetspot.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.meetspot.app.data.TimelineProcessor
import com.meetspot.app.data.TimelineSummary
import com.meetspot.app.data.repository.AuthRepository
import com.meetspot.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Mirrors the `profileFields` array in public/app.js. */
val PROFILE_FIELDS = listOf(
    "dietary" to "Dietary requirements",
    "accessibility" to "Accessibility requirements",
    "cuisines" to "Favourite cuisines",
    "activities" to "Activities you enjoy",
)

data class ProfileUiState(
    val user: FirebaseUser? = null,
    val loadingProfile: Boolean = false,
    val preferences: Map<String, String> = emptyMap(),
    val budget: String = "moderate",
    val atmosphere: String = "balanced",
    val timelineSummary: TimelineSummary? = null,
    val signingIn: Boolean = false,
    val saving: Boolean = false,
    val processingTimeline: Boolean = false,
    val statusMessage: String? = null,
    val error: String? = null,
)

/** Mirrors the Profile tab in public/app.js: auth state, `#profile-form`, and Timeline JSON upload. */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(user = authRepository.currentUser))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState().collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
                if (user != null) loadProfile(user.uid) else _uiState.value = ProfileUiState(user = null)
            }
        }
    }

    private fun loadProfile(uid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingProfile = true)
            val snapshot = profileRepository.loadProfile(uid)
            _uiState.value = _uiState.value.copy(
                loadingProfile = false,
                preferences = snapshot?.preferences ?: emptyMap(),
                budget = snapshot?.preferences?.get("budget") ?: "moderate",
                atmosphere = snapshot?.preferences?.get("atmosphere") ?: "balanced",
                timelineSummary = snapshot?.timelineSummary,
            )
        }
    }

    fun onPreferenceFieldChange(field: String, value: String) {
        _uiState.value = _uiState.value.copy(preferences = _uiState.value.preferences + (field to value))
    }

    fun onBudgetChange(value: String) { _uiState.value = _uiState.value.copy(budget = value) }
    fun onAtmosphereChange(value: String) { _uiState.value = _uiState.value.copy(atmosphere = value) }

    fun signIn(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(signingIn = true, error = null)
            val result = authRepository.signInWithGoogle(context)
            _uiState.value = _uiState.value.copy(
                signingIn = false,
                error = result.exceptionOrNull()?.let { "Google sign-in failed: ${it.message}" },
            )
        }
    }

    fun signOut() = authRepository.signOut()

    fun savePreferences() {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)
            val allPreferences = _uiState.value.preferences + mapOf(
                "budget" to _uiState.value.budget,
                "atmosphere" to _uiState.value.atmosphere,
            )
            try {
                profileRepository.savePreferences(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl?.toString(),
                    preferences = allPreferences,
                )
                _uiState.value = _uiState.value.copy(saving = false, statusMessage = "Your preferences are saved.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saving = false, error = "Preferences could not be saved: ${e.message}")
            }
        }
    }

    fun processTimelineJson(rawJson: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processingTimeline = true, error = null)
            try {
                val summary = TimelineProcessor.process(rawJson)
                profileRepository.saveTimelineSummary(user.uid, summary)
                _uiState.value = _uiState.value.copy(
                    processingTimeline = false,
                    timelineSummary = summary,
                    statusMessage = "Done: ${summary.visitCount} visits and ${summary.activityCount} activities summarised. Raw JSON was not uploaded.",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(processingTimeline = false, error = "Timeline processing failed: ${e.message}")
            }
        }
    }
}
