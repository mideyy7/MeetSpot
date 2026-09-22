package com.meetspot.app.data.model

import com.meetspot.app.data.PlaceCount
import com.meetspot.app.data.TimelineSummary
import com.meetspot.app.data.remote.dto.Journey
import com.meetspot.app.data.remote.dto.PlaceRecommendation
import com.meetspot.app.data.remote.dto.RecommendResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies the Firestore raw-map <-> domain model mapping used by GroupRoomRepository. */
class MeetingRoomTest {

    @Test
    fun `toMeetingRoom parses a raw Firestore document map`() {
        val raw: Map<String, Any?> = mapOf(
            "organizerUid" to "uid-1",
            "organizerName" to "Abdul",
            "meetingTime" to "2026-01-01T10:00:00Z",
            "travelMode" to "WALK",
            "maxMinutes" to 45L, // Firestore returns Long for integers
            "expectedParticipants" to 3L,
            "participants" to listOf(
                mapOf(
                    "uid" to "uid-1",
                    "name" to "Abdul",
                    "location" to "UCL",
                    "preference" to "coffee",
                    "profile" to mapOf("preferences" to mapOf("dietary" to "vegetarian")),
                )
            ),
            "results" to null,
        )

        val room = raw.toMeetingRoom("room-1")

        assertEquals("room-1", room.id)
        assertEquals("Abdul", room.organizerName)
        assertEquals(45, room.maxMinutes)
        assertEquals(3, room.expectedParticipants)
        assertEquals(1, room.participants.size)
        assertEquals("vegetarian", room.participants.first().profile?.preferences?.get("dietary"))
        assertNull(room.results)
    }

    @Test
    fun `toMeetingRoom defaults missing optional fields`() {
        val room = emptyMap<String, Any?>().toMeetingRoom("room-2")

        assertEquals("TRANSIT", room.travelMode)
        assertEquals(30, room.maxMinutes)
        assertEquals(2, room.expectedParticipants)
        assertEquals(emptyList<Participant>(), room.participants)
    }

    @Test
    fun `participant round-trips through toFirestoreMap and toParticipant`() {
        val original = Participant(
            uid = "uid-2",
            name = "Sarah",
            location = "London Bridge",
            preference = "bowling",
            profile = ParticipantProfileSnapshot(
                preferences = mapOf("cuisines" to "Japanese"),
                timelineSummary = TimelineSummary(
                    visitCount = 5,
                    activityCount = 2,
                    rawLocationRecordCount = 0,
                    topPlaces = listOf(PlaceCount("Cafe X", 3)),
                    activityTypes = emptyList(),
                    processedAt = "2026-01-01T00:00:00Z",
                ),
            ),
        )

        val roundTripped = original.toFirestoreMap().toParticipant()

        assertEquals(original.uid, roundTripped.uid)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.preference, roundTripped.preference)
        assertEquals("Japanese", roundTripped.profile?.preferences?.get("cuisines"))
    }

    @Test
    fun `recommend response round-trips through the Firestore map conversion`() {
        val original = RecommendResponse(
            origins = emptyList(),
            weather = null,
            recommendations = listOf(
                PlaceRecommendation(
                    id = "p1",
                    name = "Blue Bottle",
                    journeys = listOf(Journey(10, 1.0), Journey(12, 1.4)),
                    longest = 12,
                    difference = 2,
                    score = 15.0,
                    explanation = "Fair option",
                )
            ),
            maxMinutes = 30,
            travelMode = "TRANSIT",
        )

        val raw = original.toFirestoreMap()
        val roundTripped = raw.toMeetingRoomResultsForTest()

        assertEquals(1, roundTripped?.recommendations?.size)
        assertEquals("Blue Bottle", roundTripped?.recommendations?.first()?.name)
        assertEquals(12, roundTripped?.recommendations?.first()?.longest)
    }
}

/** Exercises the private `toRecommendResponse` mapper indirectly via a room document. */
private fun Map<String, Any?>.toMeetingRoomResultsForTest(): RecommendResponse? =
    mapOf("results" to this).toMeetingRoom("room-test").results
