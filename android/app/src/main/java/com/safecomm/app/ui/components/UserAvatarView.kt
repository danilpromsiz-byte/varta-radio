package com.safecomm.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.safecomm.app.ui.theme.*
import java.io.File

data class PresetAvatar(
    val id: String,
    val label: String,
    val emoji: String,
    val bgStart: Color,
    val bgEnd: Color
)

val PRESET_AVATARS = listOf(
    // 1. Tactical & Recon
    PresetAvatar("shield", "Варта", "🛡️", Color(0xFF1976D2), Color(0xFF0D47A1)),
    PresetAvatar("eagle", "Сокол", "🦅", Color(0xFFF57C00), Color(0xFFE65100)),
    PresetAvatar("wolf", "Волк", "🐺", Color(0xFF546E7A), Color(0xFF263238)),
    PresetAvatar("radio", "Связист", "📻", Color(0xFF388E3C), Color(0xFF1B5E20)),
    PresetAvatar("lightning", "Молния", "⚡", Color(0xFFFBC02D), Color(0xFFF57F17)),
    PresetAvatar("target", "Дозор", "🎯", Color(0xFFD32F2F), Color(0xFFB71C1C)),
    PresetAvatar("compass", "Штурман", "🧭", Color(0xFF0097A7), Color(0xFF006064)),
    PresetAvatar("drone", "Дрон", "🚁", Color(0xFF7B1FA2), Color(0xFF4A148C)),
    PresetAvatar("star", "Гвардеец", "🎖️", Color(0xFFC2185B), Color(0xFF880E4F)),

    // 2. Predators & Stealth
    PresetAvatar("lion", "Лев", "🦁", Color(0xFFFFA000), Color(0xFFFF6F00)),
    PresetAvatar("bear", "Медведь", "🐻", Color(0xFF795548), Color(0xFF3E2723)),
    PresetAvatar("tiger", "Тигр", "🐯", Color(0xFFFF5722), Color(0xFFBF360C)),
    PresetAvatar("panther", "Пантера", "🐆", Color(0xFF37474F), Color(0xFF212121)),
    PresetAvatar("fox", "Лис", "🦊", Color(0xFFFF7043), Color(0xFFD84315)),
    PresetAvatar("cobra", "Кобра", "🐍", Color(0xFF2E7D32), Color(0xFF1B5E20)),
    PresetAvatar("shark", "Акула", "🦈", Color(0xFF0277BD), Color(0xFF01579B)),
    PresetAvatar("dragon", "Дракон", "🐉", Color(0xFFC62828), Color(0xFF880E4F)),
    PresetAvatar("badger", "Барсук", "🦡", Color(0xFF455A64), Color(0xFF263238)),

    // 3. Special Ops & Combat
    PresetAvatar("ghost", "Призрак", "👻", Color(0xFF607D8B), Color(0xFF37474F)),
    PresetAvatar("skull", "Череп", "💀", Color(0xFF424242), Color(0xFF212121)),
    PresetAvatar("sniper", "Снайпер", "🔭", Color(0xFF4E342E), Color(0xFF3E2723)),
    PresetAvatar("paratrooper", "Десант", "🪂", Color(0xFF1565C0), Color(0xFF0D47A1)),
    PresetAvatar("swords", "Мечи", "⚔️", Color(0xFF6A1B9A), Color(0xFF4A148C)),
    PresetAvatar("ninja", "Ниндзя", "🥷", Color(0xFF212121), Color(0xFF000000)),
    PresetAvatar("knight", "Рыцарь", "🛡️", Color(0xFF5C6BC0), Color(0xFF283593)),
    PresetAvatar("dagger", "Кинжал", "🗡️", Color(0xFFAD1457), Color(0xFF6A1B9A)),
    PresetAvatar("samurai", "Самурай", "🥋", Color(0xFFB71C1C), Color(0xFF880E4F)),

    // 4. Tech & High-Tech Radio
    PresetAvatar("cyber", "Киборг", "🤖", Color(0xFF00ACC1), Color(0xFF006064)),
    PresetAvatar("satellite", "Спутник", "🛰️", Color(0xFF3949AB), Color(0xFF1A237E)),
    PresetAvatar("radar", "Радар", "📡", Color(0xFF00897B), Color(0xFF004D40)),
    PresetAvatar("hacker", "Хакер", "💻", Color(0xFF43A047), Color(0xFF1B5E20)),
    PresetAvatar("fire", "Пламя", "🔥", Color(0xFFFF3D00), Color(0xFFDD2C00)),
    PresetAvatar("meteor", "Метеор", "☄️", Color(0xFFFF6D00), Color(0xFFE65100)),
    PresetAvatar("rocket", "Ракета", "🚀", Color(0xFFD50000), Color(0xFFB71C1C)),
    PresetAvatar("energy", "Энергия", "🔋", Color(0xFF7CB342), Color(0xFF33691E)),
    PresetAvatar("crown", "Корона", "👑", Color(0xFFFFB300), Color(0xFFFF6F00))
)

