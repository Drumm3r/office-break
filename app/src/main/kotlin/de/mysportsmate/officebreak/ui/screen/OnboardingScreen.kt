package de.mysportsmate.officebreak.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.Exercise
import de.mysportsmate.officebreak.data.FitnessLevel
import de.mysportsmate.officebreak.data.resolveEffectiveSchedule
import androidx.compose.runtime.remember

@Composable
fun OnboardingScreen(
    exercises: List<Exercise>,
    onComplete: (FitnessLevel, List<Exercise>) -> Unit,
    onWorkScheduleConfigured: (Boolean, List<DaySchedule>) -> Unit = { _, _ -> },
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var selectedLevelOrdinal by rememberSaveable { mutableIntStateOf(-1) }
    var exerciseToggles by rememberSaveable(exercises) {
        mutableStateOf(exercises.map { it.isEnabled })
    }
    var workScheduleEnabled by remember { mutableStateOf(false) }
    var weekSchedule by remember { mutableStateOf(DEFAULT_WEEK_SCHEDULE) }

    val selectedLevel = if (selectedLevelOrdinal >= 0) {
        FitnessLevel.entries[selectedLevelOrdinal]
    } else {
        null
    }

    val totalSteps = 4

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepIndicator(currentStep = currentStep, totalSteps = totalSteps)

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = currentStep,
                label = "onboarding_step",
                modifier = Modifier.weight(1f),
            ) { step ->
                when (step) {
                    0 -> FitnessLevelStep(
                        selectedLevel = selectedLevel,
                        onSelect = { selectedLevelOrdinal = it.ordinal },
                    )
                    1 -> ExerciseSelectionStep(
                        exercises = exercises,
                        toggles = exerciseToggles,
                        onToggle = { index ->
                            exerciseToggles = exerciseToggles.toMutableList().also {
                                it[index] = !it[index]
                            }
                        },
                    )
                    2 -> WorkScheduleStep(
                        enabled = workScheduleEnabled,
                        weekSchedule = weekSchedule,
                        onEnabledChange = { workScheduleEnabled = it },
                        onScheduleChange = { weekSchedule = it },
                    )
                    3 -> SummaryStep(
                        level = selectedLevel!!,
                        exerciseCount = exerciseToggles.count { it },
                        workScheduleEnabled = workScheduleEnabled,
                        weekSchedule = weekSchedule,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NavigationButtons(
                currentStep = currentStep,
                totalSteps = totalSteps,
                canAdvance = when (currentStep) {
                    0 -> selectedLevel != null
                    1 -> exerciseToggles.any { it }
                    else -> true
                },
                onBack = { currentStep-- },
                onNext = { currentStep++ },
                onComplete = {
                    val selected = exercises.mapIndexed { index, exercise ->
                        exercise.copy(isEnabled = exerciseToggles.getOrElse(index) { true })
                    }
                    onWorkScheduleConfigured(workScheduleEnabled, weekSchedule)
                    onComplete(selectedLevel!!, selected)
                },
            )
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentStep) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index <= currentStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun FitnessLevelStep(
    selectedLevel: FitnessLevel?,
    onSelect: (FitnessLevel) -> Unit,
) {
    var showDetails by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_fitness_question),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.onboarding_fitness_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        FitnessLevel.entries.forEach { level ->
            FitnessLevelCard(
                level = level,
                isSelected = level == selectedLevel,
                showDetails = showDetails,
                onClick = { onSelect(level) },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        TextButton(onClick = { showDetails = !showDetails }) {
            Text(
                text = if (showDetails) {
                    stringResource(R.string.onboarding_hide_details)
                } else {
                    stringResource(R.string.onboarding_show_details)
                },
            )
        }
    }
}

@Composable
private fun FitnessLevelCard(
    level: FitnessLevel,
    isSelected: Boolean,
    showDetails: Boolean,
    onClick: () -> Unit,
) {
    val icon: ImageVector
    val labelRes: Int
    val descRes: Int
    val detailRes: Int

    when (level) {
        FitnessLevel.BEGINNER -> {
            icon = Icons.AutoMirrored.Filled.DirectionsWalk
            labelRes = R.string.onboarding_level_beginner
            descRes = R.string.onboarding_level_beginner_desc
            detailRes = R.string.onboarding_level_beginner_detail
        }
        FitnessLevel.MODERATE -> {
            icon = Icons.AutoMirrored.Filled.DirectionsRun
            labelRes = R.string.onboarding_level_moderate
            descRes = R.string.onboarding_level_moderate_desc
            detailRes = R.string.onboarding_level_moderate_detail
        }
        FitnessLevel.ATHLETIC -> {
            icon = Icons.Default.FitnessCenter
            labelRes = R.string.onboarding_level_athletic
            descRes = R.string.onboarding_level_athletic_desc
            detailRes = R.string.onboarding_level_athletic_detail
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (showDetails) {
                    Text(
                        text = stringResource(detailRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseSelectionStep(
    exercises: List<Exercise>,
    toggles: List<Boolean>,
    onToggle: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_exercises_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_exercises_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(
                exercises,
                key = { _, exercise -> exercise.name },
            ) { index, exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(
                        checked = toggles.getOrElse(index) { true },
                        onCheckedChange = { onToggle(index) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = exercise.displayName(LocalContext.current),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkScheduleStep(
    enabled: Boolean,
    weekSchedule: List<DaySchedule>,
    onEnabledChange: (Boolean) -> Unit,
    onScheduleChange: (List<DaySchedule>) -> Unit,
) {
    val dayNames = listOf(
        stringResource(R.string.day_mon),
        stringResource(R.string.day_tue),
        stringResource(R.string.day_wed),
        stringResource(R.string.day_thu),
        stringResource(R.string.day_fri),
        stringResource(R.string.day_sat),
        stringResource(R.string.day_sun),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_schedule_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_schedule_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_work_schedule_enabled),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))

            weekSchedule.forEachIndexed { index, day ->
                DayScheduleRow(
                    dayName = dayNames[index],
                    day = day,
                    effectiveDay = resolveEffectiveSchedule(weekSchedule, index),
                    isFirstEnabled = weekSchedule.indexOfFirst { it.enabled } == index,
                    onDayChange = {
                        val updated = weekSchedule.toMutableList()
                        updated[index] = it
                        onScheduleChange(updated)
                    },
                )
            }

        }
    }
}

@Composable
private fun OnboardingTimeRow(
    label: String,
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = { editing = true }) {
            Text(text = "%02d:%02d".format(hour, minute))
        }
    }

    if (editing) {
        OnboardingTimePicker(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m ->
                onTimeChange(h, m)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.material3.TimePicker(state = state)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.reset_confirm_no))
                    }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                        Text(text = stringResource(R.string.dialog_confirm_ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStep(
    level: FitnessLevel,
    exerciseCount: Int,
    workScheduleEnabled: Boolean,
    weekSchedule: List<DaySchedule>,
) {
    val levelLabelRes = when (level) {
        FitnessLevel.BEGINNER -> R.string.onboarding_level_beginner
        FitnessLevel.MODERATE -> R.string.onboarding_level_moderate
        FitnessLevel.ATHLETIC -> R.string.onboarding_level_athletic
    }

    val intervalText = if (level.hours > 0 && level.minutes > 0) {
        "${level.hours}h ${level.minutes}min"
    } else if (level.hours > 0) {
        "${level.hours}h"
    } else {
        "${level.minutes}min"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_summary_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryRow(
                    label = stringResource(levelLabelRes),
                    icon = when (level) {
                        FitnessLevel.BEGINNER -> Icons.AutoMirrored.Filled.DirectionsWalk
                        FitnessLevel.MODERATE -> Icons.AutoMirrored.Filled.DirectionsRun
                        FitnessLevel.ATHLETIC -> Icons.Default.FitnessCenter
                    },
                )
                SummaryRow(
                    label = stringResource(R.string.onboarding_summary_interval, intervalText),
                )
                SummaryRow(
                    label = stringResource(R.string.onboarding_summary_reps, level.reps),
                )
                SummaryRow(
                    label = stringResource(R.string.onboarding_summary_exercises, exerciseCount),
                )
                if (workScheduleEnabled) {
                    val dayNames = listOf(
                        stringResource(R.string.day_mon),
                        stringResource(R.string.day_tue),
                        stringResource(R.string.day_wed),
                        stringResource(R.string.day_thu),
                        stringResource(R.string.day_fri),
                        stringResource(R.string.day_sat),
                        stringResource(R.string.day_sun),
                    )

                    val groups = buildScheduleGroups(weekSchedule, dayNames)

                    groups.forEach { (days, ws, we, ls, le) ->
                        SummaryRow(label = "$days: $ws–$we")
                        SummaryRow(
                            label = stringResource(R.string.onboarding_summary_lunch, ls, le),
                        )
                    }
                } else {
                    SummaryRow(
                        label = stringResource(R.string.onboarding_summary_work_schedule_off),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    icon: ImageVector? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    canAdvance: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
) {
    val lastStep = totalSteps - 1

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.onboarding_back))
            }
        }

        Button(
            onClick = if (currentStep == lastStep) onComplete else onNext,
            enabled = canAdvance,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                if (currentStep == lastStep) {
                    stringResource(R.string.onboarding_start)
                } else {
                    stringResource(R.string.onboarding_next)
                },
            )
        }
    }
}

private data class ScheduleGroup(
    val days: String,
    val workStart: String,
    val workEnd: String,
    val lunchStart: String,
    val lunchEnd: String,
)

private fun buildScheduleGroups(
    weekSchedule: List<DaySchedule>,
    dayNames: List<String>,
): List<ScheduleGroup> {
    data class Key(val ws: String, val we: String, val ls: String, val le: String)

    return weekSchedule.indices
        .filter { weekSchedule[it].enabled }
        .map { index ->
            val eff = resolveEffectiveSchedule(weekSchedule, index) ?: weekSchedule[index]
            dayNames[index] to Key(
                ws = "%02d:%02d".format(eff.workStartHour, eff.workStartMinute),
                we = "%02d:%02d".format(eff.workEndHour, eff.workEndMinute),
                ls = "%02d:%02d".format(eff.lunchStartHour, eff.lunchStartMinute),
                le = "%02d:%02d".format(eff.lunchEndHour, eff.lunchEndMinute),
            )
        }
        .groupBy({ it.second }, { it.first })
        .map { (key, days) ->
            ScheduleGroup(
                days = days.joinToString(", "),
                workStart = key.ws,
                workEnd = key.we,
                lunchStart = key.ls,
                lunchEnd = key.le,
            )
        }
}
