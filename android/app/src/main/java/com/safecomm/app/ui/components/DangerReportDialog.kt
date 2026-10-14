package com.safecomm.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.safecomm.app.data.model.AlertType
import com.safecomm.app.data.model.AlertUsageTracker
import com.safecomm.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Danger Report Dialog with Two-Stage False-Trigger Protection:
 * 1. Explicit Category Selection (Most frequent shown first and larger)
 * 2. 2-Second Hold-to-Confirm with dynamic progress fill
 * 3. Movable marker location support
 */
@Composable
fun DangerReportDialog(
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    userLat: Double = 0.0,
    userLng: Double = 0.0,
    initialType: AlertType? = null,
    initialDurationMinutes: Int = 30,
    initialDescription: String = "",
    onStateChange: ((type: AlertType, duration: Int, desc: String) -> Unit)? = null,
    onMoveMarkerOnMap: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirmAlert: (type: AlertType, description: String, durationMinutes: Int, targetLat: Double, targetLng: Double) -> Unit,
    onHapticTick: () -> Unit = {},
    currentLanguage: com.safecomm.app.ui.i18n.AppLanguage = com.safecomm.app.ui.i18n.AppLanguage.EN
) {
    val context = LocalContext.current
    val sortedCategories = remember { AlertUsageTracker.getSortedCategories(context) }
    val topCategory = sortedCategories.first()
    val otherCategories = sortedCategories.drop(1)

    var selectedType by remember { mutableStateOf(initialType ?: topCategory) }
    var selectedDurationMinutes by remember { mutableIntStateOf(initialDurationMinutes) }
    var description by remember { mutableStateOf(initialDescription) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(vertical = 8.dp),
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
                        text = com.safecomm.app.ui.i18n.I18n.placeDangerTitle(currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        color = AlertShellingRed,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = com.safecomm.app.ui.i18n.I18n.close(currentLanguage),
                            tint = TelegramTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TOP (MOST USED) CATEGORY: First & Sleek
                val isTopSelected = selectedType == topCategory
                val topCatColor = when (topCategory) {
                    AlertType.SHELLING -> AlertShellingRed
                    AlertType.FIRE -> AlertFireOrange
                    AlertType.MEDICAL -> AlertMedicalGreen
                    AlertType.POLICE -> AlertPoliceBlue
                    AlertType.MILITARY -> AlertMilitaryYellow
                    AlertType.OTHER -> AlertOtherGray
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .border(
                            width = if (isTopSelected) 2.dp else 1.dp,
                            color = if (isTopSelected) topCatColor else TelegramDivider,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedType = topCategory },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTopSelected) topCatColor.copy(alpha = 0.22f) else TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AlertCategoryIcon(type = topCategory, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = com.safecomm.app.ui.i18n.I18n.alertTypeName(currentLanguage, topCategory.rawValue),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTopSelected) Color.White else TelegramTextPrimary
                                )
                                Text(
                                    text = com.safecomm.app.ui.i18n.I18n.frequentBadge(currentLanguage),
                                    fontSize = 10.sp,
                                    color = Color(0xFFFFD600),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (isTopSelected) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(topCatColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Other 5 Categories (in clean 3-col rows)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in otherCategories.chunked(3)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (cat in row) {
                                val isSelected = cat == selectedType
                                val catColor = when (cat) {
                                    AlertType.SHELLING -> AlertShellingRed
                                    AlertType.FIRE -> AlertFireOrange
                                    AlertType.MEDICAL -> AlertMedicalGreen
                                    AlertType.POLICE -> AlertPoliceBlue
                                    AlertType.MILITARY -> AlertMilitaryYellow
                                    AlertType.OTHER -> AlertOtherGray
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) catColor else TelegramDivider,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedType = cat },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) catColor.copy(alpha = 0.2f) else TelegramDarkCard
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        AlertCategoryIcon(type = cat, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = com.safecomm.app.ui.i18n.I18n.alertTypeName(currentLanguage, cat.rawValue),
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TelegramTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (row.size < 3) {
                                for (i in 0 until (3 - row.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Alert Expiry / Duration Selector
                Text(
                    text = com.safecomm.app.ui.i18n.I18n.alertDurationLabel(currentLanguage),
                    fontSize = 12.sp,
                    color = TelegramTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                val durations = listOf(
                    15 to com.safecomm.app.ui.i18n.I18n.duration15m(currentLanguage),
                    30 to com.safecomm.app.ui.i18n.I18n.duration30m(currentLanguage),
                    60 to com.safecomm.app.ui.i18n.I18n.duration1h(currentLanguage),
                    120 to com.safecomm.app.ui.i18n.I18n.duration2h(currentLanguage),
                    240 to com.safecomm.app.ui.i18n.I18n.duration4h(currentLanguage)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durations.forEach { (mins, label) ->
                        val isSelected = selectedDurationMinutes == mins
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) TelegramBlue else TelegramDivider.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedDurationMinutes = mins },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TelegramBlue.copy(alpha = 0.25f) else TelegramDarkCard
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TelegramTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Optional description input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            com.safecomm.app.ui.i18n.I18n.dangerDetailsHint(currentLanguage),
                            color = TelegramTextSecondary,
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TelegramTextPrimary,
                        unfocusedTextColor = TelegramTextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider,
                        focusedContainerColor = TelegramDarkCard,
                        unfocusedContainerColor = TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Compact Location Pill Row (Clean, minimal text, tactical icons)
                val hasCustomLocation = initialLat != 0.0 && initialLng != 0.0 && (initialLat != userLat || initialLng != userLng)
                val distanceMeters = if (hasCustomLocation && userLat != 0.0 && userLng != 0.0) {
                    (com.safecomm.app.util.GeoUtils.distanceKm(userLat, userLng, initialLat, initialLng) * 1000).toInt()
                } else 0

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TelegramDarkCard)
                        .border(1.dp, if (hasCustomLocation) Color(0xFFFFD600).copy(alpha = 0.6f) else TelegramDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📍", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasCustomLocation) {
                                if (distanceMeters > 0) "+${distanceMeters}м (${com.safecomm.app.ui.i18n.I18n.fromYouLabel(currentLanguage)})" else com.safecomm.app.ui.i18n.I18n.customLocationSet(currentLanguage)
                            } else {
                                "GPS"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasCustomLocation) Color(0xFFFFD600) else TelegramTextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TelegramBlue.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            onStateChange?.invoke(selectedType, selectedDurationMinutes, description)
                            onMoveMarkerOnMap()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "🎯", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = com.safecomm.app.ui.i18n.I18n.moveMarkerShort(currentLanguage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TelegramBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HOLD TO CONFIRM BUTTON (False-trigger protection)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TelegramDarkCard)
                        .border(1.dp, AlertShellingRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .pointerInput(selectedType, description, selectedDurationMinutes, initialLat, initialLng) {
                            detectTapGestures(
                                onPress = {
                                    isHolding = true
                                    val job = coroutineScope.launch {
                                        val totalMs = 2000L
                                        val stepMs = 50L
                                        var elapsed = 0L
                                        while (elapsed < totalMs && isHolding) {
                                            delay(stepMs)
                                            elapsed += stepMs
                                            holdProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                                            if (elapsed % 300 == 0L) {
                                                onHapticTick()
                                            }
                                        }
                                        if (holdProgress >= 1.0f) {
                                            AlertUsageTracker.recordUsage(context, selectedType)
                                            val finalLat = if (initialLat != 0.0) initialLat else userLat
                                            val finalLng = if (initialLng != 0.0) initialLng else userLng
                                            onConfirmAlert(selectedType, description, selectedDurationMinutes, finalLat, finalLng)
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
                    // Animated Fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(holdProgress)
                            .background(AlertShellingRed)
                    )

                    // Text label centered
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHolding) com.safecomm.app.ui.i18n.I18n.holdingProgress(currentLanguage, (holdProgress * 100).toInt()) else com.safecomm.app.ui.i18n.I18n.holdToBroadcastPrompt(currentLanguage),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onDismiss) {
                    Text(com.safecomm.app.ui.i18n.I18n.cancel(currentLanguage), color = TelegramTextSecondary)
                }
            }
        }
    }
}
