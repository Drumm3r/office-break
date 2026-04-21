package de.mysportsmate.officebreak.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme

@Composable
fun ConfirmationDialog(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    @StringRes confirmRes: Int,
    @StringRes dismissRes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(titleRes)) },
        text = { Text(text = stringResource(messageRes)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(confirmRes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(dismissRes))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreview() {
    OfficeBreakTheme {
        ConfirmationDialog(
            titleRes = R.string.reset_confirm_title,
            messageRes = R.string.reset_confirm_message,
            confirmRes = R.string.reset_confirm_yes,
            dismissRes = R.string.reset_confirm_no,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
