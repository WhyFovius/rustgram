package com.rustgram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rustgram.app.ui.screens.*
import com.rustgram.app.ui.theme.MotionSprings
import com.rustgram.app.ui.theme.RustgramTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Start Rust / TDLib backend
        val filesDir = File(filesDir, "tdlib-files").apply { mkdirs() }.path
        val dbDir = File(filesDir, "tdlib-db").apply { mkdirs() }.path
        NativeBridge.start(filesDir, dbDir)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }
            var useDynamicColor by remember { mutableStateOf(true) }

            RustgramTheme(
                darkTheme = isDarkTheme,
                dynamicColor = useDynamicColor
            ) {
                RustgramApp(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = it },
                    useDynamicColor = useDynamicColor,
                    onToggleDynamicColor = { useDynamicColor = it },
                    cacheDir = cacheDir
                )
            }
        }
    }
}

@Composable
fun RustgramApp(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    useDynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    cacheDir: File?
) {
    var authState by remember { mutableStateOf(NativeBridge.authState()) }
    var lastError by remember { mutableStateOf("") }
    var inputValue by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(true) }

    // Poll authState and any errors directly from TDLib transport
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(350)
            val state = NativeBridge.authState()
            authState = state
            val err = NativeBridge.takeError()
            if (err.isNotBlank()) {
                lastError = err
                isSubmitting = false
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        AnimatedContent(
            targetState = when {
                showWelcome -> "welcome"
                authState.contains("Ready") -> "main"
                else -> "auth"
            },
            transitionSpec = {
                if (targetState == "main") {
                    (fadeIn(animationSpec = MotionSprings.defaultEffects) +
                            scaleIn(initialScale = 0.92f, animationSpec = MotionSprings.defaultSpatial))
                        .togetherWith(
                            fadeOut(animationSpec = MotionSprings.fastEffects) +
                                    scaleOut(targetScale = 1.05f, animationSpec = MotionSprings.fastSpatial)
                        )
                } else {
                    (fadeIn(animationSpec = MotionSprings.fastEffects) +
                            slideInHorizontally(animationSpec = MotionSprings.defaultOffsetSpatial) { it / 3 })
                        .togetherWith(
                            fadeOut(animationSpec = MotionSprings.fastEffects) +
                                    slideOutHorizontally(animationSpec = MotionSprings.fastOffsetSpatial) { -it / 3 }
                        )
                }
            },
            label = "screen_spring_transition"
        ) { screen ->
            when (screen) {
                "welcome" -> WelcomeScreen(
                    onContinue = { showWelcome = false }
                )
                "main" -> MainScreen(
                    onLogout = {
                        showWelcome = true
                    },
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    useDynamicColor = useDynamicColor,
                    onToggleDynamicColor = onToggleDynamicColor,
                    cacheDir = cacheDir
                )
                else -> AuthScreen(
                    state = authState,
                    value = inputValue,
                    errorMessage = lastError,
                    isSubmitting = isSubmitting,
                    onValueChange = {
                        inputValue = it
                        if (lastError.isNotBlank()) lastError = ""
                    },
                    onSubmit = {
                        val phase = authPhaseOf(authState)
                        isSubmitting = true
                        when (phase) {
                            AuthPhase.Phone -> NativeBridge.setPhone(inputValue.trim())
                            AuthPhase.Code -> NativeBridge.setCode(inputValue.trim())
                            AuthPhase.Password -> NativeBridge.setPassword(inputValue.trim())
                            else -> {}
                        }
                        inputValue = ""
                    }
                )
            }
        }
    }
}
