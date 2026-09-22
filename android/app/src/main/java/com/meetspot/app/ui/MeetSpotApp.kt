package com.meetspot.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.meetspot.app.ui.components.AppScaffold
import com.meetspot.app.ui.components.AppTab
import com.meetspot.app.ui.screens.FindScreen
import com.meetspot.app.ui.screens.GroupScreen
import com.meetspot.app.ui.screens.ProfileScreen
import com.meetspot.app.ui.screens.RoomScreen

private object Routes {
    const val FIND = "find"
    const val GROUP = "group"
    const val PROFILE = "profile"
    const val ROOM = "room/{roomId}"
    fun room(id: String) = "room/$id"
}

/** Root composable, mirroring the three page-tabs + `/r/:id` room route in public/index.html. */
@Composable
fun MeetSpotApp(startRoomId: String? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = when (currentRoute) {
        Routes.GROUP, Routes.ROOM -> AppTab.Group
        Routes.PROFILE -> AppTab.Profile
        else -> AppTab.Find
    }

    AppScaffold(
        currentTab = currentTab,
        onSelectTab = { tab ->
            val route = when (tab) {
                AppTab.Find -> Routes.FIND
                AppTab.Group -> Routes.GROUP
                AppTab.Profile -> Routes.PROFILE
            }
            navController.navigate(route) {
                popUpTo(Routes.FIND) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
    ) { contentModifier ->
        NavHost(
            navController = navController,
            startDestination = startRoomId?.let { Routes.room(it) } ?: Routes.FIND,
            modifier = contentModifier,
        ) {
            composable(Routes.FIND) { FindScreen() }
            composable(Routes.GROUP) {
                GroupScreen(onRoomCreated = { id -> navController.navigate(Routes.room(id)) })
            }
            composable(Routes.PROFILE) { ProfileScreen() }
            composable(Routes.ROOM) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("roomId") ?: return@composable
                RoomScreen(roomId = id)
            }
        }
    }
}
