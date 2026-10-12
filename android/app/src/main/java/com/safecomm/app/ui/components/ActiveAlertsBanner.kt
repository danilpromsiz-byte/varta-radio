package com.safecomm.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.data.model.Alert
import com.safecomm.app.ui.theme.*
import com.safecomm.app.util.GeoUtils

@Composable
fun ActiveAlertsBanner(
    alerts: List<Alert>,
    userLat: Double,
    userLng: Double,
    onAlertClick: (Alert) -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "АКТИВНЫЕ МЕТКИ (${alerts.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TelegramTextSecondary,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(alerts, key = { it.id }) { alert ->
                val distance = GeoUtils.distanceKm(userLat, userLng, alert.latitude, alert.longitude)
                val distText = GeoUtils.formatDistance(distance)
                val catColor = when (alert.typedCategory.rawValue) {
                    "shelling" -> AlertShellingRed
                    "fire" -> AlertFireOrange
                    "medical" -> AlertMedicalGreen
                    "police" -> AlertPoliceBlue
                    "military" -> AlertMilitaryYellow
                    else -> AlertOtherGray
                }

                Card(
                    modifier = Modifier
                        .width(230.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, catColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable { onAlertClick(alert) },
                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        // Top Row: Icon + Label + Author Callsign
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(text = alert.typedCategory.icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = alert.typedCategory.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = catColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = alert.senderNick.ifBlank { "Аноним" },
                                fontSize = 10.sp,
                                color = TelegramTextSecondary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        // Description preview (wrapped, readable)
                        if (alert.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = alert.description,
                                fontSize = 11.sp,
                                color = TelegramTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bottom Row: Distance badge + Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📍 ", fontSize = 10.sp)
                                Text(
                                    text = distText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (distance in 0.0..2.0) AlertShellingRed else TelegramBlue
                                )
                            }
                            Text(
                                text = alert.createdAt.ifBlank { "Только что" },
                                fontSize = 10.sp,
                                color = TelegramTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
