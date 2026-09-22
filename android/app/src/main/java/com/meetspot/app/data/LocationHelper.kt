package com.meetspot.app.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.util.Locale

/** Mirrors `useCurrentLocation` in public/app.js: fetches a one-shot GPS fix and formats it as `lat,lng`. */
interface LocationHelper {
    suspend fun currentLocationString(): Result<String>
}

/**
 * The `lat,lng` string is recognised by the backend's `geocode()` via regex
 * and used directly instead of a Places text search.
 */
class LocationHelperImpl(private val context: Context) : LocationHelper {

    @SuppressLint("MissingPermission") // Caller is responsible for requesting the permission first.
    override suspend fun currentLocationString(): Result<String> {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                ?: return Result.failure(IllegalStateException("Could not get your location. Enter an address instead."))
            Result.success(String.format(Locale.US, "%.6f,%.6f", location.latitude, location.longitude))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
