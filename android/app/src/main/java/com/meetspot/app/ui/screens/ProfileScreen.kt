package com.meetspot.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meetspot.app.ui.LocalAppContainer
import com.meetspot.app.ui.theme.Green
import com.meetspot.app.ui.theme.Muted
import com.meetspot.app.ui.theme.Orange
import java.io.BufferedReader
import java.io.InputStreamReader

private val BUDGET_OPTIONS = listOf("budget" to "Budget-friendly", "moderate" to "Moderate", "flexible" to "Flexible")
private val ATMOSPHERE_OPTIONS = listOf("quiet" to "Quiet and relaxed", "balanced" to "Balanced", "lively" to "Lively and social")

/** Mirrors the Profile tab in public/index.html / public/app.js. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ProfileViewModel(container.authRepository, container.profileRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    val timelinePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val text = BufferedReader(InputStreamReader(stream)).readText()
            viewModel.processTimelineJson(text)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            if (uiState.user == null) {
                Text("YOUR MEETSPOT PROFILE", color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Make every suggestion feel like you", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                Button(onClick = { viewModel.signIn(context) }, enabled = !uiState.signingIn) {
                    Text(if (uiState.signingIn) "Opening Google sign-in…" else "Sign in with Google")
                }
                uiState.error?.let { Text(it, color = Orange, modifier = Modifier.padding(top = 12.dp)) }
                return@item
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Signed in as ${uiState.user?.displayName ?: uiState.user?.email}", color = Muted, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            OutlinedButton(onClick = viewModel::signOut) { Text("Sign out") }

            Text(
                "Set hard requirements and preferences once. You can change them whenever you want.",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )

            PROFILE_FIELDS.forEach { (key, label) ->
                OutlinedTextField(
                    value = uiState.preferences[key] ?: "",
                    onValueChange = { viewModel.onPreferenceFieldChange(key, it) },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                )
            }

            LabeledDropdown("Typical budget", BUDGET_OPTIONS, uiState.budget, viewModel::onBudgetChange)
            LabeledDropdown("Preferred atmosphere", ATMOSPHERE_OPTIONS, uiState.atmosphere, viewModel::onAtmosphereChange, modifier = Modifier.padding(top = 10.dp, bottom = 16.dp))

            uiState.statusMessage?.let { Text(it, color = Green, modifier = Modifier.padding(bottom = 8.dp)) }
            uiState.error?.let { Text(it, color = Orange, modifier = Modifier.padding(bottom = 8.dp)) }

            Button(onClick = viewModel::savePreferences, enabled = !uiState.saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (uiState.saving) "Saving…" else "Save my preferences →")
            }

            Text("Optional: learn from your Google Timeline", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
            Text(
                "The JSON is processed once on this device. Only a compact summary is saved; the raw file is never uploaded.",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            OutlinedButton(onClick = { timelinePicker.launch("application/json") }, enabled = !uiState.processingTimeline) {
                Text(if (uiState.processingTimeline) "Processing…" else "Process Timeline JSON")
            }

            uiState.timelineSummary?.let { summary ->
                Text(
                    "✓ Stored: ${summary.visitCount} visits and ${summary.activityCount} activities. Raw JSON was not uploaded.",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (summary.topPlaces.isNotEmpty()) {
                    Text(
                        summary.topPlaces.joinToString(", ") { "${it.value} ×${it.count}" },
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val text = options.firstOrNull { it.first == selected }?.second ?: selected
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(text = { Text(optionLabel) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}
