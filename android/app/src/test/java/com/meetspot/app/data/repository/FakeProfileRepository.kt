package com.meetspot.app.data.repository

import com.meetspot.app.data.TimelineSummary
import com.meetspot.app.data.model.ParticipantProfileSnapshot

class FakeProfileRepository : ProfileRepository {
    var profile: ParticipantProfileSnapshot? = null
    val savedPreferences = mutableListOf<Map<String, String>>()
    val savedTimelines = mutableListOf<TimelineSummary>()

    override suspend fun loadProfile(uid: String): ParticipantProfileSnapshot? = profile

    override suspend fun savePreferences(
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
        preferences: Map<String, String>,
    ) {
        savedPreferences += preferences
    }

    override suspend fun saveTimelineSummary(uid: String, summary: TimelineSummary) {
        savedTimelines += summary
    }
}
