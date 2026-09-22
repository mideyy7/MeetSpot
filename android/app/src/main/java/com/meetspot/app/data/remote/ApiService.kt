package com.meetspot.app.data.remote

import com.meetspot.app.data.remote.dto.JoinMeetingRequest
import com.meetspot.app.data.remote.dto.JoinMeetingResponse
import com.meetspot.app.data.remote.dto.MeetingSummary
import com.meetspot.app.data.remote.dto.RecommendRequest
import com.meetspot.app.data.remote.dto.RecommendResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit mirror of the backend's recommend + legacy invite routes
 * (server.js / app/api routes). No auth header is required — recommendations are
 * anonymous and invite links are capability URLs (server.js) or
 * self-contained signed tokens (the Next.js production routes).
 */
interface ApiService {

    @POST("api/recommend")
    suspend fun recommend(@Body body: RecommendRequest): RecommendResponse

    @GET("api/meetings/{id}")
    suspend fun fetchMeeting(@Path("id") id: String): MeetingSummary

    @POST("api/meetings/{id}/join")
    suspend fun joinMeeting(@Path("id") id: String, @Body body: JoinMeetingRequest): JoinMeetingResponse
}
