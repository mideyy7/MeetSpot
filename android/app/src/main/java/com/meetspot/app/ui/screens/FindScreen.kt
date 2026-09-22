package com.meetspot.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.meetspot.app.ui.LocalAppContainer
import com.meetspot.app.ui.MAX_MINUTES_OPTIONS
import com.meetspot.app.ui.TRAVEL_MODES
import com.meetspot.app.ui.components.DateTimeField
import com.meetspot.app.ui.components.ResultsPanel
import com.meetspot.app.ui.theme.Green
import com.meetspot.app.ui.theme.Muted
import com.meetspot.app.ui.theme.Orange

/** Mirrors the "Find a spot" tab in public/index.html / public/app.js. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindScreen() {
    val container = LocalAppContainer.current
    val viewModel: FindViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FindViewModel(container.recommendRepository, container.locationHelper) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("NO MORE “WHERE SHOULD WE MEET?”", color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("A place that works for both of you", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            Text(
                "Tell us where you're coming from and what you're looking for. We'll find real places with fair journey times.",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            LocationField(
                label = "A · Your starting point",
                value = uiState.personA,
                onValueChange = viewModel::onPersonAChange,
                onUseLocation = { viewModel.useCurrentLocation(LocationField.PERSON_A) },
                locating = uiState.locatingField == LocationField.PERSON_A,
            )
            LocationField(
                label = "B · Their starting point",
                value = uiState.personB,
                onValueChange = viewModel::onPersonBChange,
                onUseLocation = { viewModel.useCurrentLocation(LocationField.PERSON_B) },
                locating = uiState.locatingField == LocationField.PERSON_B,
            )

            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("What do you want to find?") },
                placeholder = { Text("e.g. a relaxed Japanese restaurant") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            )

            DateTimeField(value = uiState.meetingTime, onChange = viewModel::onMeetingTimeChange, modifier = Modifier.padding(bottom = 12.dp))

            TravelModeDropdown(uiState.travelMode, viewModel::onTravelModeChange)

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

            Button(
                onClick = viewModel::submit,
                enabled = !uiState.submitting && uiState.personA.isNotBlank() && uiState.personB.isNotBlank() && uiState.query.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.submitting) "Searching…" else "Find our spot →")
            }

            if (uiState.submitting) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }

        uiState.results?.let { results ->
            item {
                ResultsPanel(
                    data = results,
                    participantNames = listOf("A", "B"),
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun LocationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onUseLocation: () -> Unit,
    locating: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text("e.g. UCL, London") },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onUseLocation, enabled = !locating) {
            Text(if (locating) "Finding your location…" else "◎ Use my current location", color = Green, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelModeDropdown(selected: String, onSelect: (String) -> Unit) {
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
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TRAVEL_MODES.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}
