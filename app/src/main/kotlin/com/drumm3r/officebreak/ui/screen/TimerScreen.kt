package com.drumm3r.officebreak.ui.screen

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drumm3r.officebreak.R
import com.drumm3r.officebreak.data.SettingsRepository
import com.drumm3r.officebreak.service.TimerState
import com.drumm3r.officebreak.ui.TimerViewModel
import com.drumm3r.officebreak.ui.components.ConfirmResetDialog
import com.drumm3r.officebreak.ui.components.CountdownDisplay
import com.drumm3r.officebreak.ui.components.ExerciseDialog
import com.drumm3r.officebreak.ui.components.TimerSetup

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = viewModel(),
) {
    val timerState by viewModel.timerState.collectAsState()
    val hours by viewModel.hours.collectAsState()
    val minutes by viewModel.minutes.collectAsState()
    val reps by viewModel.reps.collectAsState()
    val currentExercise by viewModel.currentExercise.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val language by viewModel.language.collectAsState()
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showExerciseSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(timerState) {
        if (timerState is TimerState.Expired) {
            viewModel.onTimerExpired()
        }
    }

    if (currentExercise != null) {
        ExerciseDialog(
            exerciseName = currentExercise!!.name,
            reps = reps,
            onDone = { viewModel.onExerciseDone() },
        )
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
                    IconButton(onClick = { showExerciseSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = stringResource(R.string.exercises_title),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    LanguagePicker(
                        currentLanguage = language,
                        onLanguageChange = viewModel::setLanguage,
                    )
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
                                reps = reps,
                                onHoursChange = viewModel::setHours,
                                onMinutesChange = viewModel::setMinutes,
                                onRepsChange = viewModel::setReps,
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

@Composable
private fun LanguagePicker(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = stringResource(R.string.language_label),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_system)) },
                onClick = {
                    onLanguageChange(SettingsRepository.LANGUAGE_SYSTEM)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_german)) },
                onClick = {
                    onLanguageChange(SettingsRepository.LANGUAGE_DE)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.language_english)) },
                onClick = {
                    onLanguageChange(SettingsRepository.LANGUAGE_EN)
                    expanded = false
                },
            )
        }
    }
}
