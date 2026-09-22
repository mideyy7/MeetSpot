package com.meetspot.app.ui.screens

import com.google.firebase.auth.FirebaseUser
import com.meetspot.app.MainDispatcherRule
import com.meetspot.app.data.FakeLocationHelper
import com.meetspot.app.data.model.MeetingRoom
import com.meetspot.app.data.model.Participant
import com.meetspot.app.data.remote.dto.RecommendResponse
import com.meetspot.app.data.repository.FakeGroupRoomRepository
import com.meetspot.app.data.repository.FakeProfileRepository
import com.meetspot.app.data.repository.FakeRecommendRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class RoomViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun user(uid: String): FirebaseUser = mock { on { this.uid } doReturn uid }

    private fun room(expected: Int, participants: List<Participant>) = MeetingRoom(
        id = "room-1",
        organizerUid = "uid-organizer",
        organizerName = "Abdul",
        meetingTime = "2026-01-01T10:00:00Z",
        travelMode = "TRANSIT",
        maxMinutes = 30,
        expectedParticipants = expected,
        participants = participants,
    )

    private fun participant(uid: String, preference: String = "coffee") =
        Participant(uid, "Name-$uid", "Location-$uid", preference)

    private fun newViewModel(
        groupRoomRepository: FakeGroupRoomRepository,
        recommendRepository: FakeRecommendRepository = FakeRecommendRepository(),
        profileRepository: FakeProfileRepository = FakeProfileRepository(),
        currentUser: FirebaseUser? = user("uid-2"),
    ) = RoomViewModel("room-1", groupRoomRepository, recommendRepository, profileRepository, FakeLocationHelper()) { currentUser }

    @Test
    fun `join rejects a user who already joined`() = runTest {
        val groupRoomRepository = FakeGroupRoomRepository()
        groupRoomRepository.roomFlow.value = Result.success(room(2, listOf(participant("uid-2"))))
        val viewModel = newViewModel(groupRoomRepository)
        advanceUntilIdle()

        viewModel.onGuestNameChange("Sarah")
        viewModel.onGuestLocationChange("London Bridge")
        viewModel.onGuestPreferenceChange("bowling")
        viewModel.join()

        assertEquals("You have already joined this room.", viewModel.uiState.value.error)
        assertTrue(groupRoomRepository.addedParticipants.isEmpty())
    }

    @Test
    fun `join rejects when the room is already full`() = runTest {
        val groupRoomRepository = FakeGroupRoomRepository()
        groupRoomRepository.roomFlow.value = Result.success(room(2, listOf(participant("uid-a"), participant("uid-b"))))
        val viewModel = newViewModel(groupRoomRepository)
        advanceUntilIdle()

        viewModel.onGuestNameChange("Sarah")
        viewModel.onGuestLocationChange("London Bridge")
        viewModel.onGuestPreferenceChange("bowling")
        viewModel.join()

        assertEquals("This room already has everyone it was created for.", viewModel.uiState.value.error)
        assertTrue(groupRoomRepository.addedParticipants.isEmpty())
    }

    @Test
    fun `joining when more participants are still expected reports how many remain`() = runTest {
        val groupRoomRepository = FakeGroupRoomRepository()
        groupRoomRepository.roomFlow.value = Result.success(room(3, listOf(participant("uid-a"))))
        val viewModel = newViewModel(groupRoomRepository)
        advanceUntilIdle()

        viewModel.onGuestNameChange("Sarah")
        viewModel.onGuestLocationChange("London Bridge")
        viewModel.onGuestPreferenceChange("bowling")
        viewModel.join()
        advanceUntilIdle()

        assertEquals(1, groupRoomRepository.addedParticipants.size)
        assertEquals("You joined. Waiting for 1 more participant…", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun `joining the last expected participant triggers recommend and saves results`() = runTest {
        val groupRoomRepository = FakeGroupRoomRepository()
        groupRoomRepository.roomFlow.value = Result.success(room(2, listOf(participant("uid-a", "coffee"))))
        val recommendRepository = FakeRecommendRepository().apply {
            recommendResult = Result.success(RecommendResponse())
        }
        val viewModel = newViewModel(groupRoomRepository, recommendRepository)
        advanceUntilIdle()

        viewModel.onGuestNameChange("Sarah")
        viewModel.onGuestLocationChange("London Bridge")
        viewModel.onGuestPreferenceChange("coffee")
        viewModel.join()
        advanceUntilIdle()

        assertEquals(1, recommendRepository.groupCalls.size)
        assertEquals(2, recommendRepository.groupCalls.first().size)
        assertEquals(1, groupRoomRepository.savedResults.size)
        assertEquals("coffee", groupRoomRepository.savedResults.first().second) // deduped combined preference
        assertNull(viewModel.uiState.value.statusMessage)
    }
}
