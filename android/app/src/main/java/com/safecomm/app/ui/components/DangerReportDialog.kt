package com.safecomm.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.safecomm.app.data.model.AlertType
import com.safecomm.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Danger Report Dialog with Two-Stage False-Trigger Protection:
 * 1. Explicit Category Selection (Intent confirmation)
 * 2. 2-Second Hold-to-Confirm with dynamic progress fill
 *
 * UX Justification:
 * In emergency and high-stress situations, users often accidentally press panic buttons.
 * Requiring a sustained 2-second press with continuous visual/haptic feedback
 * ensures the action is deliberate while remaining under the threshold of panic latency.
 */
@Composable
fun DangerReportDialog(
    onDismiss: () -> Unit,
    onConfirmAlert: (type: AlertType, description: String, durationMinutes: Int) -> Unit,
    onHapticTick: () -> Unit = {},
    currentLanguage: com.safecomm.app.ui.i18n.AppLanguage = com.safecomm.app.ui.i18n.AppLanguage.EN
) {
    var selectedType by remember { mutableStateOf(AlertType.SHELLING) }
    var selectedDurationMinutes by remember { mutableIntStateOf(30) } // Default: 30 minutes
    var description by remember { mutableStateOf("") }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
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
                Text(
                    text = com.safecomm.app.ui.i18n.I18n.chooseDangerDesc(currentLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TelegramTextSecondary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Danger Categories 3x2 Grid
                val categories = AlertType.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in categories.chunked(3)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        .height(76.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) catColor else TelegramDivider,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedType = cat },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) catColor.copy(alpha = 0.2f) else TelegramDarkCard
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = cat.icon, fontSize = 26.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = com.safecomm.app.ui.i18n.I18n.alertTypeName(currentLanguage, cat.rawValue),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TelegramTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                                .height(38.dp)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Optional description input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            com.safecomm.app.ui.i18n.I18n.dangerDetailsHint(currentLanguage),
                            color = TelegramTextSecondary,
                            fontSize = 13.sp
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

                Spacer(modifier = Modifier.height(18.dp))

                // HOLD TO CONFIRM BUTTON (False-trigger protection)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TelegramDarkCard)
                        .border(1.dp, AlertShellingRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .pointerInput(selectedType, description, selectedDurationMinutes) {
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
                                            onConfirmAlert(selectedType, description, selectedDurationMinutes)
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
                            letterSpacing = 0.5.sp
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
