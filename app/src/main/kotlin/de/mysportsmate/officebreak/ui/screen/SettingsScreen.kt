package de.mysportsmate.officebreak.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.ui.components.ConfirmResetStatsDialog
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    language: String,
    beepVolume: Int,
    vibrationEnabled: Boolean,
    themeMode: String,
    keepScreenOn: Boolean,
    autoRestart: Boolean,
    dynamicIncreaseEnabled: Boolean,
    beepCount: Int,
    trackingEnabled: Boolean,
    onLanguageChange: (String) -> Unit,
    onBeepVolumeChange: (Int) -> Unit,
    onBeepVolumePreview: (Int) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onAutoRestartChange: (Boolean) -> Unit,
    onDynamicIncreaseEnabledChange: (Boolean) -> Unit,
    onBeepCountChange: (Int) -> Unit,
    onTrackingEnabledChange: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    onBack: () -> Unit,
) {
    var showResetStatsDialog by rememberSaveable { mutableStateOf(false) }

    if (showResetStatsDialog) {
        ConfirmResetStatsDialog(
            onConfirm = {
                showResetStatsDialog = false
                onResetStats()
            },
            onDismiss = { showResetStatsDialog = false },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )

            LanguageDropdown(
                currentLanguage = language,
                onLanguageChange = onLanguageChange,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            ThemeDropdown(
                currentTheme = themeMode,
                onThemeChange = onThemeModeChange,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_timer),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SettingsToggleRow(
                label = stringResource(R.string.settings_keep_screen_on),
                checked = keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
            )

            SettingsToggleRow(
                label = stringResource(R.string.settings_auto_restart),
                checked = autoRestart,
                onCheckedChange = onAutoRestartChange,
            )

            SettingsToggleRow(
                label = stringResource(R.string.settings_dynamic_increase),
                checked = dynamicIncreaseEnabled,
                onCheckedChange = onDynamicIncreaseEnabledChange,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_notifications),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            BeepVolumeSlider(
                beepVolume = beepVolume,
                onBeepVolumeChange = onBeepVolumeChange,
                onBeepVolumePreview = onBeepVolumePreview,
            )

            SettingsToggleRow(
                label = stringResource(R.string.settings_vibration),
                checked = vibrationEnabled,
                onCheckedChange = onVibrationEnabledChange,
            )

            BeepCountSlider(
                beepCount = beepCount,
                enabled = beepVolume > 0,
                onBeepCountChange = onBeepCountChange,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_statistics),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SettingsToggleRow(
                label = stringResource(R.string.settings_tracking_enabled),
                checked = trackingEnabled,
                onCheckedChange = onTrackingEnabledChange,
            )

            Text(
                text = stringResource(R.string.settings_tracking_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            OutlinedButton(
                onClick = { showResetStatsDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.settings_reset_stats))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val languageOptions = listOf(
        SettingsRepository.LANGUAGE_SYSTEM to stringResource(R.string.language_system),
        SettingsRepository.LANGUAGE_DE to stringResource(R.string.language_german),
        SettingsRepository.LANGUAGE_EN to stringResource(R.string.language_english),
    )

    val selectedLabel = languageOptions.firstOrNull { it.first == currentLanguage }?.second
        ?: stringResource(R.string.language_system)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            languageOptions.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onLanguageChange(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val themeOptions = listOf(
        SettingsRepository.THEME_SYSTEM to stringResource(R.string.settings_theme_system),
        SettingsRepository.THEME_LIGHT to stringResource(R.string.settings_theme_light),
        SettingsRepository.THEME_DARK to stringResource(R.string.settings_theme_dark),
    )

    val selectedLabel = themeOptions.firstOrNull { it.first == currentTheme }?.second
        ?: stringResource(R.string.settings_theme_system)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_theme)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            themeOptions.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onThemeChange(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BeepVolumeSlider(
    beepVolume: Int,
    onBeepVolumeChange: (Int) -> Unit,
    onBeepVolumePreview: (Int) -> Unit,
) {
    var sliderValue by remember(beepVolume) { mutableStateOf(beepVolume.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_beep_volume),
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onBeepVolumeChange(it.roundToInt())
                },
                onValueChangeFinished = {
                    onBeepVolumePreview(sliderValue.roundToInt())
                },
                valueRange = 0f..100f,
                steps = 19,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${sliderValue.roundToInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun BeepCountSlider(
    beepCount: Int,
    enabled: Boolean,
    onBeepCountChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_beep_count),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Slider(
                value = beepCount.toFloat(),
                onValueChange = { onBeepCountChange(it.roundToInt()) },
                valueRange = 1f..5f,
                steps = 3,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = beepCount.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
