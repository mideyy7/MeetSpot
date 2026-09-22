package com.meetspot.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire types for the backend's `/api/recommend` and legacy `/api/meetings`
 * routes, mirroring lib/recommend.js's `buildRecommendations` output and
 * server.js/app/api/meetings' request/response shapes field-for-field.
 */

@Serializable
data class LatLng(val lat: Double, val lng: Double)

@Serializable
data class Origin(val address: String, val location: LatLng)

@Serializable
data class Weather(
    val condition: String,
    val temperatureC: Double? = null,
    val rainChance: Int = 0,
    val windKph: Double = 0.0,
    val icon: String? = null,
    val difficult: Boolean = false,
    val advice: String,
)

@Serializable
data class Journey(val minutes: Int, val distanceKm: Double)

@Serializable
data class PlaceRecommendation(
    val id: String,
    val name: String,
    val address: String? = null,
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val priceLevel: String? = null,
    val mapsUrl: String? = null,
    val openNow: Boolean? = null,
    val location: PlaceLatLng? = null,
    val journeys: List<Journey>,
    val longest: Int,
    val difference: Int,
    val score: Double,
    val explanation: String,
)

// The Places API (New) location shape (`latitude`/`longitude`), distinct from
// the plain `lat`/`lng` used for participant origins.
@Serializable
data class PlaceLatLng(val latitude: Double, val longitude: Double)

@Serializable
data class AiDecision(
    val selectedPlaceId: String,
    val selectedPlace: String,
    val headline: String,
    val reason: String,
    val tradeoff: String,
    val model: String? = null,
)

@Serializable
data class RecommendResponse(
    val origins: List<Origin> = emptyList(),
    val weather: Weather? = null,
    val recommendations: List<PlaceRecommendation> = emptyList(),
    val aiDecision: AiDecision? = null,
    val maxMinutes: Int? = null,
    val travelMode: String? = null,
)

@Serializable
data class RecommendRequest(
    val personA: String? = null,
    val personB: String? = null,
    val participants: List<ParticipantInput>? = null,
    val query: String,
    val meetingTime: String,
    val travelMode: String,
    val maxMinutes: Int,
)

@Serializable
data class ParticipantInput(
    val name: String? = null,
    val location: String,
    val preference: String? = null,
    val profile: ParticipantProfileInput? = null,
)

/**
 * Mirrors the `profile` object app.js attaches to each participant
 * (`safeProfileSnapshot()`), which the backend forwards untouched into the
 * Gemini prompt (`chooseWithGemini`'s `people` mapping) — not used for
 * routing/places, only for the final AI group decision.
 */
@Serializable
data class ParticipantProfileInput(
    val preferences: Map<String, String> = emptyMap(),
    val timelineSummary: com.meetspot.app.data.TimelineSummary? = null,
)

@Serializable
data class ApiErrorBody(val error: String? = null)

// ── Legacy /api/meetings/:id share-link flow ─────────────────────────────

@Serializable
data class MeetingSummary(
    val id: String,
    val organizerName: String,
    val guestName: String? = null,
    val query: String,
    val travelMode: String,
    val maxMinutes: Int,
    val meetingTime: String,
    val status: String,
    val results: RecommendResponse? = null,
)

@Serializable
data class JoinMeetingRequest(val guestName: String, val guestLocation: String)

@Serializable
data class JoinMeetingResponse(val status: String, val results: RecommendResponse)
