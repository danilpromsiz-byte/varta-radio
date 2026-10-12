package com.safecomm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.safecomm.app.data.model.Alert
import com.safecomm.app.ui.theme.*
import com.safecomm.app.util.GeoUtils

@Composable
fun AlertDetailDialog(
    alert: Alert,
    userLat: Double,
    userLng: Double,
    baseUrl: String = "",
    onResolve: (Alert) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val distance = GeoUtils.distanceKm(userLat, userLng, alert.latitude, alert.longitude)
    val distText = GeoUtils.formatDistance(distance)

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = alert.typedCategory.icon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = com.safecomm.app.ui.i18n.I18n.alertTypeName(context, alert.typedCategory.rawValue),
                            style = MaterialTheme.typography.titleLarge,
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = com.safecomm.app.ui.i18n.I18n.close(context), tint = TelegramTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Info Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(com.safecomm.app.ui.i18n.I18n.distanceLabel(context), fontSize = 11.sp, color = TelegramTextSecondary)
                            Text(distText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(com.safecomm.app.ui.i18n.I18n.reportedByLabel(context), fontSize = 11.sp, color = TelegramTextSecondary)
                            Text(alert.senderNick.ifBlank { "User" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TelegramTextPrimary)
                        }
                    }
                }

                if (alert.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(com.safecomm.app.ui.i18n.I18n.descriptionLabel(context), fontSize = 11.sp, color = TelegramTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(alert.description, fontSize = 13.sp, color = TelegramTextPrimary)
                        }
                    }
                }

                // Photo preview if available
                if (alert.photoPath.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val fullPhotoUrl = if (alert.photoPath.startsWith("http")) alert.photoPath else "$baseUrl${alert.photoPath}"
                    AsyncImage(
                        model = fullPhotoUrl,
                        contentDescription = "Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Resolve Alert button
                Button(
                    onClick = { onResolve(alert) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertMedicalGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(com.safecomm.app.ui.i18n.I18n.resolveAlertBtn(context), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text(com.safecomm.app.ui.i18n.I18n.close(context), color = TelegramTextSecondary)
                }
            }
        }
    }
}
