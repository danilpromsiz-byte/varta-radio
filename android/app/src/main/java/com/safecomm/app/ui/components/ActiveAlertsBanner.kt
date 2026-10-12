package com.safecomm.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "АКТИВНЫЕ ТРЕВОГИ (${alerts.size})",
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
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, catColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable { onAlertClick(alert) },
                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = alert.typedCategory.icon, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = alert.typedCategory.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                            Text(
                                text = distText,
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
