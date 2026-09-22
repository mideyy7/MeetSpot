package com.meetspot.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Mirrors the `<select>` options in public/index.html's planner forms. */
val TRAVEL_MODES = listOf(
    "TRANSIT" to "Public transport",
    "WALK" to "Walking",
    "DRIVE" to "Driving",
    "BICYCLE" to "Cycling",
)

val MAX_MINUTES_OPTIONS = listOf(20, 30, 45, 60)

val GROUP_SIZE_OPTIONS = (2..10).toList()

/** Mirrors `defaultMeetingTime` in public/app.js: two hours from now, rounded up to the next 15 minutes. */
fun defaultMeetingTime(): ZonedDateTime {
    val now = ZonedDateTime.now().plusHours(2)
    val roundedMinute = ((now.minute / 15) + 1) * 15
    return now.withSecond(0).withNano(0).plusMinutes((roundedMinute - now.minute).toLong())
}

fun ZonedDateTime.toIso(): String = this.withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)

fun ZonedDateTime.displayFormat(): String =
    this.format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm"))
