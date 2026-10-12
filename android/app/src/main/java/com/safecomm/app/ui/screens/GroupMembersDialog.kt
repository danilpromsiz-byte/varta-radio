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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

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
                            text = "${members.size} участников",
                            fontSize = 12.sp,
                            color = TelegramTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть", tint = TelegramTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Invite Code Box with Copy action
                group?.inviteCode?.let { code ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", code))
                                Toast.makeText(context, "Код приглашения скопирован: $code", Toast.LENGTH_SHORT).show()
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
                            Column {
                                Text("Код приглашения:", fontSize = 11.sp, color = TelegramTextSecondary)
                                Text(code, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TelegramBlue, letterSpacing = 2.sp)
                            }
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Копировать", tint = TelegramBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "СПИСОК УЧАСТНИКОВ:",
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
                                        text = if (member.role == "admin") "Администратор" else "Участник",
                                        fontSize = 11.sp,
                                        color = if (member.role == "admin") TelegramBlue else TelegramTextSecondary
                                    )
                                }

                                if (isAdmin && !isSelf) {
                                    IconButton(
                                        onClick = { onKickMember(member) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Исключить",
                                            tint = AlertShellingRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
