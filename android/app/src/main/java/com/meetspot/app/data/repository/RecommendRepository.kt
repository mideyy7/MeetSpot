package com.meetspot.app.data.repository

import com.meetspot.app.data.remote.ApiClient
import com.meetspot.app.data.remote.ApiService
import com.meetspot.app.data.remote.dto.ApiErrorBody
import com.meetspot.app.data.remote.dto.JoinMeetingResponse
import com.meetspot.app.data.remote.dto.JoinMeetingRequest
import com.meetspot.app.data.remote.dto.MeetingSummary
import com.meetspot.app.data.remote.dto.ParticipantInput
import com.meetspot.app.data.remote.dto.RecommendRequest
import com.meetspot.app.data.remote.dto.RecommendResponse
import retrofit2.HttpException

/**
 * Domain-facing wrapper around [ApiService], mirroring the fetch calls in
 * public/app.js (`formPayload` + `/api/recommend`, `loadInvitation`'s
 * `/api/meetings/:id` + `/join`). Every call returns a [Result] whose failure
 * message follows the same precedence the web client uses:
 * `data.error || "<default message>"`.
 */
interface RecommendRepository {
    suspend fun recommendForPair(
        personA: String,
        personB: String,
        query: String,
        meetingTimeIso: String,
        travelMode: String,
        maxMinutes: Int,
    ): Result<RecommendResponse>

    suspend fun recommendForGroup(
        participants: List<ParticipantInput>,
        query: String,
        meetingTimeIso: String,
        travelMode: String,
        maxMinutes: Int,
    ): Result<RecommendResponse>

    suspend fun fetchMeeting(id: String): Result<MeetingSummary>

    suspend fun joinMeeting(id: String, guestName: String, guestLocation: String): Result<JoinMeetingResponse>
}

class RecommendRepositoryImpl(private val apiService: ApiService) : RecommendRepository {

    override suspend fun recommendForPair(
        personA: String,
        personB: String,
        query: String,
        meetingTimeIso: String,
        travelMode: String,
        maxMinutes: Int,
    ): Result<RecommendResponse> = safeCall {
        apiService.recommend(
            RecommendRequest(
                personA = personA,
                personB = personB,
                query = query,
                meetingTime = meetingTimeIso,
                travelMode = travelMode,
                maxMinutes = maxMinutes,
            )
        )
    }

    override suspend fun recommendForGroup(
        participants: List<ParticipantInput>,
        query: String,
        meetingTimeIso: String,
        travelMode: String,
        maxMinutes: Int,
    ): Result<RecommendResponse> = safeCall {
        apiService.recommend(
            RecommendRequest(
                participants = participants,
                query = query,
                meetingTime = meetingTimeIso,
                travelMode = travelMode,
                maxMinutes = maxMinutes,
            )
        )
    }

    override suspend fun fetchMeeting(id: String): Result<MeetingSummary> =
        safeCall { apiService.fetchMeeting(id) }

    override suspend fun joinMeeting(id: String, guestName: String, guestLocation: String): Result<JoinMeetingResponse> =
        safeCall { apiService.joinMeeting(id, JoinMeetingRequest(guestName, guestLocation)) }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: HttpException) {
        Result.failure(ApiException(extractServerError(e) ?: e.message() ?: "Request failed"))
    } catch (e: Exception) {
        Result.failure(ApiException(e.message ?: "Request failed"))
    }

    private fun extractServerError(e: HttpException): String? {
        val body = e.response()?.errorBody()?.string() ?: return null
        return runCatching {
            ApiClient.json.decodeFromString(ApiErrorBody.serializer(), body).error
        }.getOrNull()
    }
}

/** Thrown by [RecommendRepositoryImpl.safeCall]; `message` is always the user-facing string. */
class ApiException(message: String) : Exception(message)
