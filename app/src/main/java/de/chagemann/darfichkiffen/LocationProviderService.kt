package de.chagemann.darfichkiffen

import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

class LocationProviderService @Inject constructor(
    private val locationProviderClient: FusedLocationProviderClient
) {
    @SuppressLint("MissingPermission")
    suspend fun awaitLastLocation(): Location? {
        val locationRequest = CurrentLocationRequest.Builder()
            .setMaxUpdateAgeMillis(30.seconds.inWholeMilliseconds).build()

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            try {
                locationProviderClient.getCurrentLocation(locationRequest, cancellationTokenSource.token)
                    .addOnSuccessListener { location: Location? ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("LocationProviderService", "Failed to get current location")
                        continuation.resume(null)
                    }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            } catch (e: SecurityException) {
                Log.e("LocationProviderService", "Called without permission ")
                continuation.resume(null)
            }
        }
    }
}