package com.safecomm.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.safecomm.app.data.model.Group
import com.safecomm.app.data.model.GroupMember
import com.safecomm.app.ui.theme.*

@Composable
fun GroupMembersDialog(
    group: Group?,
    members: List<GroupMember>,
    currentUserId: Long,
    isAdmin: Boolean,
    onKickMember: (GroupMember) -> Unit,
    onBanMember: (GroupMember, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMemberForModeration by remember { mutableStateOf<GroupMember?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = group?.name ?: "Сообщество",
                            style = MaterialTheme.typography.titleMedium,
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${members.size} участников онлайн",
                            fontSize = 12.sp,
                            color = TelegramTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть", tint = TelegramTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secure Channel Identifier Box with Copy action
                group?.inviteCode?.let { code ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Varta Channel Code", code))
                                Toast.makeText(context, "Идентификатор радиоканала скопирован", Toast.LENGTH_SHORT).show()
                            },
                        colors = CardDefaults.cardColors(containerColor = TelegramDarkCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Идентификатор радиоканала:", fontSize = 11.sp, color = TelegramTextSecondary)
                                Text(code, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("🔒 Ссылка Telegram скрыта для безопасности", fontSize = 10.sp, color = Color(0xFFFFD600))
                            }
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Копировать", tint = TelegramBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "СПИСОК УЧАСТНИКОВ В ЭФИРЕ:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextSecondary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members, key = { it.userId }) { member ->
                        val isSelf = member.userId == currentUserId
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = TelegramDarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.safecomm.app.ui.components.UserAvatarView(
                                    avatarString = member.avatarUrl,
                                    size = 40.dp,
                                    borderWidth = 1.5.dp,
                                    borderColor = if (member.role == "admin") TelegramBlue else TelegramDivider
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.nickname.ifBlank { "Пользователь #${member.userId}" } + if (isSelf) " (Вы)" else "",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TelegramTextPrimary
                                    )
                                    Text(
                                        text = if (member.role == "admin") "Администратор комнаты" else "Участник",
                                        fontSize = 11.sp,
                                        color = if (member.role == "admin") TelegramBlue else TelegramTextSecondary
                                    )
                                }

                                if (isAdmin && !isSelf) {
                                    OutlinedButton(
                                        onClick = { selectedMemberForModeration = member },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertShellingRed)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Модерация",
                                            modifier = Modifier.size(16.dp),
                                            tint = AlertShellingRed
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Бан/Кик", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal moderation dialog for admin
    selectedMemberForModeration?.let { target ->
        AlertDialog(
            onDismissRequest = { selectedMemberForModeration = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Модерация участника",
                        fontWeight = FontWeight.Bold,
                        color = TelegramTextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Позывной: ${target.nickname}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFFFFD600)
                    )
                    Text(
                        text = "Выберите действие для принудительного отключения участника:",
                        fontSize = 12.sp,
                        color = TelegramTextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. KICK
                    Button(
                        onClick = {
                            onKickMember(target)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("🚪 Выгнать сейчас (Kick)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // 2. BAN 15 min
                    OutlinedButton(
                        onClick = {
                            onBanMember(target, 15 * 60 * 1000L)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D))
                    ) {
                        Text("⏳ Забанить на 15 минут", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }

                    // 3. BAN 1 hour
                    OutlinedButton(
                        onClick = {
                            onBanMember(target, 60 * 60 * 1000L)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF7043))
                    ) {
                        Text("⏳ Забанить на 1 час", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }

                    // 4. BAN 24 hours
                    OutlinedButton(
                        onClick = {
                            onBanMember(target, 24 * 60 * 60 * 1000L)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF4511E))
                    ) {
                        Text("⏳ Забанить на 24 часа", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }

                    // 5. PERMANENT BAN
                    Button(
                        onClick = {
                            onBanMember(target, -1L)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("🚫 Забанить навсегда", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedMemberForModeration = null }) {
                    Text("Отмена", color = TelegramTextSecondary)
                }
            },
            containerColor = TelegramDarkSurface
        )
    }
}
