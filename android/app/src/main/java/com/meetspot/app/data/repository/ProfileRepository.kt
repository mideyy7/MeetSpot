package com.meetspot.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.meetspot.app.data.PlaceCount
import com.meetspot.app.data.TimelineSummary
import com.meetspot.app.data.model.ParticipantProfileSnapshot
import kotlinx.coroutines.tasks.await

@Suppress("UNCHECKED_CAST")
private fun Map<*, *>.toTimelineSummary(): TimelineSummary? = runCatching {
    fun placeCounts(key: String) = (this[key] as? List<*>).orEmpty()
        .filterIsInstance<Map<String, Any?>>()
        .map { PlaceCount(it["value"] as? String ?: "", (it["count"] as? Number)?.toInt() ?: 0) }
    TimelineSummary(
        visitCount = (this["visitCount"] as? Number)?.toInt() ?: 0,
        activityCount = (this["activityCount"] as? Number)?.toInt() ?: 0,
        rawLocationRecordCount = (this["rawLocationRecordCount"] as? Number)?.toInt() ?: 0,
        topPlaces = placeCounts("topPlaces"),
        activityTypes = placeCounts("activityTypes"),
        processedAt = this["processedAt"] as? String ?: "",
        rawFileStored = this["rawFileStored"] as? Boolean ?: false,
    )
}.getOrNull()

/** Mirrors the `users/{uid}` reads/writes in public/app.js (`loadProfile`, the profile form, and Timeline upload). */
interface ProfileRepository {
    suspend fun loadProfile(uid: String): ParticipantProfileSnapshot?

    suspend fun savePreferences(
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
        preferences: Map<String, String>,
    )

    suspend fun saveTimelineSummary(uid: String, summary: TimelineSummary)
}

class ProfileRepositoryImpl(private val firestore: FirebaseFirestore) : ProfileRepository {

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    override suspend fun loadProfile(uid: String): ParticipantProfileSnapshot? {
        val snapshot = userDoc(uid).get().await()
        if (!snapshot.exists()) return null
        val preferences = (snapshot.get("preferences") as? Map<*, *>)
            ?.entries.orEmpty()
            .associate { (key, value) -> key.toString() to value.toString() }
        val timeline = (snapshot.get("timelineSummary") as? Map<*, *>)?.toTimelineSummary()
        return ParticipantProfileSnapshot(preferences = preferences, timelineSummary = timeline)
    }

    override suspend fun savePreferences(
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
        preferences: Map<String, String>,
    ) {
        userDoc(uid).set(
            mapOf(
                "email" to email,
                "displayName" to displayName,
                "photoURL" to photoUrl,
                "preferences" to preferences,
                "profileCompleted" to true,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    override suspend fun saveTimelineSummary(uid: String, summary: TimelineSummary) {
        userDoc(uid).set(
            mapOf(
                "timelineSummary" to mapOf(
                    "visitCount" to summary.visitCount,
                    "activityCount" to summary.activityCount,
                    "rawLocationRecordCount" to summary.rawLocationRecordCount,
                    "topPlaces" to summary.topPlaces.map { mapOf("value" to it.value, "count" to it.count) },
                    "activityTypes" to summary.activityTypes.map { mapOf("value" to it.value, "count" to it.count) },
                    "processedAt" to summary.processedAt,
                    "rawFileStored" to false,
                ),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }
}
