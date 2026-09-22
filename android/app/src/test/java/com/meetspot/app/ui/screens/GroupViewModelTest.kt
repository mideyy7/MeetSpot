package com.meetspot.app.ui.screens

import com.google.firebase.auth.FirebaseUser
import com.meetspot.app.MainDispatcherRule
import com.meetspot.app.data.FakeLocationHelper
import com.meetspot.app.data.model.ParticipantProfileSnapshot
import com.meetspot.app.data.repository.FakeGroupRoomRepository
import com.meetspot.app.data.repository.FakeProfileRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class GroupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun user(uid: String): FirebaseUser = mock { on { this.uid } doReturn uid }

    private fun newViewModel(
        groupRoomRepository: FakeGroupRoomRepository = FakeGroupRoomRepository(),
        profileRepository: FakeProfileRepository = FakeProfileRepository(),
        currentUser: FirebaseUser? = user("uid-1"),
    ) = GroupViewModel(groupRoomRepository, profileRepository, FakeLocationHelper()) { currentUser }

    @Test
    fun `createRoom requires name, location, and preference`() {
        val viewModel = newViewModel()
        viewModel.onOrganizerNameChange("Abdul")
        // location and preference left blank
        viewModel.createRoom()

        assertEquals(
            "Add your name, your location, the request, and meeting time first.",
            viewModel.uiState.value.error,
        )
    }

    @Test
    fun `createRoom requires the user to be signed in`() {
        val viewModel = newViewModel(currentUser = null)
        viewModel.onOrganizerNameChange("Abdul")
        viewModel.onLocationChange("UCL")
        viewModel.onPreferenceChange("coffee")
        viewModel.createRoom()

        assertEquals("Sign in with Google first so the group room can be saved.", viewModel.uiState.value.error)
    }

    @Test
    fun `createRoom attaches the caller's saved profile to the organizer`() = runTest {
        val groupRoomRepository = FakeGroupRoomRepository()
        val profileRepository = FakeProfileRepository().apply {
            profile = ParticipantProfileSnapshot(preferences = mapOf("dietary" to "vegan"))
        }
        val viewModel = newViewModel(groupRoomRepository, profileRepository)
        viewModel.onOrganizerNameChange("Abdul")
        viewModel.onLocationChange("UCL")
        viewModel.onPreferenceChange("coffee")

        viewModel.createRoom()
        advanceUntilIdle()

        val organizer = groupRoomRepository.createdRooms.single()
        assertEquals("uid-1", organizer.uid)
        assertEquals("vegan", organizer.profile?.preferences?.get("dietary"))
        assertEquals("room-1", viewModel.uiState.value.createdRoomId)
        assertNull(viewModel.uiState.value.error)
    }
}
