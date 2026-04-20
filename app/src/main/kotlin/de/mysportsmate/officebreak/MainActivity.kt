package de.mysportsmate.officebreak

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.locale.LocaleHelper
import de.mysportsmate.officebreak.ui.TimerViewModel
import de.mysportsmate.officebreak.ui.screen.OnboardingScreen
import de.mysportsmate.officebreak.ui.screen.TimerScreen
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme

class MainActivity : ComponentActivity() {

    private var currentLanguage: String? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* No action needed */ }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupLockScreenDisplay()
        requestNotificationPermissionIfNeeded()

        setContent {
            val viewModel: TimerViewModel = viewModel()
            val language by viewModel.language.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

            LaunchedEffect(language) {
                if (currentLanguage != null && currentLanguage != language) {
                    (this@MainActivity as? Activity)?.recreate()
                }
                currentLanguage = language
            }

            OfficeBreakTheme(
                darkTheme = when (themeMode) {
                    SettingsRepository.THEME_DARK -> true
                    SettingsRepository.THEME_LIGHT -> false
                    else -> isSystemInDarkTheme()
                },
            ) {
                when (onboardingCompleted) {
                    null -> Box(Modifier.fillMaxSize())
                    true -> TimerScreen(viewModel = viewModel)
                    false -> OnboardingScreen(
                        onComplete = viewModel::completeOnboarding,
                        onWorkScheduleConfigured = { enabled, autoMode, schedule ->
                            viewModel.applyWorkSchedule(enabled, autoMode, schedule)
                        },
                    )
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
