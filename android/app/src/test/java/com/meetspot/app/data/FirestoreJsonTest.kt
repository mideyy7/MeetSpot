package com.meetspot.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreJsonTest {

    private val json = Json

    @Test
    fun `converts a nested json object into plain maps and lists`() {
        val element = json.parseToJsonElement(
            """{"name":"Blue Bottle","rating":4.5,"open":true,"tags":["cafe","wifi"],"address":null}"""
        )

        val value = FirestoreJson.toFirestoreValue(element) as Map<*, *>

        assertEquals("Blue Bottle", value["name"])
        assertEquals(4.5, value["rating"])
        assertEquals(true, value["open"])
        assertEquals(listOf("cafe", "wifi"), value["tags"])
        assertEquals(null, value["address"])
    }

    @Test
    fun `round-trips integers without turning them into doubles`() {
        val element = json.parseToJsonElement("""{"count": 12}""")

        val value = FirestoreJson.toFirestoreValue(element) as Map<*, *>

        assertEquals(12L, value["count"])
    }

    @Test
    fun `converts firestore-shaped values back into JsonElement`() {
        val raw: Map<String, Any?> = mapOf("name" to "Sarah", "count" to 3L, "active" to true, "tags" to listOf("a", "b"))

        val element = FirestoreJson.fromFirestoreValue(raw)
        val roundTripped = FirestoreJson.toFirestoreValue(element) as Map<*, *>

        assertEquals("Sarah", roundTripped["name"])
        assertEquals(3L, roundTripped["count"])
        assertEquals(true, roundTripped["active"])
        assertEquals(listOf("a", "b"), roundTripped["tags"])
    }
}
