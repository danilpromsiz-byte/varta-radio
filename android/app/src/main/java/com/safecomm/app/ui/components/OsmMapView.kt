package com.safecomm.app.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import com.safecomm.app.data.model.Alert
import com.safecomm.app.ui.theme.TelegramBlue
import com.safecomm.app.ui.theme.TelegramDarkSurface
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapView(
    userLat: Double,
    userLng: Double,
    alerts: List<Alert>,
    onAlertClick: (Alert) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var userMarkerInstance by remember { mutableStateOf<Marker?>(null) }
    var hasCenteredInitial by remember { mutableStateOf(false) }

    // 1. Initial centering ONLY ONCE when valid location is acquired
    LaunchedEffect(userLat, userLng) {
        val map = mapViewInstance ?: return@LaunchedEffect
        if (!hasCenteredInitial && userLat != 0.0 && userLng != 0.0) {
            map.controller.setCenter(GeoPoint(userLat, userLng))
            map.controller.setZoom(15.0)
            hasCenteredInitial = true
        }
    }

    // 2. Smoothly update User Location Marker WITHOUT camera jumping or clearing whole map
    LaunchedEffect(userLat, userLng) {
        val map = mapViewInstance ?: return@LaunchedEffect
        if (userLat != 0.0 && userLng != 0.0) {
            val userPoint = GeoPoint(userLat, userLng)
            if (userMarkerInstance == null) {
                val newMarker = Marker(map).apply {
                    position = userPoint
                    title = com.safecomm.app.ui.i18n.I18n.myLocationTooltip(context)
                    icon = createCircleDrawable(context, AndroidColor.parseColor("#2AABEE"), "📍")
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(newMarker)
                userMarkerInstance = newMarker
            } else {
                userMarkerInstance?.position = userPoint
            }
            map.invalidate()
        }
    }

    // 3. Update Alert Danger Markers when alerts list changes
    LaunchedEffect(alerts) {
        val map = mapViewInstance ?: return@LaunchedEffect

        // Remove all markers except user marker
        val userMarker = userMarkerInstance
        map.overlays.clear()
        if (userMarker != null) {
            map.overlays.add(userMarker)
        }

        for (alert in alerts) {
            // Ignore (0.0, 0.0) invalid coordinates
            if (alert.latitude == 0.0 && alert.longitude == 0.0) continue

            val point = GeoPoint(alert.latitude, alert.longitude)
            val color = when (alert.typedCategory.rawValue) {
                "shelling" -> AndroidColor.parseColor("#E53935")
                "fire" -> AndroidColor.parseColor("#FF9800")
                "medical" -> AndroidColor.parseColor("#4CAF50")
                "police" -> AndroidColor.parseColor("#2196F3")
                "military" -> AndroidColor.parseColor("#FFC107")
                else -> AndroidColor.parseColor("#9E9E9E")
            }

            val catLabel = com.safecomm.app.ui.i18n.I18n.alertTypeName(context, alert.typedCategory.rawValue)
            val marker = Marker(map).apply {
                position = point
                title = "${alert.typedCategory.icon} $catLabel"
                snippet = alert.description.ifBlank { "${com.safecomm.app.ui.i18n.I18n.reportedByLabel(context)}: ${alert.senderNick}" }
                icon = createCircleDrawable(context, color, alert.typedCategory.icon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    onAlertClick(alert)
                    true
                }
            }
            map.overlays.add(marker)
        }

        map.invalidate()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    controller.setZoom(15.0)
                    val startPoint = if (userLat != 0.0 && userLng != 0.0) {
                        GeoPoint(userLat, userLng)
                    } else {
                        GeoPoint(50.4501, 30.5234) // Default fallback
                    }
                    controller.setCenter(startPoint)
                    mapViewInstance = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Action Button: "Моё местоположение" (Manual smooth re-centering)
        FloatingActionButton(
            onClick = {
                val map = mapViewInstance ?: return@FloatingActionButton
                if (userLat != 0.0 && userLng != 0.0) {
                    map.controller.animateTo(GeoPoint(userLat, userLng))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(44.dp),
            shape = CircleShape,
            containerColor = TelegramDarkSurface,
            contentColor = TelegramBlue
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Моё местоположение",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Creates custom circular colored marker icon with danger emoji
 */
private fun createCircleDrawable(context: Context, color: Int, emoji: String): Drawable {
    val size = 96
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color
        style = Paint.Style.FILL
    }
    // Outer shadow circle
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

    // White border
    val strokePaint = Paint().apply {
        isAntiAlias = true
        this.color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, strokePaint)

    // Emoji text
    val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(emoji, size / 2f, size / 2f + 14, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
