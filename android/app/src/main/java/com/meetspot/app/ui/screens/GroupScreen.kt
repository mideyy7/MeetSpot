package com.meetspot.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meetspot.app.BuildConfig
import com.meetspot.app.ui.LocalAppContainer
import com.meetspot.app.ui.GROUP_SIZE_OPTIONS
import com.meetspot.app.ui.MAX_MINUTES_OPTIONS
import com.meetspot.app.ui.TRAVEL_MODES
import com.meetspot.app.ui.components.DateTimeField
import com.meetspot.app.ui.theme.Green
import com.meetspot.app.ui.theme.Muted
import com.meetspot.app.ui.theme.Orange

/** Mirrors the "Group room" tab in public/index.html / public/app.js. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(onRoomCreated: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: GroupViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GroupViewModel(
                    container.groupRoomRepository,
                    container.profileRepository,
                    container.locationHelper,
                ) { container.authRepository.currentUser }
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createdRoomId) {
        uiState.createdRoomId?.let { onRoomCreated(it) }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("PLAN WITH EVERYONE", color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("Create one shared meeting room", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            Text(
                "You start the room. Everyone else uses the same link to add their own location and preference.",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = uiState.organizerName,
                onValueChange = viewModel::onOrganizerNameChange,
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            )
            OutlinedTextField(
                value = uiState.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text("Your starting point") },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = viewModel::useCurrentLocation, enabled = !uiState.locating) {
                Text(if (uiState.locating) "Finding your location…" else "◎ Use my current location", color = Green, fontSize = 12.sp)
            }

            GroupSizeDropdown(uiState.groupSize, viewModel::onGroupSizeChange)

            OutlinedTextField(
                value = uiState.preference,
                onValueChange = viewModel::onPreferenceChange,
                label = { Text("What would you like to do?") },
                placeholder = { Text("e.g. coffee with indoor seating") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
            )

            DateTimeField(value = uiState.meetingTime, onChange = viewModel::onMeetingTimeChange, modifier = Modifier.padding(bottom = 12.dp))

            TravelModeDropdownGroup(uiState.travelMode, viewModel::onTravelModeChange)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)) {
                MAX_MINUTES_OPTIONS.forEach { minutes ->
                    val selected = uiState.maxMinutes == minutes
                    TextButton(onClick = { viewModel.onMaxMinutesChange(minutes) }) {
                        Text("${minutes}m", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Green else Muted)
                    }
                }
            }

            uiState.error?.let {
                Text(it, color = Orange, modifier = Modifier.padding(bottom = 12.dp))
            }

            Button(onClick = viewModel::createRoom, enabled = !uiState.creating, modifier = Modifier.fillMaxWidth()) {
                Text(if (uiState.creating) "Creating…" else "Create group room →")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupSizeDropdown(selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.padding(top = 10.dp)) {
        OutlinedTextField(
            value = "$selected people",
            onValueChange = {},
            readOnly = true,
            label = { Text("How many people are meeting?") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GROUP_SIZE_OPTIONS.forEach { size ->
                DropdownMenuItem(text = { Text("$size people") }, onClick = { onSelect(size); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelModeDropdownGroup(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = TRAVEL_MODES.firstOrNull { it.first == selected }?.second ?: selected
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Travel mode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TRAVEL_MODES.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}

fun shareRoomLink(context: android.content.Context, roomId: String) {
    val url = "${BuildConfig.DEFAULT_SERVER_BASE_URL}/r/$roomId"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, "Share room link"))
}
