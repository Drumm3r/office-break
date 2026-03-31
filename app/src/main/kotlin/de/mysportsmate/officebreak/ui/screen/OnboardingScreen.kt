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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import de.mysportsmate.officebreak.data.Exercise
import de.mysportsmate.officebreak.data.FitnessLevel

@Composable
fun OnboardingScreen(
    exercises: List<Exercise>,
    onComplete: (FitnessLevel, List<Exercise>) -> Unit,
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var selectedLevelOrdinal by rememberSaveable { mutableIntStateOf(-1) }
    var exerciseToggles by rememberSaveable(exercises) {
        mutableStateOf(exercises.map { it.isEnabled })
    }

    val selectedLevel = if (selectedLevelOrdinal >= 0) {
        FitnessLevel.entries[selectedLevelOrdinal]
    } else {
        null
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepIndicator(currentStep = currentStep, totalSteps = 3)

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
                    2 -> SummaryStep(
                        level = selectedLevel!!,
                        exerciseCount = exerciseToggles.count { it },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NavigationButtons(
                currentStep = currentStep,
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

        Spacer(modifier = Modifier.height(32.dp))

        FitnessLevel.entries.forEach { level ->
            FitnessLevelCard(
                level = level,
                isSelected = level == selectedLevel,
                onClick = { onSelect(level) },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FitnessLevelCard(
    level: FitnessLevel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val icon: ImageVector
    val labelRes: Int
    val descRes: Int

    when (level) {
        FitnessLevel.BEGINNER -> {
            icon = Icons.AutoMirrored.Filled.DirectionsWalk
            labelRes = R.string.onboarding_level_beginner
            descRes = R.string.onboarding_level_beginner_desc
        }
        FitnessLevel.MODERATE -> {
            icon = Icons.AutoMirrored.Filled.DirectionsRun
            labelRes = R.string.onboarding_level_moderate
            descRes = R.string.onboarding_level_moderate_desc
        }
        FitnessLevel.ATHLETIC -> {
            icon = Icons.Default.FitnessCenter
            labelRes = R.string.onboarding_level_athletic
            descRes = R.string.onboarding_level_athletic_desc
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
                    Checkbox(
                        checked = toggles.getOrElse(index) { true },
                        onCheckedChange = { onToggle(index) },
                    )
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
private fun SummaryStep(
    level: FitnessLevel,
    exerciseCount: Int,
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
    canAdvance: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
) {
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
            onClick = if (currentStep == 2) onComplete else onNext,
            enabled = canAdvance,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                if (currentStep == 2) {
                    stringResource(R.string.onboarding_start)
                } else {
                    stringResource(R.string.onboarding_next)
                },
            )
        }
    }
}
