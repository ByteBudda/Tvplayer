package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.AppViewModel
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SlateCard

@Composable
fun ParentalScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val parentalEnabled by viewModel.parentalEnabled.collectAsState()
    val savedPin by viewModel.parentalPin.collectAsState()

    var isEnabledChecked by remember(parentalEnabled) { mutableStateOf(parentalEnabled) }
    var pinValue by remember(savedPin) { mutableStateOf(savedPin) }

    var feedbackMessage by remember { mutableStateOf("") }
    var isErrorType by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Text(
                text = "Родительский контроль",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Защита каналов и параметров вещаний семейным PIN-кодом",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toggle Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isEnabledChecked) Icons.Filled.Security else Icons.Filled.LockOpen,
                            contentDescription = "Замок",
                            tint = if (isEnabledChecked) CinemaAmber else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                "Ограничение вещания",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Запрашивать PIN для заблокированных каналов",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    var isSwitchFocused by remember { mutableStateOf(false) }
                    Switch(
                        checked = isEnabledChecked,
                        onCheckedChange = { isEnabledChecked = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CinemaAmber,
                            checkedTrackColor = CinemaAmber.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .onFocusChanged { isSwitchFocused = it.isFocused }
                            .border(
                                if (isSwitchFocused) 2.dp else 0.dp,
                                if (isSwitchFocused) Color.White else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .focusable()
                            .testTag("parental_switch")
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // PIN setup row
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Задайте проверочный код доступа:",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { 
                            if (it.length <= 8) pinValue = it 
                        },
                        label = { Text("Пароль (PIN-код)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_pin_field")
                    )
                    
                    Text(
                        "Введите от 4 до 8 цифр для защиты родительского профиля",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                var isSaveFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        if (pinValue.length < 4) {
                            feedbackMessage = "Минимальная длина пароля - 4 символа!"
                            isErrorType = true
                        } else {
                            viewModel.setParentalLockState(isEnabledChecked, pinValue)
                            feedbackMessage = "Профиль успешно защищен и сохранен!"
                            isErrorType = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnabledChecked) CinemaAmber else MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .onFocusChanged { isSaveFocused = it.isFocused }
                        .border(
                            if (isSaveFocused) 2.dp else 0.dp,
                            if (isSaveFocused) Color.White else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .focusable()
                        .testTag("save_parental_settings_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(
                            "Сохранить конфигурацию",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Info Alerts
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = LiveRed.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Оповещение",
                    tint = LiveRed
                )
                Column {
                    Text(
                        "Заметка для родителей",
                        color = LiveRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Вы можете нажать на иконку замочка рядом с любым каналом или удерживать его в списке главного экрана, чтобы быстро заблокировать или разблокировать его от детей. Заблокированный канал потребует ввода кода перед просмотром.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Status Feedback banners
        AnimatedVisibility(
            visible = feedbackMessage.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isErrorType) LiveRed.copy(alpha = 0.2f) else CinemaAmber.copy(alpha = 0.2f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = feedbackMessage,
                    color = if (isErrorType) LiveRed else CinemaAmber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // auto-clear message
        LaunchedEffect(feedbackMessage) {
            if (feedbackMessage.isNotEmpty()) {
                kotlinx.coroutines.delay(4000)
                feedbackMessage = ""
            }
        }
    }
}
