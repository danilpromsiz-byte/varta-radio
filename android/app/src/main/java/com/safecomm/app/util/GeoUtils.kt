package com.safecomm.app.util

import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if ((lat1 == 0.0 && lon1 == 0.0) || (lat2 == 0.0 && lon2 == 0.0)) return -1.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2.0) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (EARTH_RADIUS_KM * c).coerceAtLeast(0.0)
    }

    fun formatDistance(km: Double): String {
        return when {
            km < 0.0 -> "Определяется..."
            km < 0.05 -> "Рядом (< 50 м)"
            km < 1.0 -> "${(km * 1000).toInt()} м"
            km < 100.0 -> "%.1f км".format(km)
            else -> "%.0f км".format(km)
        }
    }
}
