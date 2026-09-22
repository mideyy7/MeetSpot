package com.meetspot.app.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.meetspot.app.BuildConfig
import com.meetspot.app.R
import com.meetspot.app.data.remote.ApiClient
import com.meetspot.app.data.repository.AuthRepository
import com.meetspot.app.data.repository.AuthRepositoryImpl
import com.meetspot.app.data.repository.GroupRoomRepository
import com.meetspot.app.data.repository.GroupRoomRepositoryImpl
import com.meetspot.app.data.repository.ProfileRepository
import com.meetspot.app.data.repository.ProfileRepositoryImpl
import com.meetspot.app.data.repository.RecommendRepository
import com.meetspot.app.data.repository.RecommendRepositoryImpl

/**
 * Minimal hand-rolled DI container — no Hilt/Dagger needed at this app's size.
 * One instance lives on [com.meetspot.app.MeetSpotApplication] for the process
 * lifetime; ViewModels receive dependencies via `viewModelFactory { initializer { ... } }`.
 */
class AppContainer(context: Context) {

    private val okHttpClient = ApiClient.buildOkHttpClient()
    private val apiService = ApiClient.buildApiService(BuildConfig.DEFAULT_SERVER_BASE_URL, okHttpClient)
    val recommendRepository: RecommendRepository = RecommendRepositoryImpl(apiService)

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val authRepository: AuthRepository = AuthRepositoryImpl(
        auth = firebaseAuth,
        webClientId = context.getString(R.string.default_web_client_id),
    )
    val profileRepository: ProfileRepository = ProfileRepositoryImpl(firestore)
    val groupRoomRepository: GroupRoomRepository = GroupRoomRepositoryImpl(firestore)
    val locationHelper: LocationHelper = LocationHelperImpl(context)
}
