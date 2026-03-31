package de.mysportsmate.officebreak.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.LocalActivity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.service.TimerState
import de.mysportsmate.officebreak.data.AchievementRegistry
import de.mysportsmate.officebreak.ui.BackupUiState
import de.mysportsmate.officebreak.ui.TimerViewModel
import de.mysportsmate.officebreak.ui.components.AchievementUnlockDialog
import de.mysportsmate.officebreak.ui.components.ConfirmResetDialog
import de.mysportsmate.officebreak.ui.components.CountdownDisplay
import de.mysportsmate.officebreak.ui.components.DynamicIncreaseDialog
import de.mysportsmate.officebreak.ui.components.ExerciseDialog
import de.mysportsmate.officebreak.ui.components.TimerSetup

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = viewModel(),
) {
    val timerState by viewModel.timerState.collectAsState()
    val hours by viewModel.hours.collectAsState()
    val minutes by viewModel.minutes.collectAsState()
    val repsMin by viewModel.repsMin.collectAsState()
    val repsMax by viewModel.repsMax.collectAsState()
    val repsLinked by viewModel.repsLinked.collectAsState()
    val currentReps by viewModel.currentReps.collectAsState()
    val currentExercise by viewModel.currentExercise.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val language by viewModel.language.collectAsState()
    val beepVolume by viewModel.beepVolume.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val autoRestart by viewModel.autoRestart.collectAsState()
    val beepCount by viewModel.beepCount.collectAsState()
    val trackingEnabled by viewModel.trackingEnabled.collectAsState()
    val statsSnapshot by viewModel.statsSnapshot.collectAsState()
    val achievementState by viewModel.achievementState.collectAsState()
    val breakRecords by viewModel.breakRecords.collectAsState()
    val dynamicIncreaseEnabled by viewModel.dynamicIncreaseEnabled.collectAsState()
    val dynamicIncreaseOffer by viewModel.dynamicIncreaseOffer.collectAsState()
    val newlyUnlockedAchievements by viewModel.newlyUnlockedAchievements.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showExerciseSettings by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showStats by rememberSaveable { mutableStateOf(false) }
    var showAchievements by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(timerState) {
        if (timerState is TimerState.Expired) {
            viewModel.onTimerExpired()
        }
    }

    val activity = LocalActivity.current
    LaunchedEffect(keepScreenOn, timerState) {
        if (keepScreenOn && timerState is TimerState.Running) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val toastContext = LocalContext.current
    LaunchedEffect(backupState) {
        val message = when (backupState) {
            is BackupUiState.ExportSuccess -> toastContext.getString(R.string.export_success)
            is BackupUiState.ImportSuccess -> toastContext.getString(R.string.import_success)
            is BackupUiState.Error -> (backupState as BackupUiState.Error).message
            else -> null
        }
        if (message != null) {
            Toast.makeText(toastContext, message, Toast.LENGTH_LONG).show()
            viewModel.clearBackupState()
        }
    }

    if (currentExercise != null) {
        ExerciseDialog(
            exerciseName = currentExercise!!.displayName(LocalContext.current),
            reps = currentReps ?: repsMin,
            onDone = { viewModel.onExerciseDone() },
        )
    }

    if (newlyUnlockedAchievements.isNotEmpty()) {
        val context = LocalContext.current
        val first = newlyUnlockedAchievements.first()
        val titleResId = context.resources.getIdentifier(first.titleResKey, "string", context.packageName)
        val descResId = context.resources.getIdentifier(first.descriptionResKey, "string", context.packageName)
        val title = if (titleResId != 0) context.getString(titleResId) else first.titleResKey
        val description = if (descResId != 0) context.getString(descResId) else first.descriptionResKey

        AchievementUnlockDialog(
            title = title,
            description = description,
            onDismiss = { viewModel.dismissAchievementCelebration() },
        )
    }

    if (dynamicIncreaseOffer != null && newlyUnlockedAchievements.isEmpty()) {
        DynamicIncreaseDialog(
            offer = dynamicIncreaseOffer!!,
            onAcceptReps = { viewModel.acceptIncreaseReps() },
            onAcceptInterval = { viewModel.acceptDecreaseInterval() },
            onDecline = { viewModel.declineDynamicIncrease() },
        )
    }

    if (showStats) {
        StatsScreen(
            snapshot = statsSnapshot,
            breakRecords = breakRecords,
            onBack = { showStats = false },
        )
        return
    }

    if (showAchievements) {
        AchievementsScreen(
            snapshot = statsSnapshot,
            achievementState = achievementState,
            onBack = { showAchievements = false },
        )
        return
    }

    if (showExerciseSettings) {
        ExerciseSettingsScreen(
            exercises = exercises,
            onToggle = viewModel::toggleExercise,
            onAdd = viewModel::addExercise,
            onRemove = viewModel::removeExercise,
            onBack = { showExerciseSettings = false },
        )

        return
    }

    if (showSettings) {
        SettingsScreen(
            language = language,
            beepVolume = beepVolume,
            vibrationEnabled = vibrationEnabled,
            themeMode = themeMode,
            keepScreenOn = keepScreenOn,
            autoRestart = autoRestart,
            dynamicIncreaseEnabled = dynamicIncreaseEnabled,
            beepCount = beepCount,
            trackingEnabled = trackingEnabled,
            onLanguageChange = viewModel::setLanguage,
            onBeepVolumeChange = viewModel::setBeepVolume,
            onBeepVolumePreview = viewModel::playPreviewBeep,
            onVibrationEnabledChange = viewModel::setVibrationEnabled,
            onThemeModeChange = viewModel::setThemeMode,
            onKeepScreenOnChange = viewModel::setKeepScreenOn,
            onAutoRestartChange = viewModel::setAutoRestart,
            onDynamicIncreaseEnabledChange = viewModel::setDynamicIncreaseEnabled,
            onBeepCountChange = viewModel::setBeepCount,
            onTrackingEnabledChange = viewModel::setTrackingEnabled,
            onResetStats = viewModel::resetStats,
            onExportToUri = viewModel::exportData,
            onImportFromUri = viewModel::importData,
            onBack = { showSettings = false },
        )

        return
    }

    if (showResetDialog) {
        ConfirmResetDialog(
            onConfirm = {
                showResetDialog = false
                viewModel.resetTimer()
            },
            onDismiss = { showResetDialog = false },
        )
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (timerState is TimerState.Idle) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    IconButton(onClick = { showStats = true }) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.stats_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { showAchievements = true }) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = stringResource(R.string.achievements_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { showExerciseSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = stringResource(R.string.exercises_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = timerState is TimerState.Idle,
                    label = "timer_content",
                ) { isIdle ->
                    if (isIdle) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            TimerSetup(
                                hours = hours,
                                minutes = minutes,
                                repsMin = repsMin,
                                repsMax = repsMax,
                                repsLinked = repsLinked,
                                onHoursChange = viewModel::setHours,
                                onMinutesChange = viewModel::setMinutes,
                                onRepsMinChange = viewModel::setRepsMin,
                                onRepsMaxChange = viewModel::setRepsMax,
                                onRepsLinkedChange = viewModel::setRepsLinked,
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { viewModel.startTimer() },
                                enabled = hours > 0 || minutes > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                                    .height(56.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.timer_start),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val running = timerState as? TimerState.Running

                            CountdownDisplay(
                                remainingSeconds = running?.remainingSeconds ?: 0L,
                                totalSeconds = running?.totalSeconds ?: 1L,
                            )

                            Spacer(modifier = Modifier.height(48.dp))

                            OutlinedButton(
                                onClick = { showResetDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                                    .height(56.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.timer_reset),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

