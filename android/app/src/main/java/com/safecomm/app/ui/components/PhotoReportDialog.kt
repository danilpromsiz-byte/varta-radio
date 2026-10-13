package com.safecomm.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.safecomm.app.data.model.AlertType
import com.safecomm.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Photo Fixation Dialog:
 * Captures image of danger with GPS coordinates and distributes to all community members.
 */
@Composable
fun PhotoReportDialog(
    photoUri: Uri?,
    onLaunchCamera: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmReport: (type: AlertType, description: String) -> Unit,
    onHapticTick: () -> Unit = {},
    currentLanguage: com.safecomm.app.ui.i18n.AppLanguage = com.safecomm.app.ui.i18n.AppLanguage.EN
) {
    var selectedType by remember { mutableStateOf(AlertType.OTHER) }
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

                Spacer(modifier = Modifier.height(12.dp))

                // Photo Preview or Camera Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TelegramDarkCard)
                        .clickable { onLaunchCamera() },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = PhotoActionBlue,
                                modifier = Modifier.size(48.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                // Danger Type Selection
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
                    for (cat in AlertType.entries) {
                        val isSelected = cat == selectedType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PhotoActionBlue else TelegramDarkCard)
                                .clickable { selectedType = cat },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = cat.icon, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(com.safecomm.app.ui.i18n.I18n.photoDetailsHint(currentLanguage), color = TelegramTextSecondary, fontSize = 13.sp) },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Hold to Confirm Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TelegramDarkCard)
                        .border(1.dp, PhotoActionBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .pointerInput(selectedType, description, photoUri) {
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
                                            onConfirmReport(selectedType, description)
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
