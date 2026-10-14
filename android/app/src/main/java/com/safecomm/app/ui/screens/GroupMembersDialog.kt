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
import androidx.compose.material.icons.filled.Edit
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
import com.safecomm.app.data.model.UserStatus
import com.safecomm.app.ui.i18n.AppLanguage
import com.safecomm.app.ui.i18n.I18n
import com.safecomm.app.ui.theme.*

@Composable
fun GroupMembersDialog(
    group: Group?,
    members: List<GroupMember>,
    currentUserId: Long,
    isChiefAdmin: Boolean,
    isAdmin: Boolean,
    currentLanguage: AppLanguage = AppLanguage.EN,
    currentCallSign: String = "",
    onUpdateCallSign: (String) -> Unit = {},
    onDelegateAdmin: (GroupMember, Boolean) -> Unit,
    onKickMember: (GroupMember) -> Unit,
    onBanMember: (GroupMember, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMemberForModeration by remember { mutableStateOf<GroupMember?>(null) }
    var showEditCallSignDialog by remember { mutableStateOf(false) }
    var callSignInput by remember { mutableStateOf(currentCallSign) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.80f)
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
                            text = group?.name ?: I18n.appTitle(currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            color = TelegramTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = I18n.membersDialogTitle(currentLanguage, members.size),
                            fontSize = 12.sp,
                            color = TelegramTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = I18n.close(currentLanguage), tint = TelegramTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secure Channel Identifier Box
                group?.inviteCode?.let { code ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Varta Channel Code", code))
                                Toast.makeText(context, I18n.copiedToClipboard(context), Toast.LENGTH_SHORT).show()
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
                                Text(I18n.roomLabel(currentLanguage), fontSize = 11.sp, color = TelegramTextSecondary)
                                Text(code, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TelegramBlue)
                            }
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = TelegramBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members, key = { "${it.userId}_${it.senderId}_${it.role}" }) { member ->
                        val isSelf = member.userId == currentUserId || member.nickname.contains("(Вы)") || member.nickname.contains("(Ви)") || member.nickname.contains("(You)")
                        val canModerateThis = !isSelf && (isChiefAdmin || (isAdmin && member.role == "member"))

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
                                    borderColor = when (member.role) {
                                        "owner" -> Color(0xFFFFD600)
                                        "admin" -> TelegramBlue
                                        else -> TelegramDivider
                                    }
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val userStatus = UserStatus.fromCode(member.status)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = member.nickname.ifBlank { "User #${member.userId}" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TelegramTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = userStatus.icon,
                                            fontSize = 13.sp
                                        )
                                    }

                                    val roleText = when (member.role) {
                                        "owner" -> I18n.ownerBadge(currentLanguage)
                                        "admin" -> I18n.adminBadge(currentLanguage)
                                        else -> I18n.memberBadge(currentLanguage)
                                    }
                                    val roleColor = when (member.role) {
                                        "owner" -> Color(0xFFFFD600)
                                        "admin" -> TelegramBlue
                                        else -> TelegramTextSecondary
                                    }
                                    val statusLabel = when (userStatus) {
                                        UserStatus.TALK -> I18n.userStatusTalk(currentLanguage)
                                        UserStatus.LISTEN -> I18n.userStatusListen(currentLanguage)
                                        UserStatus.AFK -> I18n.userStatusAfk(currentLanguage)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = roleText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = roleColor
                                        )
                                        Text(
                                            text = " • $statusLabel",
                                            fontSize = 11.sp,
                                            color = TelegramTextSecondary
                                        )
                                    }
                                }

                                if (isSelf) {
                                    IconButton(
                                        onClick = {
                                            callSignInput = currentCallSign.ifBlank {
                                                member.nickname.replace("(Вы)", "").replace("(Ви)", "").replace("(You)", "").trim()
                                            }
                                            showEditCallSignDialog = true
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = I18n.editCallsignTitle(currentLanguage),
                                            tint = TelegramBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (canModerateThis) {
                                    OutlinedButton(
                                        onClick = { selectedMemberForModeration = member },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertShellingRed)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Shield",
                                            modifier = Modifier.size(16.dp),
                                            tint = AlertShellingRed
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(I18n.actionManageBtn(currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        val isTargetAdmin = target.role == "admin"

        AlertDialog(
            onDismissRequest = { selectedMemberForModeration = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = target.nickname,
                        fontWeight = FontWeight.Bold,
                        color = TelegramTextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. CHIEF ADMIN DELEGATION OPTION
                    if (isChiefAdmin) {
                        Button(
                            onClick = {
                                onDelegateAdmin(target, !isTargetAdmin)
                                selectedMemberForModeration = null
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTargetAdmin) Color(0xFF5C6BC0) else TelegramBlue
                            )
                        ) {
                            Text(
                                text = if (isTargetAdmin) I18n.removeAdminBtn(currentLanguage) else I18n.makeAdminBtn(currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Divider(color = TelegramDivider, modifier = Modifier.padding(vertical = 4.dp))
                    }

                    // 2. KICK
                    Button(
                        onClick = {
                            onKickMember(target)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text(I18n.kickBtn(currentLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // 3. BAN 15 min
                    OutlinedButton(
                        onClick = {
                            onBanMember(target, 15 * 60 * 1000L)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertShellingRed)
                    ) {
                        Text(I18n.ban15mBtn(currentLanguage), fontSize = 12.sp)
                    }

                    // 4. BAN 1 hour
                    OutlinedButton(
                        onClick = {
                            onBanMember(target, 60 * 60 * 1000L)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertShellingRed)
                    ) {
                        Text(I18n.ban1hBtn(currentLanguage), fontSize = 12.sp)
                    }

                    // 5. BAN 24 hours
                    OutlinedButton(
                        onClick = {
                            onBanMember(target, 24 * 60 * 60 * 1000L)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertShellingRed)
                    ) {
                        Text(I18n.ban24hBtn(currentLanguage), fontSize = 12.sp)
                    }

                    // 6. BAN FOREVER
                    Button(
                        onClick = {
                            onBanMember(target, -1L)
                            selectedMemberForModeration = null
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertShellingRed)
                    ) {
                        Text(I18n.banForeverBtn(currentLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedMemberForModeration = null }) {
                    Text(I18n.cancel(currentLanguage), color = TelegramTextSecondary)
                }
            },
            containerColor = TelegramDarkSurface
        )
    }

    // Modal dialog to change callsign
    if (showEditCallSignDialog) {
        AlertDialog(
            onDismissRequest = { showEditCallSignDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✏️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = I18n.editCallsignTitle(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        color = TelegramTextPrimary,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = I18n.editCallsignPrompt(currentLanguage),
                        fontSize = 13.sp,
                        color = TelegramTextSecondary
                    )
                    OutlinedTextField(
                        value = callSignInput,
                        onValueChange = { callSignInput = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = I18n.callSignFieldPlaceholder(currentLanguage),
                                color = TelegramTextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TelegramTextPrimary,
                            unfocusedTextColor = TelegramTextPrimary,
                            focusedBorderColor = TelegramBlue,
                            unfocusedBorderColor = TelegramDivider,
                            focusedContainerColor = TelegramDarkCard,
                            unfocusedContainerColor = TelegramDarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = callSignInput.trim()
                        if (trimmed.isNotBlank()) {
                            onUpdateCallSign(trimmed)
                            Toast.makeText(context, I18n.callsignSavedToast(currentLanguage), Toast.LENGTH_SHORT).show()
                            showEditCallSignDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(I18n.save(currentLanguage), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCallSignDialog = false }) {
                    Text(I18n.cancel(currentLanguage), color = TelegramTextSecondary)
                }
            },
            containerColor = TelegramDarkSurface
        )
    }
}
