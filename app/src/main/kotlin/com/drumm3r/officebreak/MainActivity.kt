package com.drumm3r.officebreak

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drumm3r.officebreak.data.SettingsRepository
import com.drumm3r.officebreak.ui.TimerViewModel
import com.drumm3r.officebreak.ui.screen.TimerScreen
import com.drumm3r.officebreak.ui.theme.OfficeBreakTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* No action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupLockScreenDisplay()
        requestNotificationPermissionIfNeeded()

        setContent {
            val viewModel: TimerViewModel = viewModel()
            val language by viewModel.language.collectAsState()

            LocaleWrapper(language = language) {
                OfficeBreakTheme {
                    TimerScreen(viewModel = viewModel)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setupLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun LocaleWrapper(
    language: String,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    if (language == SettingsRepository.LANGUAGE_SYSTEM) {
        content()

        return
    }

    val locale = Locale.forLanguageTag(language)
    val config = Configuration(context.resources.configuration).apply {
        setLocale(locale)
    }
    val localizedContext = context.createConfigurationContext(config)

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides config,
    ) {
        content()
    }
}
