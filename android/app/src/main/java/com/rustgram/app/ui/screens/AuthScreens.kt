package com.rustgram.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rustgram.app.ui.components.*
import com.rustgram.app.ui.theme.MotionSprings

enum class AuthPhase { Connecting, Phone, Code, Password, Ready }

fun authPhaseOf(state: String): AuthPhase = when {
    state.contains("WaitingPhoneNumber") || state.contains("WaitingPhone") -> AuthPhase.Phone
    state.contains("WaitingCode") -> AuthPhase.Code
    state.contains("WaitingPassword") -> AuthPhase.Password
    state.contains("Ready") -> AuthPhase.Ready
    else -> AuthPhase.Connecting
}

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            RustgramLogo(
                modifier = Modifier
                    .size(124.dp)
                    .expressiveBouncyClick(pressedScale = 0.92f) {},
                cornerRadius = 36.dp,
                innerPadding = 24.dp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Rustgram",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Нативный Telegram-клиент на официальном TDLib\nRust Tokio Core & Material 3 Expressive",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TDLib 1.8.61",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Network",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.2f))

            FilledTonalButton(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .expressiveBouncyClick(pressedScale = 0.96f) { onContinue() },
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    "Подключиться",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun AuthScreen(
    state: String,
    value: String,
    errorMessage: String,
    isSubmitting: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = authPhaseOf(state)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            RustgramLogo(
                modifier = Modifier.size(80.dp),
                cornerRadius = 24.dp,
                innerPadding = 16.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Авторизация Telegram",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    (fadeIn(animationSpec = MotionSprings.fastEffects) +
                            slideInVertically(animationSpec = MotionSprings.defaultOffsetSpatial) { it / 2 })
                        .togetherWith(
                            fadeOut(animationSpec = MotionSprings.fastEffects) +
                                    slideOutVertically(animationSpec = MotionSprings.fastOffsetSpatial) { -it / 2 }
                        )
                },
                label = "auth_status_text"
            ) { targetPhase ->
                Text(
                    text = when (targetPhase) {
                        AuthPhase.Connecting -> "Инициализация защищенного подключения TDLib MTProto..."
                        AuthPhase.Phone -> "Введите номер телефона в международном формате (+7...)"
                        AuthPhase.Code -> "Введите проверочный код из приложения Telegram"
                        AuthPhase.Password -> "Введите пароль двухфакторной аутентификации (2FA)"
                        AuthPhase.Ready -> "Авторизация успешно завершена!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Shared Morphing Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = MotionSprings.defaultSizeSpatial),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (phase == AuthPhase.Connecting) {
                        M3HexagonLoadingIndicator(
                            size = 56.dp,
                            contained = true
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Связь с дата-центрами Telegram...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (phase == AuthPhase.Code) {
                        // High-grade Segmented OTP Input
                        Text(
                            "Код подтверждения",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        SegmentedOtpInput(
                            value = value,
                            onValueChange = onValueChange,
                            length = 5,
                            onComplete = {
                                onSubmit()
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        MorphingExpressiveButton(
                            text = "Подтвердить",
                            isLoading = isSubmitting,
                            enabled = value.length >= 5,
                            onClick = onSubmit
                        )
                    } else {
                        OutlinedTextField(
                            value = value,
                            onValueChange = onValueChange,
                            singleLine = true,
                            label = {
                                Text(
                                    when (phase) {
                                        AuthPhase.Phone -> "Номер телефона (+7...)"
                                        else -> "Облачный пароль"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    when (phase) {
                                        AuthPhase.Phone -> Icons.Default.Phone
                                        else -> Icons.Default.Password
                                    },
                                    contentDescription = null
                                )
                            },
                            visualTransformation = if (phase == AuthPhase.Password) {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = when (phase) {
                                    AuthPhase.Password -> KeyboardType.Password
                                    else -> KeyboardType.Phone
                                },
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (value.isNotBlank()) onSubmit()
                                }
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        MorphingExpressiveButton(
                            text = when (phase) {
                                AuthPhase.Phone -> "Отправить код"
                                else -> "Войти"
                            },
                            isLoading = isSubmitting,
                            enabled = value.isNotBlank(),
                            onClick = onSubmit
                        )
                    }
                }
            }
        }
    }
}
