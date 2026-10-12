package com.safecomm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.ui.theme.AlarmActionRed
import com.safecomm.app.ui.theme.PhotoActionBlue

/**
 * Secondary Emergency Action Buttons: Alarm (Тревога) and Photo Fixation (Фото).
 *
 * UX Justification:
 * Positioned in the lower portion of the screen directly below the main PTT button,
 * within immediate thumb reach. Medium height (54dp) ensures effortless activation
 * during high stress while preventing accidental taps compared to full-screen triggers.
 */
@Composable
fun AlertActionButtons(
    onTriggerAlarm: () -> Unit,
    onTriggerPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 🚨 Panic / Alert Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = AlarmActionRed)
                .clip(RoundedCornerShape(16.dp))
                .background(AlarmActionRed)
                .clickable { onTriggerAlarm() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Тревога",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ТРЕВОГА",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // 📷 Photo Fixation Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = PhotoActionBlue)
                .clip(RoundedCornerShape(16.dp))
                .background(PhotoActionBlue)
                .clickable { onTriggerPhoto() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Фотофиксация",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ФОТО",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
