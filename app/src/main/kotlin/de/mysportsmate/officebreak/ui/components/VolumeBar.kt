package de.mysportsmate.officebreak.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme
import kotlin.math.roundToInt

@Composable
fun VolumeBar(
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var preMuteVolume by rememberSaveable { mutableIntStateOf(DEFAULT_PRE_MUTE_VOLUME) }
    var sliderValue by remember(volume) { mutableIntStateOf(volume) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        IconButton(
            onClick = {
                if (volume > 0) {
                    preMuteVolume = volume
                    onVolumeChange(0)
                } else {
                    onVolumeChange(preMuteVolume)
                }
            },
        ) {
            Icon(
                imageVector = if (volume > 0) {
                    Icons.AutoMirrored.Filled.VolumeUp
                } else {
                    Icons.AutoMirrored.Filled.VolumeOff
                },
                contentDescription = if (volume > 0) {
                    stringResource(R.string.volume_mute)
                } else {
                    stringResource(R.string.volume_unmute)
                },
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        val volumeLabel = stringResource(R.string.settings_volume)
        Slider(
            value = sliderValue.toFloat(),
            onValueChange = {
                sliderValue = it.roundToInt()
                onVolumeChange(sliderValue)
            },
            valueRange = 0f..100f,
            steps = 19,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = volumeLabel },
        )
    }
}

private const val DEFAULT_PRE_MUTE_VOLUME = 80

@Preview(showBackground = true)
@Composable
private fun VolumeBarPreview() {
    OfficeBreakTheme {
        VolumeBar(
            volume = 80,
            onVolumeChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VolumeBarMutedPreview() {
    OfficeBreakTheme {
        VolumeBar(
            volume = 0,
            onVolumeChange = {},
        )
    }
}
