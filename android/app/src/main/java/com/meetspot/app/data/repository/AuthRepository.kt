package com.meetspot.app.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Auth + Google Sign-In, mirroring the `onAuthStateChanged` /
 * `signInWithPopup(auth, googleProvider)` / `signOut(auth)` calls in
 * public/app.js.
 */
interface AuthRepository {
    val currentUser: FirebaseUser?
    fun authState(): Flow<FirebaseUser?>
    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser>
    fun signOut()
}

/**
 * Sign-in uses Credential Manager (the current recommended replacement for
 * the deprecated GoogleSignInClient), exchanging the Google ID token for a
 * Firebase credential via [GoogleAuthProvider]. Requires a real
 * `google-services.json` with this app's OAuth web client id registered in
 * the Firebase console — see android/README.md.
 */
class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val webClientId: String,
) : AuthRepository {
    override val currentUser: FirebaseUser? get() = auth.currentUser

    override fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = CredentialManager.create(context).getCredential(context, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: return Result.failure(IllegalStateException("Sign-in returned no user"))
            Result.success(user)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}
