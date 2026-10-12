package com.safecomm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.ui.theme.*

/**
 * Authentication and Community Access Screen.
 *
 * Supports Google Services Authentication for verified unique identities
 * and anti-duplicate protection, plus tactical sector and password access.
 */
@Composable
fun AuthCommunityScreen(
    currentCallSign: String,
    initialCommunity: String,
    initialPassword: String,
    googleAccountEmail: String? = null,
    onGoogleSignInClick: () -> Unit = {},
    onGoogleSignOutClick: () -> Unit = {},
    onEnterCommunity: (callSign: String, communityTarget: String, groupPassword: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var callSign by remember { mutableStateOf(currentCallSign.ifBlank { "Позывной-" + (100..999).random() }) }
    var communityTarget by remember { mutableStateOf(initialCommunity) }
    var groupPassword by remember { mutableStateOf(initialPassword) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TelegramDarkBg)
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "🛡️ Varta Radio PTT",
            style = MaterialTheme.typography.headlineLarge,
            color = TelegramTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Оперативная связь и безопасность сообществ\n(Прямой защищённый радиоэфир)",
            style = MaterialTheme.typography.bodyMedium,
            color = TelegramTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // --- GOOGLE SERVICES AUTHENTICATION BLOCK ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (googleAccountEmail != null) Color(0xFF1B2E24) else Color(0xFF222B35)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (googleAccountEmail != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✅ Google Services: Авторизован",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = googleAccountEmail,
                                fontSize = 12.sp,
                                color = TelegramTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Уникальный ID подтверждён • Защита от дубликатов активна",
                                fontSize = 10.sp,
                                color = TelegramTextSecondary
                            )
                        }
                        TextButton(onClick = onGoogleSignOutClick) {
                            Text("Сменить", fontSize = 12.sp, color = TelegramBlue)
                        }
                    }
                } else {
                    Text(
                        text = "🌐 Авторизация через Google Services:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TelegramTextPrimary
                    )
                    Text(
                        text = "Вход через аккаунт Google обеспечивает подтверждение личности и защиту сообщества от спама и дубликатов.",
                        fontSize = 11.sp,
                        color = TelegramTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onGoogleSignInClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Войти через аккаунт Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- COMMUNITY SECTOR ACCESS BLOCK ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = TelegramDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 1. Call-sign (Independent tactical nickname)
                Text(
                    text = "1. Ваш оперативный позывной:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextPrimary
                )
                Text(
                    text = "Позывной в эфире рации (уникален для сектора, защищён от дубликатов)",
                    fontSize = 11.sp,
                    color = TelegramTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = callSign,
                    onValueChange = { callSign = it },
                    placeholder = { Text("Напр. Сокол, Дозор-3, Волна", color = TelegramTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TelegramTextPrimary,
                        unfocusedTextColor = TelegramTextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider,
                        focusedContainerColor = TelegramDarkCard,
                        unfocusedContainerColor = TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Manual Community Sector Entry
                Text(
                    text = "2. Закрытый сектор сообщества:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextPrimary
                )
                Text(
                    text = "Название закрытого канала/сектора (список скрыт для безопасности)",
                    fontSize = 11.sp,
                    color = TelegramTextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = communityTarget,
                    onValueChange = { communityTarget = it.trim() },
                    placeholder = { Text("Сектор-Север, Округ-1...", color = TelegramTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TelegramTextPrimary,
                        unfocusedTextColor = TelegramTextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider,
                        focusedContainerColor = TelegramDarkCard,
                        unfocusedContainerColor = TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Community Password
                Text(
                    text = "3. Пароль доступа к сектору:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramTextPrimary
                )
                Text(
                    text = "Ключ криптографической защиты (без него посторонний не попадёт в эфир)",
                    fontSize = 11.sp,
                    color = TelegramTextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = groupPassword,
                    onValueChange = { groupPassword = it },
                    placeholder = { Text("Пароль закрытого сообщества", color = TelegramTextSecondary) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TelegramTextPrimary,
                        unfocusedTextColor = TelegramTextPrimary,
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = TelegramDivider,
                        focusedContainerColor = TelegramDarkCard,
                        unfocusedContainerColor = TelegramDarkCard
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (callSign.isNotBlank() && communityTarget.isNotBlank() && groupPassword.isNotBlank()) {
                            onEnterCommunity(callSign.trim(), communityTarget.trim(), groupPassword.trim())
                        }
                    },
                    enabled = callSign.isNotBlank() && communityTarget.isNotBlank() && groupPassword.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Войти в защищённый эфир", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
