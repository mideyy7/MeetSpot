package com.meetspot.app.data.repository

import com.meetspot.app.data.model.MeetingRoom
import com.meetspot.app.data.model.Participant
import com.meetspot.app.data.remote.dto.RecommendResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGroupRoomRepository : GroupRoomRepository {
    var createResult: Result<String> = Result.success("room-1")
    var addParticipantResult: Result<Unit> = Result.success(Unit)
    var saveResultsResult: Result<Unit> = Result.success(Unit)
    val roomFlow = MutableStateFlow<Result<MeetingRoom?>>(Result.success(null))

    val createdRooms = mutableListOf<Participant>()
    val addedParticipants = mutableListOf<Participant>()
    val savedResults = mutableListOf<Pair<RecommendResponse, String>>()

    override suspend fun createRoom(
        organizerUid: String,
        organizerName: String,
        meetingTimeIso: String,
        travelMode: String,
        maxMinutes: Int,
        expectedParticipants: Int,
        organizer: Participant,
    ): Result<String> {
        createdRooms += organizer
        return createResult
    }

    override fun observeRoom(id: String): Flow<Result<MeetingRoom?>> = roomFlow

    override suspend fun addParticipant(id: String, participant: Participant): Result<Unit> {
        addedParticipants += participant
        return addParticipantResult
    }

    override suspend fun saveResults(id: String, results: RecommendResponse, selectedQuery: String): Result<Unit> {
        savedResults += results to selectedQuery
        return saveResultsResult
    }
}
