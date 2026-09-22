package com.meetspot.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meetspot.app.ui.theme.Ink
import com.meetspot.app.ui.theme.Paper

/** Mirrors the three `.page-tabs` in public/index.html. */
enum class AppTab(val label: String) {
    Find("Find a spot"),
    Group("Group room"),
    Profile("Profile"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    currentTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = {
                    Text(buildAnnotatedBrand())
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper, titleContentColor = Ink),
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == currentTab,
                        onClick = { onSelectTab(tab) },
                        icon = { Icon(iconFor(tab), contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding -> content(Modifier.padding(padding)) }
}

private fun iconFor(tab: AppTab) = when (tab) {
    AppTab.Find -> Icons.Filled.Search
    AppTab.Group -> Icons.Filled.Groups
    AppTab.Profile -> Icons.Filled.Person
}

private fun buildAnnotatedBrand() = "● MeetSpot"
