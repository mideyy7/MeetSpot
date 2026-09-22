package com.meetspot.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(initialUser: FirebaseUser? = null) : AuthRepository {
    private val userFlow = MutableStateFlow(initialUser)
    var signInResult: Result<FirebaseUser> = Result.failure(IllegalStateException("not configured"))
    var signOutCallCount = 0

    override val currentUser: FirebaseUser? get() = userFlow.value

    override fun authState(): Flow<FirebaseUser?> = userFlow

    override suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        signInResult.onSuccess { userFlow.value = it }
        return signInResult
    }

    override fun signOut() {
        signOutCallCount++
        userFlow.value = null
    }

    fun setUser(user: FirebaseUser?) {
        userFlow.value = user
    }
}
