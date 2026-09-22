package com.meetspot.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Mirrors the intent of `processTimeline` in public/app.js's Timeline upload handler. */
class TimelineProcessorTest {

    @Test
    fun `counts visits and activities from legacy timelineObjects`() {
        val json = """
            {
              "timelineObjects": [
                { "placeVisit": { "location": { "name": "Blue Bottle Coffee" } } },
                { "placeVisit": { "location": { "name": "Blue Bottle Coffee" } } },
                { "activitySegment": { "activityType": "WALKING" } }
              ]
            }
        """.trimIndent()

        val summary = TimelineProcessor.process(json)

        assertEquals(2, summary.visitCount)
        assertEquals(1, summary.activityCount)
        assertEquals(listOf(PlaceCount("Blue Bottle Coffee", 2)), summary.topPlaces)
        assertEquals(listOf(PlaceCount("WALKING", 1)), summary.activityTypes)
        assertEquals(false, summary.rawFileStored)
    }

    @Test
    fun `counts visits and activities from newer semanticSegments`() {
        val json = """
            {
              "semanticSegments": [
                { "visit": { "topCandidate": { "semanticType": "Home" } } },
                { "activity": { "topCandidate": { "type": "IN_PASSENGER_VEHICLE" } } }
              ]
            }
        """.trimIndent()

        val summary = TimelineProcessor.process(json)

        assertEquals(1, summary.visitCount)
        assertEquals(1, summary.activityCount)
        assertEquals("Home", summary.topPlaces.first().value)
        assertEquals("IN_PASSENGER_VEHICLE", summary.activityTypes.first().value)
    }

    @Test
    fun `falls back to placeId then location name when semanticType is absent`() {
        val json = """
            {
              "semanticSegments": [
                { "visit": { "topCandidate": { "placeId": "ChIJ123" } } },
                { "visit": { "location": { "name": "Fallback Cafe" } } }
              ]
            }
        """.trimIndent()

        val summary = TimelineProcessor.process(json)

        assertEquals(setOf("ChIJ123", "Fallback Cafe"), summary.topPlaces.map { it.value }.toSet())
    }

    @Test
    fun `caps top places and activity types at 12 and 10`() {
        val visits = (1..15).joinToString(",") { """{ "placeVisit": { "location": { "name": "Place $it" } } }""" }
        val json = """{ "timelineObjects": [$visits] }"""

        val summary = TimelineProcessor.process(json)

        assertEquals(12, summary.topPlaces.size)
        assertEquals(15, summary.visitCount)
    }

    @Test
    fun `counts raw location records even without visits or activities`() {
        val json = """{ "locations": [{}, {}, {}] }"""

        val summary = TimelineProcessor.process(json)

        assertEquals(3, summary.rawLocationRecordCount)
        assertEquals(0, summary.visitCount)
    }

    @Test
    fun `throws when no supported records are found`() {
        assertThrows(TimelineProcessor.NoRecordsFoundException::class.java) {
            TimelineProcessor.process("""{ "someOtherField": true }""")
        }
    }
}
