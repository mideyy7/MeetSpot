package com.meetspot.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.meetspot.app.data.AppContainer

/** Provided once, near the Compose root, by MainActivity. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided")
}
