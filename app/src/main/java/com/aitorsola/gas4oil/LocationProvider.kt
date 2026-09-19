package com.aitorsola.gas4oil

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class Place(val location: Location, val city: String?, val countryCode: String?)

class LocationProvider(private val context: Context) {

    val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    suspend fun current(): Place? {
        if (!hasPermission) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val location = lastKnown(manager) ?: withTimeoutOrNull(20_000) { singleUpdate(manager) }
        ?: return null
        val address = address(location)
        return Place(location, address?.locality, address?.countryCode)
    }

    private fun lastKnown(manager: LocationManager): Location? {
        if (!hasPermission) return null
        return manager.getProviders(true).mapNotNull {
            runCatching { manager.getLastKnownLocation(it) }.getOrNull()
        }.filter { System.currentTimeMillis() - it.time < 30 * 60_000 }
            .maxByOrNull { it.time }
    }

    private suspend fun singleUpdate(manager: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            if (!hasPermission) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            val providers = manager.getProviders(true) - LocationManager.PASSIVE_PROVIDER
            if (providers.isEmpty()) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            lateinit var listener: android.location.LocationListener
            listener = android.location.LocationListener { location ->
                runCatching { manager.removeUpdates(listener) }
                if (cont.isActive) cont.resume(location)
            }
            val requested = providers.count { provider ->
                runCatching {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }.isSuccess
            }
            if (requested == 0 && cont.isActive) cont.resume(null)
            cont.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
        }

    private suspend fun address(location: Location): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(8_000) {
                suspendCancellableCoroutine<Address?> { cont ->
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { list ->
                        if (cont.isActive) cont.resume(list.firstOrNull())
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            }.getOrNull()
        }
    }
}
