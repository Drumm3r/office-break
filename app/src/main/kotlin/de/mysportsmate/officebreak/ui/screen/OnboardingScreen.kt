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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.ExerciseMode
import de.mysportsmate.officebreak.data.FitnessLevel
import de.mysportsmate.officebreak.data.resolveEffectiveSchedule
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme

@Composable
fun OnboardingScreen(
    onComplete: (FitnessLevel, ExerciseMode) -> Unit,
    onWorkScheduleConfigured: (Boolean, Boolean, List<DaySchedule>) -> Unit = { _, _, _ -> },
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var selectedLevelOrdinal by rememberSaveable { mutableIntStateOf(-1) }
    var selectedModeOrdinal by rememberSaveable { mutableIntStateOf(-1) }
    var workScheduleEnabled by remember { mutableStateOf(false) }
    var autoModeByDayEnabled by remember { mutableStateOf(false) }
    var weekSchedule by remember { mutableStateOf(DEFAULT_WEEK_SCHEDULE) }

    val selectedLevel = if (selectedLevelOrdinal >= 0) {
        FitnessLevel.entries[selectedLevelOrdinal]
    } else {
        null
    }

    val selectedMode = if (selectedModeOrdinal >= 0) {
        ExerciseMode.entries[selectedModeOrdinal]
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
                    1 -> ExerciseModeSelectionStep(
                        selectedMode = selectedMode,
                        onSelect = { selectedModeOrdinal = it.ordinal },
                    )
                    2 -> WorkScheduleStep(
                        enabled = workScheduleEnabled,
                        weekSchedule = weekSchedule,
                        autoModeByDayEnabled = autoModeByDayEnabled,
                        selectedMode = selectedMode,
                        onEnabledChange = { workScheduleEnabled = it },
                        onAutoModeChange = { newEnabled ->
                            autoModeByDayEnabled = newEnabled
                            if (newEnabled && selectedMode != null) {
                                weekSchedule = weekSchedule.map { it.copy(defaultMode = selectedMode) }
                            }
                        },
                        onScheduleChange = { weekSchedule = it },
                    )
                    3 -> SummaryStep(
                        level = selectedLevel!!,
                        mode = selectedMode!!,
                        workScheduleEnabled = workScheduleEnabled,
                        autoModeByDayEnabled = autoModeByDayEnabled,
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
                    1 -> selectedMode != null
                    else -> true
                },
                onBack = { currentStep-- },
                onNext = { currentStep++ },
                onComplete = {
                    onWorkScheduleConfigured(workScheduleEnabled, autoModeByDayEnabled, weekSchedule)
                    onComplete(selectedLevel!!, selectedMode!!)
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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
                contentDescription = stringResource(labelRes),
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
private fun ExerciseModeSelectionStep(
    selectedMode: ExerciseMode?,
    onSelect: (ExerciseMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_mode_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_mode_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        ExerciseMode.entries.forEach { mode ->
            ExerciseModeCard(
                mode = mode,
                isSelected = mode == selectedMode,
                onClick = { onSelect(mode) },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ExerciseModeCard(
    mode: ExerciseMode,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val icon: ImageVector
    val labelRes: Int
    val descRes: Int

    when (mode) {
        ExerciseMode.HOME_WORKOUT -> {
            icon = Icons.Default.FitnessCenter
            labelRes = R.string.exercise_mode_home_workout
            descRes = R.string.exercise_mode_home_workout_desc
        }
        ExerciseMode.HOME_MOBILITY -> {
            icon = Icons.Default.SelfImprovement
            labelRes = R.string.exercise_mode_home_mobility
            descRes = R.string.exercise_mode_home_mobility_desc
        }
        ExerciseMode.OFFICE -> {
            icon = Icons.Default.Business
            labelRes = R.string.exercise_mode_office
            descRes = R.string.exercise_mode_office_desc
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
                contentDescription = stringResource(labelRes),
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
            }
        }
    }
}

@Composable
private fun WorkScheduleStep(
    enabled: Boolean,
    weekSchedule: List<DaySchedule>,
    autoModeByDayEnabled: Boolean,
    selectedMode: ExerciseMode?,
    onEnabledChange: (Boolean) -> Unit,
    onAutoModeChange: (Boolean) -> Unit,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_auto_mode_by_day),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = autoModeByDayEnabled,
                    onCheckedChange = onAutoModeChange,
                    enabled = selectedMode != null,
                )
            }

            Text(
                text = stringResource(R.string.settings_auto_mode_by_day_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            weekSchedule.forEachIndexed { index, day ->
                DayScheduleRow(
                    dayName = dayNames[index],
                    day = day,
                    effectiveDay = resolveEffectiveSchedule(weekSchedule, index),
                    isFirstEnabled = weekSchedule.indexOfFirst { it.enabled } == index,
                    showModeSelector = autoModeByDayEnabled,
                    dayNames = dayNames,
                    weekSchedule = weekSchedule,
                    dayIndex = index,
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
private fun SummaryStep(
    level: FitnessLevel,
    mode: ExerciseMode,
    workScheduleEnabled: Boolean,
    autoModeByDayEnabled: Boolean,
    weekSchedule: List<DaySchedule>,
) {
    val levelLabelRes = when (level) {
        FitnessLevel.BEGINNER -> R.string.onboarding_level_beginner
        FitnessLevel.MODERATE -> R.string.onboarding_level_moderate
        FitnessLevel.ATHLETIC -> R.string.onboarding_level_athletic
    }

    val modeLabelRes = when (mode) {
        ExerciseMode.HOME_WORKOUT -> R.string.exercise_mode_home_workout
        ExerciseMode.HOME_MOBILITY -> R.string.exercise_mode_home_mobility
        ExerciseMode.OFFICE -> R.string.exercise_mode_office
    }

    val intervalText = if (level.hours > 0 && level.minutes > 0) {
        "${level.hours}h ${level.minutes}min"
    } else if (level.hours > 0) {
        "${level.hours}h"
    } else {
        "${level.minutes}min"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
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
                if (!(workScheduleEnabled && autoModeByDayEnabled)) {
                    SummaryRow(
                        label = stringResource(R.string.onboarding_summary_mode, stringResource(modeLabelRes)),
                        icon = when (mode) {
                            ExerciseMode.HOME_WORKOUT -> Icons.Default.FitnessCenter
                            ExerciseMode.HOME_MOBILITY -> Icons.Default.SelfImprovement
                            ExerciseMode.OFFICE -> Icons.Default.Business
                        },
                    )
                }
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

                    val groups = buildScheduleGroups(weekSchedule, dayNames, autoModeByDayEnabled)

                    groups.forEach { group ->
                        SummaryRow(label = "${group.days}: ${group.workStart}-${group.workEnd}")
                        SummaryRow(
                            label = stringResource(
                                R.string.onboarding_summary_lunch,
                                group.lunchStart,
                                group.lunchEnd,
                            ),
                            indent = 16.dp,
                        )
                        if (group.mode != null) {
                            val modeRes = when (group.mode) {
                                ExerciseMode.HOME_WORKOUT -> R.string.exercise_mode_home_workout
                                ExerciseMode.HOME_MOBILITY -> R.string.exercise_mode_home_mobility
                                ExerciseMode.OFFICE -> R.string.exercise_mode_office
                            }
                            val modeIcon = when (group.mode) {
                                ExerciseMode.HOME_WORKOUT -> Icons.Default.FitnessCenter
                                ExerciseMode.HOME_MOBILITY -> Icons.Default.SelfImprovement
                                ExerciseMode.OFFICE -> Icons.Default.Business
                            }
                            ModeSummaryRow(
                                modeName = stringResource(modeRes),
                                icon = modeIcon,
                                indent = 16.dp,
                            )
                        }
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
private fun ModeSummaryRow(
    modeName: String,
    icon: ImageVector,
    indent: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val labelPrefix = stringResource(R.string.onboarding_summary_mode, "").trimEnd()
    Row(
        modifier = Modifier.padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = labelPrefix,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = modeName,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = modeName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    icon: ImageVector? = null,
    indent: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Row(
        modifier = Modifier.padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
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
    val mode: ExerciseMode?,
)

private fun buildScheduleGroups(
    weekSchedule: List<DaySchedule>,
    dayNames: List<String>,
    includeMode: Boolean,
): List<ScheduleGroup> {
    data class Key(
        val ws: String,
        val we: String,
        val ls: String,
        val le: String,
        val mode: ExerciseMode?,
    )

    return weekSchedule.indices
        .filter { weekSchedule[it].enabled }
        .map { index ->
            val eff = resolveEffectiveSchedule(weekSchedule, index) ?: weekSchedule[index]
            dayNames[index] to Key(
                ws = "%02d:%02d".format(eff.workStartHour, eff.workStartMinute),
                we = "%02d:%02d".format(eff.workEndHour, eff.workEndMinute),
                ls = "%02d:%02d".format(eff.lunchStartHour, eff.lunchStartMinute),
                le = "%02d:%02d".format(eff.lunchEndHour, eff.lunchEndMinute),
                mode = if (includeMode) eff.defaultMode else null,
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
                mode = key.mode,
            )
        }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    OfficeBreakTheme {
        OnboardingScreen(
            onComplete = { _, _ -> },
            onWorkScheduleConfigured = { _, _, _ -> },
        )
    }
}
