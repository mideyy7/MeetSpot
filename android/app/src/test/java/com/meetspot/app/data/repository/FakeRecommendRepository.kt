package com.meetspot.app.data.repository

import com.meetspot.app.data.remote.dto.JoinMeetingResponse
import com.meetspot.app.data.remote.dto.MeetingSummary
import com.meetspot.app.data.remote.dto.ParticipantInput
import com.meetspot.app.data.remote.dto.RecommendResponse

class FakeRecommendRepository : RecommendRepository {
    var recommendResult: Result<RecommendResponse> = Result.success(RecommendResponse())
    var meetingResult: Result<MeetingSummary>? = null
    var joinResult: Result<JoinMeetingResponse> = Result.failure(ApiException("not configured"))

    val pairCalls = mutableListOf<Triple<String, String, String>>() // personA, personB, query
    val groupCalls = mutableListOf<List<ParticipantInput>>()

    override suspend fun recommendForPair(
        personA: String,
        personB: String,
        query: String,
        meetingTimeIso: String,
        travelMode: String,
        maxMinutes: Int,
    ): Result<RecommendResponse> {
        pairCalls += Triple(personA, personB, query)
        return recommendResult
    }

    override suspend fun recommendForGroup(
        participants: List<ParticipantInput>,
        query: String,
        meetingTimeIso: String,
        travelMode: String,
        maxMinutes: Int,
    ): Result<RecommendResponse> {
        groupCalls += participants
        return recommendResult
    }

    override suspend fun fetchMeeting(id: String): Result<MeetingSummary> =
        meetingResult ?: Result.failure(ApiException("no meeting configured for $id"))

    override suspend fun joinMeeting(id: String, guestName: String, guestLocation: String): Result<JoinMeetingResponse> =
        joinResult
}
