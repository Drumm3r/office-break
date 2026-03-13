package com.drumm3r.officebreak.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.drumm3r.officebreak.R
import com.drumm3r.officebreak.ui.theme.OfficeBreakTheme

@Composable
fun ConfirmResetDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.reset_confirm_title)) },
        text = { Text(text = stringResource(R.string.reset_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.reset_confirm_yes))
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
private fun ConfirmResetDialogPreview() {
    OfficeBreakTheme {
        ConfirmResetDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
