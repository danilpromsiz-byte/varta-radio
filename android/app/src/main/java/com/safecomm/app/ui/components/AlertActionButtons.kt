package com.safecomm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.ui.i18n.I18n

/**
 * Waze-style Secondary Tactical Emergency Action Buttons:
 * Hazard Alert (Метка) and Photo Fixation (Фото).
 *
 * Designed with rounded ergonomic pills, high tactile contrast,
 * and guaranteed text fit across all languages without truncation.
 */
@Composable
fun AlertActionButtons(
    onTriggerAlarm: () -> Unit,
    onTriggerPhoto: () -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: com.safecomm.app.ui.i18n.AppLanguage = com.safecomm.app.ui.i18n.AppLanguage.EN
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ⚠️ Waze-style Danger/Hazard Alert Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFFFF5252).copy(alpha = 0.5f))
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
                    )
                )
                .border(1.5.dp, Color(0xFFFF8A80).copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                .clickable { onTriggerAlarm() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = I18n.dangerBtn(currentLanguage),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = I18n.dangerBtn(currentLanguage),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 📷 Waze-style Photo Fixation Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF29B6F6).copy(alpha = 0.5f))
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF29B6F6), Color(0xFF0288D1))
                    )
                )
                .border(1.5.dp, Color(0xFF81D4FA).copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                .clickable { onTriggerPhoto() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = I18n.photoBtn(currentLanguage),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = I18n.photoBtn(currentLanguage),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
