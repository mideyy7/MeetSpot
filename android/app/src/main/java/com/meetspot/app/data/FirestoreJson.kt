package com.meetspot.app.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Converts between kotlinx.serialization's [JsonElement] tree and the plain
 * `Map`/`List`/primitive shape the Firestore Android SDK reads and writes
 * natively. Used to store [com.meetspot.app.data.remote.dto.RecommendResponse]
 * on a meeting room document, mirroring `updateDoc(roomRef, { results: ... })`
 * in public/app.js — the web SDK can write a plain JS object directly, but
 * our response is a typed Kotlin data class, so it has to pass through JSON
 * first.
 */
object FirestoreJson {

    /** [JsonElement] -> Firestore-writable value (nested Map/List/primitives). */
    fun toFirestoreValue(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonObject -> element.mapValues { (_, value) -> toFirestoreValue(value) }
        is JsonArray -> element.map { toFirestoreValue(it) }
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.booleanOrNull != null -> element.booleanOrNull
            element.longOrNull != null -> element.longOrNull
            element.doubleOrNull != null -> element.doubleOrNull
            else -> element.content
        }
    }

    /** Firestore-read value (nested Map/List/primitives) -> [JsonElement]. */
    @Suppress("UNCHECKED_CAST")
    fun fromFirestoreValue(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Map<*, *> -> JsonObject((value as Map<String, Any?>).mapValues { fromFirestoreValue(it.value) })
        is List<*> -> JsonArray(value.map { fromFirestoreValue(it) })
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }
}
