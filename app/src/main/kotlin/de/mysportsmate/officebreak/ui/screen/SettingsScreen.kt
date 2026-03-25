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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.SettingsRepository
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    language: String,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    themeMode: String,
    keepScreenOn: Boolean,
    autoRestart: Boolean,
    beepCount: Int,
    onLanguageChange: (String) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onAutoRestartChange: (Boolean) -> Unit,
    onBeepCountChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_notifications),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SettingsToggleRow(
                label = stringResource(R.string.settings_sound),
                checked = soundEnabled,
                onCheckedChange = onSoundEnabledChange,
            )

            SettingsToggleRow(
                label = stringResource(R.string.settings_vibration),
                checked = vibrationEnabled,
                onCheckedChange = onVibrationEnabledChange,
            )

            BeepCountSlider(
                beepCount = beepCount,
                enabled = soundEnabled,
                onBeepCountChange = onBeepCountChange,
            )

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
