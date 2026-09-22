package com.meetspot.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meetspot.app.ui.LocalAppContainer
import com.meetspot.app.ui.components.ResultsPanel
import com.meetspot.app.ui.theme.Green
import com.meetspot.app.ui.theme.Muted
import com.meetspot.app.ui.theme.Orange

/** Mirrors the invited/room view in public/app.js's `loadGroupRoom`. */
@Composable
fun RoomScreen(roomId: String) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: RoomViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                RoomViewModel(
                    roomId,
                    container.groupRoomRepository,
                    container.recommendRepository,
                    container.profileRepository,
                    container.locationHelper,
                ) { container.authRepository.currentUser }
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val currentUid = container.authRepository.currentUser?.uid

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            when {
                uiState.loading -> CircularProgressIndicator()
                uiState.room == null -> Text(uiState.error ?: "This meeting room does not exist.", color = Orange)
                else -> {
                    val room = uiState.room!!
                    val alreadyJoined = room.participants.any { it.uid == currentUid }

                    Text("YOU'VE BEEN INVITED", color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("${room.organizerName} wants to meet", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                    Text(
                        "${room.participants.size} of ${room.expectedParticipants} joined · maximum ${room.maxMinutes} minutes",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    if (room.participants.size < room.expectedParticipants) {
                        OutlinedButton(onClick = { shareRoomLink(context, roomId) }, modifier = Modifier.padding(bottom = 16.dp)) {
                            Text("Share room link")
                        }
                    }

                    if (room.participants.isNotEmpty()) {
                        Text(
                            if (room.participants.size >= room.expectedParticipants) "Everyone is here" else "Waiting for ${room.expectedParticipants - room.participants.size} more",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        room.participants.forEach { participant ->
                            Text("${participant.name} — ${participant.preference}", color = Muted, fontSize = 13.sp)
                        }
                    }

                    if (!alreadyJoined && room.results == null) {
                        OutlinedTextField(
                            value = uiState.guestName,
                            onValueChange = viewModel::onGuestNameChange,
                            label = { Text("Your name") },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp),
                        )
                        OutlinedTextField(
                            value = uiState.guestLocation,
                            onValueChange = viewModel::onGuestLocationChange,
                            label = { Text("Your starting point") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        )
                        OutlinedTextField(
                            value = uiState.guestPreference,
                            onValueChange = viewModel::onGuestPreferenceChange,
                            label = { Text("What would you like to do?") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        )

                        uiState.statusMessage?.let { Text(it, color = Muted, modifier = Modifier.padding(bottom = 8.dp)) }
                        uiState.error?.let { Text(it, color = Orange, modifier = Modifier.padding(bottom = 8.dp)) }

                        Button(onClick = viewModel::join, enabled = !uiState.joining, modifier = Modifier.fillMaxWidth()) {
                            Text(if (uiState.joining) "Joining…" else "Join room and add my vote →")
                        }
                    }

                    room.results?.let { results ->
                        ResultsPanel(
                            data = results,
                            participantNames = room.participants.map { it.name },
                            modifier = Modifier.padding(top = 20.dp),
                        )
                    }
                }
            }
        }
    }
}
