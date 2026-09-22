package com.meetspot.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.meetspot.app.data.model.MeetingRoom
import com.meetspot.app.data.model.Participant
import com.meetspot.app.data.model.toFirestoreMap
import com.meetspot.app.data.model.toMeetingRoom
import com.meetspot.app.data.remote.dto.RecommendResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Mirrors the `meetings` collection reads/writes in public/app.js's Group
 * room feature: `addDoc` (create), `onSnapshot` (live updates while people
 * join), and the two `updateDoc` calls (adding a participant, then saving
 * the final results once everyone has joined).
 */
interface GroupRoomRepository {
    suspend fun createRoom(
        organizerUid: String,
        organizerName: String,
        meetingTimeIso: String,
        travelMode: String,
        maxMinutes: Int,
        expectedParticipants: Int,
        organizer: Participant,
    ): Result<String>

    /** Live updates for a room, mirroring `onSnapshot(roomRef, ...)`. Emits `null` if the room doesn't exist. */
    fun observeRoom(id: String): Flow<Result<MeetingRoom?>>

    suspend fun addParticipant(id: String, participant: Participant): Result<Unit>

    suspend fun saveResults(id: String, results: RecommendResponse, selectedQuery: String): Result<Unit>
}

class GroupRoomRepositoryImpl(private val firestore: FirebaseFirestore) : GroupRoomRepository {

    private fun roomDoc(id: String) = firestore.collection("meetings").document(id)

    override suspend fun createRoom(
        organizerUid: String,
        organizerName: String,
        meetingTimeIso: String,
        travelMode: String,
        maxMinutes: Int,
        expectedParticipants: Int,
        organizer: Participant,
    ): Result<String> = try {
        val data = mapOf(
            "organizerUid" to organizerUid,
            "organizerName" to organizerName,
            "meetingTime" to meetingTimeIso,
            "travelMode" to travelMode,
            "maxMinutes" to maxMinutes,
            "expectedParticipants" to expectedParticipants,
            "participants" to listOf(organizer.toFirestoreMap()),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        val ref = firestore.collection("meetings").add(data).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeRoom(id: String): Flow<Result<MeetingRoom?>> = callbackFlow {
        val registration = roomDoc(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            val room = snapshot?.takeIf { it.exists() }?.data?.toMeetingRoom(id)
            trySend(Result.success(room))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addParticipant(id: String, participant: Participant): Result<Unit> = try {
        roomDoc(id).update("participants", FieldValue.arrayUnion(participant.toFirestoreMap())).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun saveResults(id: String, results: RecommendResponse, selectedQuery: String): Result<Unit> = try {
        roomDoc(id).update(
            mapOf(
                "results" to results.toFirestoreMap(),
                "selectedQuery" to selectedQuery,
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