fun getPresetAvatar(id: String): PresetAvatar {
    return PRESET_AVATARS.firstOrNull { it.id == id } ?: PRESET_AVATARS[0]
}

/**
 * Universal circular avatar view supporting preset tactical badges and custom photo uploads.
 */
@Composable
fun UserAvatarView(
    avatarString: String?,
    size: Dp = 44.dp,
    borderWidth: Dp = 1.5.dp,
    borderColor: Color = TelegramBlue,
    modifier: Modifier = Modifier
) {
    val clean = avatarString?.trim() ?: "shield"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (clean.startsWith("custom:") || clean.startsWith("content://") || clean.startsWith("file://") || clean.startsWith("/")) {
            val uriOrPath = clean.removePrefix("custom:")
            AsyncImage(
                model = uriOrPath,
                contentDescription = "Аватар",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val preset = getPresetAvatar(clean)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(preset.bgStart, preset.bgEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.emoji,
                    fontSize = (size.value * 0.48f).sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Tactical Avatar Selection Modal Dialog.
 * Allows picking from preset military/tactical icons or selecting custom image from gallery.
 */
@Composable
fun AvatarSelectionDialog(
    currentAvatar: String,
    onSelectAvatar: (String) -> Unit,
    onDismiss: () -> Unit,
    currentLanguage: com.safecomm.app.ui.i18n.AppLanguage = com.safecomm.app.ui.i18n.AppLanguage.EN
) {
    var selectedId by remember { mutableStateOf(currentAvatar) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val customAvatarKey = "custom:$uri"
            selectedId = customAvatarKey
            onSelectAvatar(customAvatarKey)
            onDismiss()
        }
    }

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
                        text = "👤 ${com.safecomm.app.ui.i18n.I18n.avatarPresetTitle(currentLanguage)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TelegramTextPrimary,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Current Large Preview
                UserAvatarView(
                    avatarString = selectedId,
                    size = 72.dp,
                    borderWidth = 3.dp,
                    borderColor = TelegramBlue
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (selectedId.startsWith("custom:")) "Ваше фото" else getPresetAvatar(selectedId).label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Grid
                Text(
                    text = "Тактические позывные значки (36):",
                    fontSize = 12.sp,
                    color = TelegramTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PRESET_AVATARS, key = { it.id }) { preset ->
                        val isSelected = selectedId == preset.id
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) Color(0xFFFF9800) else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    selectedId = preset.id
                                    onSelectAvatar(preset.id)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            UserAvatarView(
                                avatarString = preset.id,
                                size = 46.dp,
                                borderWidth = 0.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Photo Action Button
                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TelegramBlue)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        com.safecomm.app.ui.i18n.I18n.avatarCustomPhoto(currentLanguage),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
