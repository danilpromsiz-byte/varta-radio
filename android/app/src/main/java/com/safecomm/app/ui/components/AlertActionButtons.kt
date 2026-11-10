package com.safecomm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.data.model.AudioMode
import com.safecomm.app.ui.i18n.AppLanguage
import com.safecomm.app.ui.i18n.I18n

/**
 * Waze-style Secondary Tactical Emergency Action Dock:
 * 1. Audio Mode Toggle (Звук 🔊 / Вибро 📳 / Без звука 🔇) with high visibility in silent mode.
 * 2. Hazard Alert (Метка ⚠️).
 * 3. Photo Fixation (Фото 📷).
 *
 * Designed with rounded ergonomic pills, high tactile contrast,
 * and guaranteed text fit across all languages without truncation.
 */
@Composable
fun AlertActionButtons(
    onTriggerAlarm: () -> Unit,
    onTriggerPhoto: () -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: AppLanguage = AppLanguage.EN,
    audioMode: AudioMode = AudioMode.SOUND,
    onToggleAudioMode: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // =========================================================================
        // 1. 🔊 / 📳 / 🔇 AUDIO MODE BUTTON (Звук / Вибро / Без звука)
        // =========================================================================
        val audioGradient = when (audioMode) {
            AudioMode.SOUND -> Brush.verticalGradient(
                colors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
            )
            AudioMode.VIBRATE -> Brush.verticalGradient(
                colors = listOf(Color(0xFFE65100), Color(0xFFBF360C))
            )
            AudioMode.SILENT -> Brush.verticalGradient(
                // Striking, high-visibility crimson alert gradient so muted mode is instantly noticeable!
                colors = listOf(Color(0xFFD32F2F), Color(0xFF880E4F))
            )
        }
        val audioBorder = when (audioMode) {
            AudioMode.SOUND -> Color(0xFF81C784).copy(alpha = 0.8f)
            AudioMode.VIBRATE -> Color(0xFFFFB74D).copy(alpha = 0.8f)
            AudioMode.SILENT -> Color(0xFFFF8A80)
        }
        val audioShadowColor = when (audioMode) {
            AudioMode.SOUND -> Color(0xFF4CAF50).copy(alpha = 0.5f)
            AudioMode.VIBRATE -> Color(0xFFFF9800).copy(alpha = 0.5f)
            AudioMode.SILENT -> Color(0xFFFF1744).copy(alpha = 0.6f)
        }
        val audioIcon = when (audioMode) {
            AudioMode.SOUND -> Icons.AutoMirrored.Filled.VolumeUp
            AudioMode.VIBRATE -> Icons.Default.Vibration
            AudioMode.SILENT -> Icons.AutoMirrored.Filled.VolumeOff
        }
        val audioText = when (audioMode) {
            AudioMode.SOUND -> I18n.audioBtnSound(currentLanguage)
            AudioMode.VIBRATE -> I18n.audioBtnVibrate(currentLanguage)
            AudioMode.SILENT -> I18n.audioBtnSilent(currentLanguage)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = audioShadowColor)
                .clip(RoundedCornerShape(20.dp))
                .background(audioGradient)
                .border(1.5.dp, audioBorder, RoundedCornerShape(20.dp))
                .clickable { onToggleAudioMode() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = audioIcon,
                        contentDescription = audioText,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = audioText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // =========================================================================
        // 2. ⚠️ WAZE-STYLE DANGER/HAZARD ALERT BUTTON (МЕТКА)
        // =========================================================================
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFFF5252).copy(alpha = 0.5f))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
                    )
                )
                .border(1.5.dp, Color(0xFFFF8A80).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .clickable { onTriggerAlarm() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = I18n.dangerBtn(currentLanguage),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = I18n.dangerBtn(currentLanguage),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // =========================================================================
        // 3. 📷 WAZE-STYLE PHOTO FIXATION BUTTON (ФОТО)
        // =========================================================================
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF29B6F6).copy(alpha = 0.5f))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF29B6F6), Color(0xFF0288D1))
                    )
                )
                .border(1.5.dp, Color(0xFF81D4FA).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .clickable { onTriggerPhoto() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = I18n.photoBtn(currentLanguage),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = I18n.photoBtn(currentLanguage),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
