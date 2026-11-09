package com.safecomm.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationHelper(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    init {
        // Instantly initialize with cached position from previous session so the map
        // starts right at the user's location without waiting for satellite lock.
        try {
            val prefs = context.getSharedPreferences("varta_location_prefs", Context.MODE_PRIVATE)
            val savedLat = prefs.getFloat("last_lat", 0f).toDouble()
            val savedLng = prefs.getFloat("last_lng", 0f).toDouble()
            if (savedLat != 0.0 && savedLng != 0.0) {
                val cached = Location("cached").apply {
                    latitude = savedLat
                    longitude = savedLng
                }
                _currentLocation.value = cached
                Log.d("LocationHelper", "Initialized with cached location: $savedLat, $savedLng")
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Failed to load cached location: ${e.message}")
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.latitude != 0.0 && location.longitude != 0.0) {
                _currentLocation.value = location
                saveLocation(location.latitude, location.longitude)
                Log.d("LocationHelper", "GPS updated: ${location.latitude}, ${location.longitude}")
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun saveLocation(lat: Double, lng: Double) {
        try {
            if (lat != 0.0 && lng != 0.0) {
                context.getSharedPreferences("varta_location_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putFloat("last_lat", lat.toFloat())
                    .putFloat("last_lng", lng.toFloat())
                    .apply()
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Failed to save location: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            // Get last known location immediately from all providers
            val lastGps = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
            val lastNet = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
            val lastPassive = try { locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (_: Exception) { null }
            val bestLast = lastGps ?: lastNet ?: lastPassive
            if (bestLast != null && bestLast.latitude != 0.0 && bestLast.longitude != 0.0) {
                _currentLocation.value = bestLast
                saveLocation(bestLast.latitude, bestLast.longitude)
                Log.d("LocationHelper", "Using provider lastKnown: ${bestLast.latitude}, ${bestLast.longitude}")
            }

            // Register continuous updates with high responsiveness
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    3f,
                    locationListener
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    5f,
                    locationListener
                )
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Location update error: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (_: Exception) {}
    }
}
