package com.safecomm.app.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.safecomm.app.data.model.AlertType
import com.safecomm.app.data.model.AlertUsageTracker
import com.safecomm.app.ui.theme.*
import com.safecomm.app.util.NormalizedArrow
import com.safecomm.app.util.PhotoMarkupHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Tactical Photo Fixation Dialog:
 * - Photo Preview with interactive markup (draw modern tactical arrows)
 * - State preservation (category never resets on location adjustments)
 * - Compact location pill & full scrollable interface
 */
@Composable
fun PhotoReportDialog(
    photoUri: Uri?,
    photoFile: File? = null,
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    userLat: Double = 0.0,
    userLng: Double = 0.0,
    initialType: AlertType? = null,
    initialDescription: String = "",
    onStateChange: ((type: AlertType, desc: String) -> Unit)? = null,
    onMoveMarkerOnMap: () -> Unit = {},
    onLaunchCamera: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmReport: (type: AlertType, description: String, targetLat: Double, targetLng: Double) -> Unit,
    onHapticTick: () -> Unit = {},
    currentLanguage: com.safecomm.app.ui.i18n.AppLanguage = com.safecomm.app.ui.i18n.AppLanguage.EN
) {
    val context = LocalContext.current
    val sortedCategories = remember { AlertUsageTracker.getSortedCategories(context) }
    var selectedType by remember { mutableStateOf(initialType ?: sortedCategories.first()) }
    var description by remember { mutableStateOf(initialDescription) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    // Tactical Arrows Markup
    val arrows = remember { mutableStateListOf<NormalizedArrow>() }
    var currentDragStart by remember { mutableStateOf<Offset?>(null) }
    var currentDragEnd by remember { mutableStateOf<Offset?>(null) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = com.safecomm.app.ui.i18n.I18n.photoFixationTitle(currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        color = PhotoActionBlue,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = com.safecomm.app.ui.i18n.I18n.close(currentLanguage),
                            tint = TelegramTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Photo Preview with Interactive Tactical Arrow Markup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TelegramDarkCard)
                        .border(1.dp, TelegramDivider, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { previewSize = it }
                        ) {
                            // 1. The Captured Image
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Captured Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // 2. Interactive Gesture Overlay for Tactical Arrows
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(previewSize) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentDragStart = offset
                                                currentDragEnd = offset
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentDragEnd = change.position
                                            },
                                            onDragEnd = {
                                                val start = currentDragStart
                                                val end = currentDragEnd
                                                val pw = previewSize.width.toFloat()
                                                val ph = previewSize.height.toFloat()
                                                if (start != null && end != null && pw > 0 && ph > 0) {
                                                    val dist = hypot(end.x - start.x, end.y - start.y)
                                                    if (dist > 16f) {
                                                        // Drawn arrow
                                                        arrows.add(
                                                            NormalizedArrow(
                                                                startX = (start.x / pw).coerceIn(0f, 1f),
                                                                startY = (start.y / ph).coerceIn(0f, 1f),
                                                                endX = (end.x / pw).coerceIn(0f, 1f),
                                                                endY = (end.y / ph).coerceIn(0f, 1f)
                                                            )
                                                        )
                                                    } else {
                                                        // Tap: place tactical arrow pointing at tap spot
                                                        val tapX = end.x
                                                        val tapY = end.y
                                                        val arrowLen = (pw * 0.18f).coerceAtLeast(45f)
                                                        val sx = (tapX - arrowLen * 0.7f).coerceAtLeast(10f)
                                                        val sy = (tapY - arrowLen * 0.7f).coerceAtLeast(10f)
                                                        arrows.add(
                                                            NormalizedArrow(
                                                                startX = (sx / pw).coerceIn(0f, 1f),
                                                                startY = (sy / ph).coerceIn(0f, 1f),
                                                                endX = (tapX / pw).coerceIn(0f, 1f),
                                                                endY = (tapY / ph).coerceIn(0f, 1f)
                                                            )
                                                        )
                                                    }
                                                }
                                                currentDragStart = null
                                                currentDragEnd = null
                                            },
                                            onDragCancel = {
                                                currentDragStart = null
                                                currentDragEnd = null
                                            }
                                        )
                                    }
                            ) {
                                val pw = size.width
                                val ph = size.height

                                fun drawTacticalArrow(sx: Float, sy: Float, ex: Float, ey: Float) {
                                    val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
                                    val arrowAngle = Math.toRadians(26.0)
                                    val headLen = (pw * 0.07f).coerceAtLeast(26f)

                                    // Line outline (black)
                                    drawLine(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        start = Offset(sx, sy),
                                        end = Offset(ex, ey),
                                        strokeWidth = 9f,
                                        cap = StrokeCap.Round
                                    )
                                    // Neon yellow line
                                    drawLine(
                                        color = Color(0xFFFFD600),
                                        start = Offset(sx, sy),
                                        end = Offset(ex, ey),
                                        strokeWidth = 5f,
                                        cap = StrokeCap.Round
                                    )

                                    // Arrowhead points
                                    val x1 = (ex - headLen * cos(angle - arrowAngle)).toFloat()
                                    val y1 = (ey - headLen * sin(angle - arrowAngle)).toFloat()
                                    val x2 = (ex - headLen * cos(angle + arrowAngle)).toFloat()
                                    val y2 = (ey - headLen * sin(angle + arrowAngle)).toFloat()

                                    val headPath = Path().apply {
                                        moveTo(ex, ey)
                                        lineTo(x1, y1)
                                        lineTo(x2, y2)
                                        close()
                                    }

                                    drawPath(
                                        path = headPath,
                                        color = Color.Black.copy(alpha = 0.85f),
                                        style = Stroke(width = 6f, join = StrokeJoin.Round)
                                    )
                                    drawPath(
                                        path = headPath,
                                        color = Color(0xFFFFD600)
                                    )
                                }

                                // Draw confirmed arrows
                                for (arr in arrows) {
                                    drawTacticalArrow(arr.startX * pw, arr.startY * ph, arr.endX * pw, arr.endY * ph)
                                }

                                // Draw arrow being dragged
                                val ds = currentDragStart
                                val de = currentDragEnd
                                if (ds != null && de != null && hypot(de.x - ds.x, de.y - ds.y) > 10f) {
                                    drawTacticalArrow(ds.x, ds.y, de.x, de.y)
                                }
                            }

                            // Re-take photo button floating in top-right
                            IconButton(
                                onClick = onLaunchCamera,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Retake",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // Empty photo placeholder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onLaunchCamera() }
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = PhotoActionBlue,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = com.safecomm.app.ui.i18n.I18n.tapToTakePhoto(currentLanguage),
                                color = TelegramTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Markup Guidance & Clear Button
                if (photoUri != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = com.safecomm.app.ui.i18n.I18n.photoMarkupHint(currentLanguage),
                            fontSize = 11.sp,
                            color = TelegramTextSecondary
                        )
                        if (arrows.isNotEmpty()) {
                            Text(
                                text = com.safecomm.app.ui.i18n.I18n.photoMarkupClear(currentLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertShellingRed,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { arrows.clear() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Danger Type Selection Chips
                Text(
                    text = com.safecomm.app.ui.i18n.I18n.photoHazardTypeLabel(currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TelegramTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (cat in sortedCategories) {
                        val isSelected = cat == selectedType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PhotoActionBlue else TelegramDarkCard)
                                .clickable {
                                    selectedType = cat
                                    onStateChange?.invoke(selectedType, description)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AlertCategoryIcon(type = cat, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        onStateChange?.invoke(selectedType, description)
                    },
                    placeholder = {
                        Text(
                            com.safecomm.app.ui.i18n.I18n.photoDetailsHint(currentLanguage),
                            color = TelegramTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TelegramTextPrimary,
                        unfocusedTextColor = TelegramTextPrimary,
                        focusedBorderColor = PhotoActionBlue,
                        unfocusedBorderColor = TelegramDivider,
                        focusedContainerColor = TelegramDarkCard,
                        unfocusedContainerColor = TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Compact Location Pill Row (Minimal text, clear pictograms)
                val hasCustomLocation = initialLat != 0.0 && initialLng != 0.0 && (initialLat != userLat || initialLng != userLng)
                val distanceMeters = if (hasCustomLocation && userLat != 0.0 && userLng != 0.0) {
                    (com.safecomm.app.util.GeoUtils.distanceKm(userLat, userLng, initialLat, initialLng) * 1000).toInt()
                } else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TelegramDarkCard)
                        .border(1.dp, if (hasCustomLocation) Color(0xFFFFD600).copy(alpha = 0.6f) else TelegramDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📍", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasCustomLocation) {
                                if (distanceMeters > 0) "+${distanceMeters}м (${com.safecomm.app.ui.i18n.I18n.fromYouLabel(currentLanguage)})" else com.safecomm.app.ui.i18n.I18n.customLocationSet(currentLanguage)
                            } else {
                                "GPS"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasCustomLocation) Color(0xFFFFD600) else TelegramTextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TelegramBlue.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            onStateChange?.invoke(selectedType, description)
                            onMoveMarkerOnMap()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "🎯", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = com.safecomm.app.ui.i18n.I18n.moveMarkerShort(currentLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TelegramBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hold to Confirm Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TelegramDarkCard)
                        .border(1.dp, PhotoActionBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .pointerInput(selectedType, description, photoUri, photoFile, initialLat, initialLng, arrows.size) {
                            detectTapGestures(
                                onPress = {
                                    isHolding = true
                                    val job = coroutineScope.launch {
                                        val totalMs = 1500L
                                        val stepMs = 50L
                                        var elapsed = 0L
                                        while (elapsed < totalMs && isHolding) {
                                            delay(stepMs)
                                            elapsed += stepMs
                                            holdProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                                            if (elapsed % 300 == 0L) onHapticTick()
                                        }
                                        if (holdProgress >= 1.0f) {
                                            AlertUsageTracker.recordUsage(context, selectedType)
                                            // Bake arrows into photo file if drawn
                                            if (photoFile != null && arrows.isNotEmpty()) {
                                                try {
                                                    PhotoMarkupHelper.bakeArrows(photoFile, arrows.toList())
                                                } catch (_: Exception) {}
                                            }
                                            val finalLat = if (initialLat != 0.0) initialLat else userLat
                                            val finalLng = if (initialLng != 0.0) initialLng else userLng
                                            onConfirmReport(selectedType, description, finalLat, finalLng)
                                        }
                                    }
                                    tryAwaitRelease()
                                    isHolding = false
                                    holdProgress = 0f
                                    job.cancel()
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(holdProgress)
                            .background(PhotoActionBlue)
                    )

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isHolding) com.safecomm.app.ui.i18n.I18n.holdingProgress(currentLanguage, (holdProgress * 100).toInt()) else com.safecomm.app.ui.i18n.I18n.holdToSendPhotoPrompt(currentLanguage),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text(com.safecomm.app.ui.i18n.I18n.cancel(currentLanguage), color = TelegramTextSecondary)
                }
            }
        }
    }
}
