package com.safecomm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.safecomm.app.data.model.HAZARD_ICON_PRESETS
import com.safecomm.app.data.model.HazardIconManager
import com.safecomm.app.data.model.HazardIconPreset
import com.safecomm.app.ui.i18n.AppLanguage
import com.safecomm.app.ui.i18n.I18n
import com.safecomm.app.ui.theme.*

@Composable
fun HazardIconSelectionDialog(
    currentIcon: String,
    onSelectIcon: (String) -> Unit,
    onDismiss: () -> Unit,
    currentLanguage: AppLanguage = AppLanguage.EN
) {
    val context = LocalContext.current
    var selectedEmoji by remember { mutableStateOf(currentIcon) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = I18n.hazardIconPickerTitle(currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = I18n.hazardIconPickerSubtitle(currentLanguage),
                            fontSize = 11.sp,
                            color = TelegramTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = I18n.close(currentLanguage),
                            tint = TelegramTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Preview Card
                val currentPreset = HazardIconManager.getPresetByEmoji(selectedEmoji)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFFFC107).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedEmoji == "🐢") {
                                CamoTurtleIcon(size = 32.dp)
                            } else {
                                Text(text = selectedEmoji, fontSize = 24.sp)
                            }
                        }
                        Column {
                            Text(
                                text = currentPreset.localizedName(currentLanguage),
                                color = TelegramTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = I18n.activeHazardIconNote(currentLanguage),
                                color = Color(0xFFFFD600),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 20 Presets Grid (4 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HAZARD_ICON_PRESETS, key = { it.id }) { preset ->
                        val isSelected = selectedEmoji == preset.emoji
                        Box(
                            modifier = Modifier
                                .aspectRatio(0.92f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFFFC107).copy(alpha = 0.22f) else TelegramDarkCard)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFFFC107) else TelegramDivider.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedEmoji = preset.emoji
                                    HazardIconManager.setMilitaryIcon(context, preset.emoji)
                                    onSelectIcon(preset.emoji)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (preset.emoji == "🐢") {
                                        CamoTurtleIcon(size = 28.dp)
                                    } else {
                                        Text(text = preset.emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.localizedName(currentLanguage),
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color.White else TelegramTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(text = I18n.close(currentLanguage), color = TelegramTextSecondary)
                }
            }
        }
    }
}
