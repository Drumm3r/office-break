package de.mysportsmate.officebreak.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme

@Composable
fun ExerciseDialog(
    exerciseName: String,
    reps: Int,
    onDone: () -> Unit,
    showMusicToggle: Boolean = false,
    isMusicPlaying: Boolean = false,
    onToggleMusic: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = { /* Not dismissible */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.exercise_heading),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.reps_format, reps),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.exercise_done),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                if (showMusicToggle) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onToggleMusic,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (isMusicPlaying) R.string.break_pause_music
                                else R.string.break_resume_music,
                            ),
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
private fun ExerciseDialogPreview() {
    OfficeBreakTheme {
        ExerciseDialog(
            exerciseName = "Push Ups",
            reps = 10,
            onDone = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExerciseDialogWithMusicPreview() {
    OfficeBreakTheme {
        ExerciseDialog(
            exerciseName = "Push Ups",
            reps = 10,
            onDone = {},
            showMusicToggle = true,
            isMusicPlaying = true,
            onToggleMusic = {},
        )
    }
}
