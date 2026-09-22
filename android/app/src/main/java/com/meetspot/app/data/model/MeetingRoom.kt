package com.meetspot.app.data.model

import com.meetspot.app.data.FirestoreJson
import com.meetspot.app.data.TimelineSummary
import com.meetspot.app.data.remote.ApiClient
import com.meetspot.app.data.remote.dto.RecommendResponse
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/** Mirrors the `profile` snapshot attached to each participant in public/app.js's `safeProfileSnapshot`. */
data class ParticipantProfileSnapshot(
    val preferences: Map<String, String> = emptyMap(),
    val timelineSummary: TimelineSummary? = null,
)

/** Mirrors a participant entry in the Firestore `meetings/{id}.participants` array. */
data class Participant(
    val uid: String,
    val name: String,
    val location: String,
    val preference: String,
    val profile: ParticipantProfileSnapshot? = null,
)

/** Mirrors a Firestore `meetings/{id}` document, written/read by the Group room feature. */
data class MeetingRoom(
    val id: String,
    val organizerUid: String,
    val organizerName: String,
    val meetingTime: String,
    val travelMode: String,
    val maxMinutes: Int,
    val expectedParticipants: Int,
    val participants: List<Participant> = emptyList(),
    val results: RecommendResponse? = null,
    val selectedQuery: String? = null,
)

/** Maps Firestore's raw `DocumentSnapshot.data` shape into [MeetingRoom]. */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toMeetingRoom(id: String): MeetingRoom {
    val participantMaps = (this["participants"] as? List<*>).orEmpty()
    return MeetingRoom(
        id = id,
        organizerUid = this["organizerUid"] as? String ?: "",
        organizerName = this["organizerName"] as? String ?: "",
        meetingTime = this["meetingTime"] as? String ?: "",
        travelMode = this["travelMode"] as? String ?: "TRANSIT",
        maxMinutes = (this["maxMinutes"] as? Number)?.toInt() ?: 30,
        expectedParticipants = (this["expectedParticipants"] as? Number)?.toInt() ?: 2,
        participants = participantMaps.filterIsInstance<Map<String, Any?>>().map { it.toParticipant() },
        results = (this["results"] as? Map<String, Any?>)?.toRecommendResponse(),
        selectedQuery = this["selectedQuery"] as? String,
    )
}

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toParticipant(): Participant = Participant(
    uid = this["uid"] as? String ?: "",
    name = this["name"] as? String ?: "",
    location = this["location"] as? String ?: "",
    preference = this["preference"] as? String ?: "",
    profile = (this["profile"] as? Map<String, Any?>)?.toProfileSnapshot(),
)

fun Map<String, Any?>.toProfileSnapshot(): ParticipantProfileSnapshot = ParticipantProfileSnapshot(
    preferences = (this["preferences"] as? Map<*, *>)
        ?.entries.orEmpty()
        .associate { (key, value) -> key.toString() to value.toString() },
    timelineSummary = null, // Not needed client-side once submitted; the backend reads it from Firestore-forwarded data.
)

fun Participant.toFirestoreMap(): Map<String, Any?> = buildMap {
    put("uid", uid)
    put("name", name)
    put("location", location)
    put("preference", preference)
    profile?.let {
        put(
            "profile",
            mapOf(
                "preferences" to it.preferences,
                "timelineSummary" to it.timelineSummary?.let { summary ->
                    FirestoreJson.toFirestoreValue(ApiClient.json.encodeToJsonElement(summary))
                },
            ),
        )
    }
}

fun RecommendResponse.toFirestoreMap(): Map<String, Any?> {
    val element = ApiClient.json.encodeToJsonElement(this)
    @Suppress("UNCHECKED_CAST")
    return FirestoreJson.toFirestoreValue(element) as Map<String, Any?>
}

private fun Map<String, Any?>.toRecommendResponse(): RecommendResponse? = runCatching {
    val jsonObject = FirestoreJson.fromFirestoreValue(this).jsonObject
    ApiClient.json.decodeFromJsonElement<RecommendResponse>(jsonObject)
}.getOrNull()
