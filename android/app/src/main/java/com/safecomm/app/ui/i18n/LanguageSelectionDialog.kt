package com.safecomm.app.ui.i18n

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.safecomm.app.ui.theme.*

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(currentLanguage) }

    Dialog(
        onDismissRequest = { /* Modal: user must pick language on update */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = TelegramDarkSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌍",
                    fontSize = 36.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (selected) {
                        AppLanguage.UK -> "Оберіть мову"
                        AppLanguage.EN -> "Select Language"
                        AppLanguage.RU -> "Выберите язык"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = TelegramTextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (selected) {
                        AppLanguage.UK -> "Мову можна буде змінити в налаштуваннях"
                        AppLanguage.EN -> "Language can be changed anytime in settings"
                        AppLanguage.RU -> "Язык можно будет изменить в настройках"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TelegramTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                AppLanguage.entries.forEach { lang ->
                    val isPicked = selected == lang
                    val borderColor = if (isPicked) TelegramBlue else TelegramDarkBg
                    val bgColor = if (isPicked) TelegramBlue.copy(alpha = 0.15f) else TelegramDarkBg.copy(alpha = 0.6f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(
                                width = if (isPicked) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selected = lang }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lang.flag,
                            fontSize = 24.sp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = lang.title,
                            color = if (isPicked) TelegramBlue else TelegramTextPrimary,
                            fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 17.sp,
                            modifier = Modifier.weight(1f)
                        )

                        RadioButton(
                            selected = isPicked,
                            onClick = { selected = lang },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = TelegramBlue,
                                unselectedColor = TelegramTextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onLanguageSelected(selected) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (selected) {
                            AppLanguage.UK -> "Продовжити"
                            AppLanguage.EN -> "Continue"
                            AppLanguage.RU -> "Продолжить"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
