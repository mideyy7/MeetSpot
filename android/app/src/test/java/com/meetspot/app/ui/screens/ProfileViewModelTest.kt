package com.meetspot.app.ui.screens

import com.google.firebase.auth.FirebaseUser
import com.meetspot.app.MainDispatcherRule
import com.meetspot.app.data.model.ParticipantProfileSnapshot
import com.meetspot.app.data.repository.FakeAuthRepository
import com.meetspot.app.data.repository.FakeProfileRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun user(uid: String, name: String = "Abdul"): FirebaseUser = mock {
        on { this.uid } doReturn uid
        on { displayName } doReturn name
        on { email } doReturn "$name@example.com"
    }

    @Test
    fun `no user means no profile is loaded`() = runTest {
        val viewModel = ProfileViewModel(FakeAuthRepository(), FakeProfileRepository())
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.user)
        assertEquals(emptyMap<String, String>(), viewModel.uiState.value.preferences)
    }

    @Test
    fun `signing in loads the saved profile`() = runTest {
        val authRepository = FakeAuthRepository()
        val profileRepository = FakeProfileRepository().apply {
            profile = ParticipantProfileSnapshot(preferences = mapOf("dietary" to "halal", "budget" to "flexible"))
        }
        val viewModel = ProfileViewModel(authRepository, profileRepository)

        authRepository.setUser(user("uid-1"))
        advanceUntilIdle()

        assertEquals("uid-1", viewModel.uiState.value.user?.uid)
        assertEquals("halal", viewModel.uiState.value.preferences["dietary"])
        assertEquals("flexible", viewModel.uiState.value.budget)
    }

    @Test
    fun `savePreferences merges field edits with budget and atmosphere`() = runTest {
        val authRepository = FakeAuthRepository(user("uid-1"))
        val profileRepository = FakeProfileRepository()
        val viewModel = ProfileViewModel(authRepository, profileRepository)
        advanceUntilIdle()

        viewModel.onPreferenceFieldChange("dietary", "vegetarian")
        viewModel.onBudgetChange("budget")
        viewModel.savePreferences()
        advanceUntilIdle()

        val saved = profileRepository.savedPreferences.single()
        assertEquals("vegetarian", saved["dietary"])
        assertEquals("budget", saved["budget"])
        assertEquals("Your preferences are saved.", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun `processTimelineJson saves a summary and surfaces the status message`() = runTest {
        val authRepository = FakeAuthRepository(user("uid-1"))
        val profileRepository = FakeProfileRepository()
        val viewModel = ProfileViewModel(authRepository, profileRepository)
        advanceUntilIdle()

        viewModel.processTimelineJson(
            """{"timelineObjects":[{"placeVisit":{"location":{"name":"Cafe"}}}]}"""
        )
        advanceUntilIdle()

        assertEquals(1, profileRepository.savedTimelines.size)
        assertEquals(1, viewModel.uiState.value.timelineSummary?.visitCount)
        assertEquals(
            "Done: 1 visits and 0 activities summarised. Raw JSON was not uploaded.",
            viewModel.uiState.value.statusMessage,
        )
    }

    @Test
    fun `processTimelineJson surfaces a friendly error for unsupported input`() = runTest {
        val authRepository = FakeAuthRepository(user("uid-1"))
        val viewModel = ProfileViewModel(authRepository, FakeProfileRepository())
        advanceUntilIdle()

        viewModel.processTimelineJson("""{"nothingUseful": true}""")
        advanceUntilIdle()

        assertEquals(
            "Timeline processing failed: No supported Timeline records were found",
            viewModel.uiState.value.error,
        )
    }
}
