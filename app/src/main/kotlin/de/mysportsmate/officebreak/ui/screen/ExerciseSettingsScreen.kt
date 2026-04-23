package de.mysportsmate.officebreak.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.Exercise
import de.mysportsmate.officebreak.data.ExerciseMode
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSettingsScreen(
    exercises: List<Exercise>,
    exerciseMode: ExerciseMode,
    onModeChange: (ExerciseMode) -> Unit,
    onToggle: (Int) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onBack: () -> Unit,
    showOverrideHint: Boolean = false,
) {
    var newExerciseName by rememberSaveable { mutableStateOf("") }
    val listState = remember(exerciseMode) { LazyListState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exercises_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(horizontal = 16.dp),
            ) {
                ExerciseMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == exerciseMode,
                        onClick = { onModeChange(mode) },
                        modifier = Modifier.fillMaxHeight(),
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ExerciseMode.entries.size,
                        ),
                        icon = {},
                    ) {
                        Text(
                            text = when (mode) {
                                ExerciseMode.HOME_WORKOUT -> stringResource(R.string.exercise_mode_short_home_workout)
                                ExerciseMode.HOME_MOBILITY -> stringResource(R.string.exercise_mode_short_home_mobility)
                                ExerciseMode.OFFICE -> stringResource(R.string.exercise_mode_short_office)
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }

            if (showOverrideHint) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.exercise_mode_override_today_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newExerciseName,
                    onValueChange = { newExerciseName = it },
                    label = { Text(stringResource(R.string.new_exercise_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newExerciseName.isNotBlank()) {
                                onAdd(newExerciseName)
                                newExerciseName = ""
                            }
                        },
                    ),
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newExerciseName.isNotBlank()) {
                            onAdd(newExerciseName)
                            newExerciseName = ""
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_exercise),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            val sortedExercises = exercises
                .mapIndexed { index, exercise -> index to exercise }
                .sortedByDescending { it.second.isEnabled }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = sortedExercises.size,
                    key = { sortedExercises[it].second.name },
                ) { sortedIndex ->
                    val (originalIndex, exercise) = sortedExercises[sortedIndex]
                    ExerciseRow(
                        exercise = exercise,
                        canDelete = exercises.size > 1,
                        onToggle = { onToggle(originalIndex) },
                        onRemove = { onRemove(originalIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    canDelete: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Checkbox(
                checked = exercise.isEnabled,
                onCheckedChange = { onToggle() },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = exercise.displayName(LocalContext.current),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (canDelete) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.remove_exercise),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseSettingsScreenPreview() {
    OfficeBreakTheme {
        ExerciseSettingsScreen(
            exercises = listOf(
                Exercise(name = "Push Ups", nameResKey = "exercise_push_ups", isEnabled = true),
                Exercise(name = "Squats", nameResKey = "exercise_squats", isEnabled = true),
                Exercise(name = "Lunges", nameResKey = "exercise_lunges", isEnabled = false),
            ),
            exerciseMode = ExerciseMode.HOME_WORKOUT,
            onModeChange = {},
            onToggle = {},
            onAdd = {},
            onRemove = {},
            onBack = {},
        )
    }
}
