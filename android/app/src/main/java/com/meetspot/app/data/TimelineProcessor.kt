package com.meetspot.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

@Serializable
data class PlaceCount(val value: String, val count: Int)

@Serializable
data class TimelineSummary(
    val visitCount: Int,
    val activityCount: Int,
    val rawLocationRecordCount: Int,
    val topPlaces: List<PlaceCount>,
    val activityTypes: List<PlaceCount>,
    val processedAt: String,
    val rawFileStored: Boolean = false,
)

/**
 * Mirrors `processTimeline` in public/app.js: summarizes a Google Timeline/
 * Location History JSON export into visit/activity counts and top places,
 * entirely on-device. The raw export is never uploaded — only this summary
 * is persisted to Firestore, matching the web app's privacy behavior.
 */
object TimelineProcessor {

    private val json = Json { ignoreUnknownKeys = true }

    class NoRecordsFoundException : Exception("No supported Timeline records were found")

    fun process(rawJson: String): TimelineSummary {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val timelineObjects = root.arrayOrEmpty("timelineObjects")
        val semanticSegments = root.arrayOrEmpty("semanticSegments")
        val locations = root.arrayOrEmpty("locations")

        val placeNames = LinkedHashMap<String, Int>()
        val activityTypes = LinkedHashMap<String, Int>()
        var visitCount = 0
        var activityCount = 0

        for (item in timelineObjects) {
            val obj = item.jsonObject
            obj["placeVisit"]?.jsonObject?.let { placeVisit ->
                visitCount++
                val location = placeVisit["location"]?.jsonObject
                val name = location?.stringOrNull("name") ?: location?.stringOrNull("address")
                if (name != null) placeNames.increment(name)
            }
            obj["activitySegment"]?.jsonObject?.let { segment ->
                activityCount++
                segment.stringOrNull("activityType")?.let { activityTypes.increment(it) }
            }
        }

        for (item in semanticSegments) {
            val obj = item.jsonObject
            val visit = obj["visit"]?.jsonObject ?: obj["placeVisit"]?.jsonObject
            val activity = obj["activity"]?.jsonObject ?: obj["activitySegment"]?.jsonObject
            if (visit != null) {
                visitCount++
                val topCandidate = visit["topCandidate"]?.jsonObject
                val name = topCandidate?.stringOrNull("placeId")
                    ?: topCandidate?.stringOrNull("semanticType")
                    ?: visit["location"]?.jsonObject?.stringOrNull("name")
                if (name != null) placeNames.increment(name)
            }
            if (activity != null) {
                activityCount++
                val type = activity["topCandidate"]?.jsonObject?.stringOrNull("type")
                    ?: activity.stringOrNull("activityType")
                if (type != null) activityTypes.increment(type)
            }
        }

        val summary = TimelineSummary(
            visitCount = visitCount,
            activityCount = activityCount,
            rawLocationRecordCount = locations.size,
            topPlaces = placeNames.top(12),
            activityTypes = activityTypes.top(10),
            processedAt = Instant.now().toString(),
            rawFileStored = false,
        )
        if (summary.visitCount == 0 && summary.activityCount == 0 && summary.rawLocationRecordCount == 0) {
            throw NoRecordsFoundException()
        }
        return summary
    }

    private fun JsonObject.arrayOrEmpty(key: String): JsonArray =
        (this[key] as? JsonArray) ?: JsonArray(emptyList())

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key])?.jsonPrimitive?.contentOrNull()

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
        if (this.isString) content else null

    private fun LinkedHashMap<String, Int>.increment(key: String) {
        this[key] = (this[key] ?: 0) + 1
    }

    private fun LinkedHashMap<String, Int>.top(limit: Int): List<PlaceCount> =
        entries.sortedByDescending { it.value }.take(limit).map { PlaceCount(it.key, it.value) }
}
