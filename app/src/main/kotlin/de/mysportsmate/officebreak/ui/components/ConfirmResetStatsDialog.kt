package de.mysportsmate.officebreak.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme

@Composable
fun ConfirmResetStatsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.reset_stats_confirm_title)) },
        text = { Text(text = stringResource(R.string.reset_stats_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.reset_stats_confirm_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.reset_confirm_no))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmResetStatsDialogPreview() {
    OfficeBreakTheme {
        ConfirmResetStatsDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
