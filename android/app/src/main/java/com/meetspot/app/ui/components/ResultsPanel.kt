package com.meetspot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetspot.app.data.remote.dto.PlaceRecommendation
import com.meetspot.app.data.remote.dto.RecommendResponse
import com.meetspot.app.ui.theme.Green
import com.meetspot.app.ui.theme.Ink
import com.meetspot.app.ui.theme.Line
import com.meetspot.app.ui.theme.Muted
import com.meetspot.app.ui.theme.Orange
import kotlin.math.roundToInt

/**
 * Mirrors `renderResults` in public/app.js: the AI pick banner (if Gemini
 * produced one), the weather summary, and the ranked place cards with each
 * participant's journey time.
 */
@Composable
fun ResultsPanel(data: RecommendResponse, participantNames: List<String>, modifier: Modifier = Modifier) {
    if (data.recommendations.isEmpty()) {
        Text(
            "No places fit the journey limit. Try a broader request or longer time.",
            color = Muted,
            modifier = modifier,
        )
        return
    }

    val ordered = data.aiDecision?.let { decision ->
        data.recommendations.sortedByDescending { it.id == decision.selectedPlaceId }
    } ?: data.recommendations

    Column(modifier = modifier) {
        data.aiDecision?.let { decision ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Ink),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("✦ GEMINI GROUP PICK", color = Orange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(decision.selectedPlace, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.padding(top = 6.dp))
                    Text(decision.headline, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Text(decision.reason, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    Text("Trade-off: ${decision.tradeoff}", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        data.weather?.let { weather ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${weather.condition} · ${weather.temperatureC?.roundToInt() ?: "—"}°C · ${weather.rainChance}% rain",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(weather.advice, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        } ?: Text("Weather unavailable", color = Muted, modifier = Modifier.padding(bottom = 16.dp))

        Text("BEST COMPROMISES", color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Your meeting spots", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp, top = 2.dp))

        ordered.forEachIndexed { index, place ->
            PlaceCard(index + 1, place, participantNames, modifier = Modifier.padding(bottom = 10.dp))
        }
    }
}

@Composable
private fun PlaceCard(rank: Int, place: PlaceRecommendation, participantNames: List<String>, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "$rank",
                    color = Muted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Line, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(place.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        place.rating?.let {
                            Text("★ $it (${place.ratingCount ?: 0})", color = Muted, fontSize = 12.sp)
                        }
                    }
                    place.address?.let { Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 10.dp)) {
                place.journeys.forEachIndexed { journeyIndex, journey ->
                    val name = participantNames.getOrNull(journeyIndex) ?: "Person ${journeyIndex + 1}"
                    Text(
                        "$name  ${journey.minutes} min · ${journey.distanceKm} km",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                    )
                }
            }

            Text(place.explanation, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))

            place.mapsUrl?.let { url ->
                TextButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.padding(top = 4.dp)) {
                    Text("View in Maps ↗", color = Green)
                }
            }
        }
    }
}
