package com.drumm3r.officebreak.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drumm3r.officebreak.R
import com.drumm3r.officebreak.ui.theme.OfficeBreakTheme
import kotlin.math.roundToInt

@Composable
fun TimerSetup(
    hours: Int,
    minutes: Int,
    reps: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimeInputDialog by rememberSaveable { mutableStateOf(false) }

    if (showTimeInputDialog) {
        TimeInputDialog(
            initialHours = hours,
            initialMinutes = minutes,
            onConfirm = { h, m ->
                onHoursChange(h)
                onMinutesChange(m)
                showTimeInputDialog = false
            },
            onDismiss = { showTimeInputDialog = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "%02d:%02d".format(hours, minutes),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { showTimeInputDialog = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Text(
            text = stringResource(R.string.tap_to_enter),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        SliderRow(
            label = stringResource(R.string.hours_label),
            value = hours,
            range = 0f..4f,
            onValueChange = onHoursChange,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SliderRow(
            label = stringResource(R.string.minutes_label),
            value = minutes,
            range = 0f..59f,
            onValueChange = onMinutesChange,
        )

        Spacer(modifier = Modifier.height(24.dp))

        SliderRow(
            label = stringResource(R.string.reps_label),
            value = reps,
            range = 1f..50f,
            onValueChange = onRepsChange,
        )
    }
}

@Composable
private fun TimeInputDialog(
    initialHours: Int,
    initialMinutes: Int,
    onConfirm: (hours: Int, minutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hoursText by rememberSaveable { mutableStateOf(initialHours.toString()) }
    var minutesText by rememberSaveable { mutableStateOf(initialMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enter_time)) },
        text = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { input ->
                        if (input.length <= 2 && input.all { it.isDigit() }) {
                            hoursText = input
                        }
                    },
                    label = { Text(stringResource(R.string.hours_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { input ->
                        if (input.length <= 2 && input.all { it.isDigit() }) {
                            minutesText = input
                        }
                    },
                    label = { Text(stringResource(R.string.minutes_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = (hoursText.toIntOrNull() ?: 0).coerceIn(0, 4)
                    val m = (minutesText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    onConfirm(h, m)
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reset_confirm_no))
            }
        },
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range,
            steps = (range.endInclusive - range.start).toInt() - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerSetupPreview() {
    OfficeBreakTheme {
        TimerSetup(
            hours = 0,
            minutes = 30,
            reps = 10,
            onHoursChange = {},
            onMinutesChange = {},
            onRepsChange = {},
        )
    }
}
