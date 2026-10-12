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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import com.safecomm.app.data.model.Alert
import com.safecomm.app.ui.i18n.AppLanguage
import com.safecomm.app.ui.i18n.I18n
import com.safecomm.app.ui.theme.TelegramBlue
import com.safecomm.app.ui.theme.TelegramDarkSurface
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

data class AlertCluster(
    val primaryAlert: Alert,
    val count: Int,
    val latitude: Double,
    val longitude: Double,
    val allAlerts: List<Alert>
)

@Composable
fun OsmMapView(
    userLat: Double,
    userLng: Double,
    alerts: List<Alert>,
    onAlertClick: (Alert) -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: AppLanguage = AppLanguage.EN
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
                    title = I18n.myLocationTooltip(currentLanguage)
                    icon = createCleanMarkerDrawable(context, "📍", 1)
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

    // 3. Update Alert Danger Markers with Clustering & Radar Waves
    LaunchedEffect(alerts, currentLanguage) {
        val map = mapViewInstance ?: return@LaunchedEffect

        // Remove all overlays except user marker
        val userMarker = userMarkerInstance
        map.overlays.clear()
        if (userMarker != null) {
            map.overlays.add(userMarker)
        }

        val validAlerts = alerts.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        if (validAlerts.isEmpty()) {
            map.invalidate()
            return@LaunchedEffect
        }

        // Clustering: Group markers within ~60 meters
        val clusters = clusterAlerts(validAlerts, thresholdMeters = 60.0)

        // Identify the newest alert for pulsing radar waves
        val newestAlert = validAlerts.maxByOrNull { it.id }
        if (newestAlert != null) {
            val pulseOverlay = RadarPulseOverlay(GeoPoint(newestAlert.latitude, newestAlert.longitude))
            map.overlays.add(pulseOverlay)
        }

        for (cluster in clusters) {
            val alert = cluster.primaryAlert
            val point = GeoPoint(cluster.latitude, cluster.longitude)
            val catLabel = I18n.alertTypeName(currentLanguage, alert.typedCategory.rawValue)

            val marker = Marker(map).apply {
                position = point
                title = if (cluster.count > 1) {
                    "${alert.typedCategory.icon} $catLabel (x${cluster.count})"
                } else {
                    "${alert.typedCategory.icon} $catLabel"
                }
                snippet = alert.description.ifBlank { "${I18n.reportedByLabel(currentLanguage)}: ${alert.senderNick}" }
                // Clean icon without circle background, with count badge if clustered
                icon = createCleanMarkerDrawable(context, alert.typedCategory.icon, cluster.count)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
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

        // Floating Action Button: "Моё местоположение" (elevated above bottom dock)
        FloatingActionButton(
            onClick = {
                val map = mapViewInstance ?: return@FloatingActionButton
                if (userLat != 0.0 && userLng != 0.0) {
                    map.controller.animateTo(GeoPoint(userLat, userLng))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 140.dp, end = 16.dp)
                .size(46.dp),
            shape = CircleShape,
            containerColor = TelegramDarkSurface,
            contentColor = TelegramBlue
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = I18n.myLocationTooltip(currentLanguage),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Animated radiating radar wave circles expanding outward from the latest alert.
 */
class RadarPulseOverlay(private val point: GeoPoint) : Overlay() {
    private var phase = 0f
    private val ringPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = AndroidColor.parseColor("#E53935")
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val pj = mapView.projection
        val screenPoint = android.graphics.Point()
        pj.toPixels(point, screenPoint)

        val maxRadius = 120f
        for (i in 0..2) {
            val progress = ((phase + i * 0.33f) % 1.0f)
            val radius = 18f + progress * maxRadius
            val alpha = ((1.0f - progress) * 220).toInt().coerceIn(0, 255)
            ringPaint.strokeWidth = 3.5f * (1.0f - progress * 0.5f)
            ringPaint.alpha = alpha
            canvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), radius, ringPaint)
        }

        phase = (phase + 0.025f) % 1.0f
        mapView.postInvalidateDelayed(40)
    }
}

/**
 * Clusters nearby alerts within [thresholdMeters] into a single cluster.
 */
private fun clusterAlerts(rawAlerts: List<Alert>, thresholdMeters: Double = 60.0): List<AlertCluster> {
    val clusters = mutableListOf<AlertCluster>()
    val visited = BooleanArray(rawAlerts.size)

    for (i in rawAlerts.indices) {
        if (visited[i]) continue
        visited[i] = true
        val current = rawAlerts[i]
        val clusterGroup = mutableListOf(current)

        for (j in (i + 1) until rawAlerts.size) {
            if (!visited[j]) {
                val other = rawAlerts[j]
                val dist = com.safecomm.app.util.GeoUtils.distanceKm(current.latitude, current.longitude, other.latitude, other.longitude) * 1000.0
                if (dist <= thresholdMeters) {
                    visited[j] = true
                    clusterGroup.add(other)
                }
            }
        }

        val avgLat = clusterGroup.map { it.latitude }.average()
        val avgLng = clusterGroup.map { it.longitude }.average()
        clusters.add(AlertCluster(clusterGroup.first(), clusterGroup.size, avgLat, avgLng, clusterGroup))
    }
    return clusters
}

/**
 * Creates clean marker icon without colored background circle, with badge count if clustered.
 */
private fun createCleanMarkerDrawable(context: Context, emoji: String, clusterCount: Int = 1): Drawable {
    val size = 110
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)

    // Emoji icon directly with subtle shadow
    val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 52f
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 0f, 4f, AndroidColor.argb(200, 0, 0, 0))
    }
    canvas.drawText(emoji, size / 2f, size / 2f + 18, textPaint)

    // If cluster count > 1, draw crisp count badge in top-right corner
    if (clusterCount > 1) {
        val badgePaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#E53935")
            style = Paint.Style.FILL
        }
        val badgeStroke = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val badgeRadius = 20f
        val badgeX = size - badgeRadius - 4f
        val badgeY = badgeRadius + 4f
        canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)
        canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeStroke)

        val badgeTextPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.WHITE
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(clusterCount.toString(), badgeX, badgeY + 8f, badgeTextPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}
