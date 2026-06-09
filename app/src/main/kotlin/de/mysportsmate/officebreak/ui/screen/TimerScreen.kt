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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.activity.compose.LocalActivity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.service.TimerState
import de.mysportsmate.officebreak.widget.WidgetTimerState
import de.mysportsmate.officebreak.data.AchievementRegistry
import de.mysportsmate.officebreak.ui.BackupUiState
import de.mysportsmate.officebreak.ui.TimerViewModel
import de.mysportsmate.officebreak.ui.components.AchievementUnlockDialog
import de.mysportsmate.officebreak.ui.components.ConfirmationDialog
import de.mysportsmate.officebreak.ui.components.CountdownDisplay
import de.mysportsmate.officebreak.ui.components.DonationPromptDialog
import de.mysportsmate.officebreak.ui.components.DynamicIncreaseDialog
import de.mysportsmate.officebreak.ui.components.ExerciseDialog
import de.mysportsmate.officebreak.ui.components.TimerSetup
import de.mysportsmate.officebreak.ui.components.VolumeBar
import de.mysportsmate.officebreak.data.ExerciseMode
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.resolveEffectiveSchedule
import java.time.LocalDate
import de.mysportsmate.officebreak.locale.LocaleHelper
import de.mysportsmate.officebreak.tts.BreakTtsManager
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme

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
    val addExerciseError by viewModel.addExerciseError.collectAsState()
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
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val customSoundUri by viewModel.customSoundUri.collectAsState()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()
    val exerciseMode by viewModel.exerciseMode.collectAsState()
    val workScheduleEnabled by viewModel.workScheduleEnabled.collectAsState()
    val weekSchedule by viewModel.weekSchedule.collectAsState()
    val autoModeByDayEnabled by viewModel.autoModeByDayEnabled.collectAsState()
    val modeOverrideForToday by viewModel.modeOverrideForToday.collectAsState()
    val dynamicIncreaseOffer by viewModel.dynamicIncreaseOffer.collectAsState()
    val newlyUnlockedAchievements by viewModel.newlyUnlockedAchievements.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val showDonationPrompt by viewModel.showDonationPrompt.collectAsState()
    val devModeEnabled by viewModel.devModeEnabled.collectAsState()
    val cloudBackupEnabled by viewModel.cloudBackupEnabled.collectAsState()
    val settingsDump by viewModel.settingsDump.collectAsState()
    val ttsContext = LocalContext.current
    val ttsManager = remember { BreakTtsManager(ttsContext) }
    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

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

    val snackbarContext = LocalContext.current
    val exportSuccessMsg = stringResource(R.string.export_success)
    val importSuccessMsg = stringResource(R.string.import_success)
    val exerciseHeadingPrefix = stringResource(R.string.exercise_heading)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(backupState) {
        val message = when (backupState) {
            is BackupUiState.ExportSuccess -> exportSuccessMsg
            is BackupUiState.ImportSuccess -> importSuccessMsg
            is BackupUiState.Error -> (backupState as BackupUiState.Error).message
            else -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearBackupState()
        }
    }

    if (currentExercise != null) {
        val exerciseName = currentExercise!!.displayName(LocalContext.current)
        val reps = currentReps ?: repsMin

        LaunchedEffect(currentExercise) {
            if (ttsEnabled) {
                val locale = LocaleHelper.resolveLocale(language)
                ttsManager.speak("$exerciseHeadingPrefix $reps $exerciseName", locale)
            }
        }

        ExerciseDialog(
            exerciseName = exerciseName,
            reps = reps,
            onDone = { viewModel.onExerciseDone() },
            showMusicToggle = customSoundUri != null,
            isMusicPlaying = isMusicPlaying,
            onToggleMusic = { viewModel.toggleMusicPlayback() },
        )
    }

    if (newlyUnlockedAchievements.isNotEmpty()) {
        val first = newlyUnlockedAchievements.first()
        val title = stringResource(first.titleResId)
        val description = stringResource(first.descriptionResId)

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

    var donationDialogVisible by rememberSaveable { mutableStateOf(false) }
    val donationDialogAllowed = showDonationPrompt &&
        currentExercise == null &&
        newlyUnlockedAchievements.isEmpty() &&
        dynamicIncreaseOffer == null &&
        !showResetDialog
    LaunchedEffect(donationDialogAllowed) {
        if (donationDialogAllowed && !donationDialogVisible) {
            donationDialogVisible = true
            viewModel.onDonationShown()
        }
    }
    if (donationDialogVisible) {
        DonationPromptDialog(
            onSupport = {
                donationDialogVisible = false
                viewModel.onDonationSupport()
            },
            onLater = {
                donationDialogVisible = false
                viewModel.onDonationLater()
            },
            onDismiss = {
                donationDialogVisible = false
                viewModel.onDonationDismiss()
            },
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
        val showOverrideHint = autoModeByDayEnabled && modeOverrideForToday != null && run {
            val dayIndex = LocalDate.now().dayOfWeek.ordinal
            val planMode = resolveEffectiveSchedule(weekSchedule, dayIndex)?.defaultMode
            planMode != null && planMode != modeOverrideForToday
        }
        ExerciseSettingsScreen(
            exercises = exercises,
            exerciseMode = exerciseMode,
            onModeChange = viewModel::setExerciseMode,
            onToggle = viewModel::toggleExercise,
            onAdd = viewModel::addExercise,
            onRemove = viewModel::removeExercise,
            onBack = { showExerciseSettings = false },
            showOverrideHint = showOverrideHint,
            addErrorMessage = addExerciseError,
            onErrorShown = viewModel::clearAddExerciseError,
            successEvents = viewModel.addExerciseSuccess,
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
            ttsEnabled = ttsEnabled,
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
            onTtsEnabledChange = viewModel::setTtsEnabled,
            customSoundUri = customSoundUri,
            onCustomSoundSelected = viewModel::setCustomSoundUri,
            onCustomSoundCleared = viewModel::clearCustomSound,
            onCustomSoundPreview = viewModel::playPreviewSound,
            isPreviewPlaying = viewModel.isPreviewPlaying.collectAsState().value,
            onStopPreview = viewModel::stopPreview,
            workScheduleEnabled = workScheduleEnabled,
            weekSchedule = weekSchedule,
            autoModeByDayEnabled = autoModeByDayEnabled,
            onWorkScheduleEnabledChange = viewModel::setWorkScheduleEnabled,
            onAutoModeByDayEnabledChange = viewModel::setAutoModeByDayEnabled,
            onDayScheduleChange = viewModel::updateDaySchedule,
            onTrackingEnabledChange = viewModel::setTrackingEnabled,
            onResetStats = viewModel::resetStats,
            onExportToUri = viewModel::exportData,
            onImportFromUri = viewModel::importData,
            onOpenKofi = viewModel::openKofiLink,
            onOpenImprint = viewModel::openImprintLink,
            onOpenPrivacy = viewModel::openPrivacyLink,
            cloudBackupEnabled = cloudBackupEnabled,
            onCloudBackupEnabledChange = viewModel::setCloudBackupEnabled,
            devModeEnabled = devModeEnabled,
            settingsDump = settingsDump,
            onDevModeEnabledChange = viewModel::setDevModeEnabled,
            onResetDonationPrompt = viewModel::resetDonationPromptForTesting,
            onResetOnboarding = viewModel::resetOnboarding,
            onShowDataStoreDump = viewModel::loadSettingsDump,
            onDismissSettingsDump = viewModel::clearSettingsDump,
            onClearAllData = viewModel::clearAllData,
            onBack = { showSettings = false },
        )

        return
    }

    if (showResetDialog) {
        ConfirmationDialog(
            titleRes = R.string.reset_confirm_title,
            messageRes = R.string.reset_confirm_message,
            confirmRes = R.string.reset_confirm_yes,
            dismissRes = R.string.reset_confirm_no,
            onConfirm = {
                showResetDialog = false
                viewModel.resetTimer()
            },
            onDismiss = { showResetDialog = false },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            var topBarHeightPx by remember { mutableStateOf(0) }
            val density = LocalDensity.current
            val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topBarHeightDp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = timerState,
                    contentKey = { state ->
                        when (state) {
                            is TimerState.Idle -> WidgetTimerState.STATUS_IDLE
                            is TimerState.Running -> WidgetTimerState.STATUS_RUNNING
                            is TimerState.Paused -> WidgetTimerState.STATUS_PAUSED
                            is TimerState.Expired -> WidgetTimerState.STATUS_EXPIRED
                            is TimerState.WorkEnded -> WidgetTimerState.STATUS_WORK_ENDED
                        }
                    },
                    label = "timer_content",
                ) { state ->
                    if (state is TimerState.Idle) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
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
                    } else if (state is TimerState.Paused) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.break_pause_title),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.break_pause_message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            CountdownDisplay(
                                remainingSeconds = state.remainingSeconds,
                                totalSeconds = state.totalSeconds,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(48.dp))

                            OutlinedButton(
                                onClick = { showResetDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(56.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.timer_reset),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    } else if (state is TimerState.WorkEnded) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.work_ended_title),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.work_ended_message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(48.dp))

                            Button(
                                onClick = { viewModel.dismissWorkEnded() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(56.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.work_ended_dismiss),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            val running = state as? TimerState.Running

                            CountdownDisplay(
                                remainingSeconds = running?.remainingSeconds ?: 0L,
                                totalSeconds = running?.totalSeconds ?: 1L,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth(),
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

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { topBarHeightPx = it.height },
            ) {
                if (timerState is TimerState.Running ||
                    timerState is TimerState.Paused ||
                    timerState is TimerState.Expired
                ) {
                    VolumeBar(
                        volume = beepVolume,
                        onVolumeChange = viewModel::setBeepVolume,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else if (timerState is TimerState.Idle) {
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenIdlePreview() {
    OfficeBreakTheme {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.stats_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = stringResource(R.string.achievements_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = stringResource(R.string.exercises_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    TimerSetup(
                        hours = SettingsRepository.DEFAULT_HOURS,
                        minutes = SettingsRepository.DEFAULT_MINUTES,
                        repsMin = SettingsRepository.DEFAULT_REPS_MIN,
                        repsMax = SettingsRepository.DEFAULT_REPS_MAX,
                        repsLinked = true,
                        onHoursChange = {},
                        onMinutesChange = {},
                        onRepsMinChange = {},
                        onRepsMaxChange = {},
                        onRepsLinkedChange = {},
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {},
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenRunningPreview() {
    OfficeBreakTheme {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                VolumeBar(
                    volume = 80,
                    onVolumeChange = {},
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CountdownDisplay(
                        remainingSeconds = 1425L,
                        totalSeconds = 2700L,
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    OutlinedButton(
                        onClick = {},
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

@Preview(showBackground = true)
@Composable
private fun TimerScreenPausedPreview() {
    OfficeBreakTheme {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.break_pause_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.break_pause_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        CountdownDisplay(
                            remainingSeconds = 1425L,
                            totalSeconds = 2700L,
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        OutlinedButton(
                            onClick = {},
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
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

@Preview(showBackground = true)
@Composable
private fun TimerScreenWorkEndedPreview() {
    OfficeBreakTheme {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.work_ended_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.work_ended_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(56.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.work_ended_dismiss),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

