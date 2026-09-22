package com.meetspot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.meetspot.app.ui.LocalAppContainer
import com.meetspot.app.ui.MeetSpotApp
import com.meetspot.app.ui.theme.MeetSpotTheme

/**
 * Single-activity host. If the app is opened via a `/r/{id}` share link (see
 * the VIEW intent-filter in AndroidManifest.xml), the room id is extracted
 * from the deep link and used as the nav graph's start destination —
 * mirroring the `roomMatch`/`roomQuery` handling at the bottom of
 * public/app.js.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MeetSpotApplication).container
        val startRoomId = intent?.data?.pathSegments
            ?.takeIf { it.size == 2 && it[0] == "r" }
            ?.get(1)

        setContent {
            MeetSpotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalAppContainer provides container) {
                        MeetSpotApp(startRoomId = startRoomId)
                    }
                }
            }
        }
    }
}
