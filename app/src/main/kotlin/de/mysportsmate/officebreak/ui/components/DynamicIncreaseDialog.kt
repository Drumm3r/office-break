package de.mysportsmate.officebreak.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.ui.DynamicIncreaseOffer
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme

@Composable
fun DynamicIncreaseDialog(
    offer: DynamicIncreaseOffer,
    onAcceptReps: () -> Unit,
    onAcceptInterval: () -> Unit,
    onDecline: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* Not dismissible */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        LaunchedEffect(Unit) {
            delay(60_000L)
            onDecline()
        }
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
                    text = stringResource(R.string.dynamic_increase_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.dynamic_increase_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (offer) {
                    is DynamicIncreaseOffer.Both -> {
                        Button(
                            onClick = onAcceptReps,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.dynamic_increase_reps, offer.newReps))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onAcceptInterval,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.dynamic_increase_interval, offer.newIntervalMinutes))
                        }
                    }
                    is DynamicIncreaseOffer.RepsOnly -> {
                        Button(
                            onClick = onAcceptReps,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.dynamic_increase_reps, offer.newReps))
                        }
                    }
                    is DynamicIncreaseOffer.IntervalOnly -> {
                        Button(
                            onClick = onAcceptInterval,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.dynamic_increase_interval, offer.newIntervalMinutes))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDecline) {
                    Text(text = stringResource(R.string.dynamic_increase_decline))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DynamicIncreaseDialogBothPreview() {
    OfficeBreakTheme {
        DynamicIncreaseDialog(
            offer = DynamicIncreaseOffer.Both(newReps = 12, newIntervalMinutes = 25),
            onAcceptReps = {},
            onAcceptInterval = {},
            onDecline = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DynamicIncreaseDialogRepsOnlyPreview() {
    OfficeBreakTheme {
        DynamicIncreaseDialog(
            offer = DynamicIncreaseOffer.RepsOnly(newReps = 12),
            onAcceptReps = {},
            onAcceptInterval = {},
            onDecline = {},
        )
    }
}
