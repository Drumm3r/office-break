package de.mysportsmate.officebreak.ui.screen

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.resolveEffectiveSchedule
import de.mysportsmate.officebreak.data.validated
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.ui.components.ConfirmImportDialog
import de.mysportsmate.officebreak.ui.components.VolumeBar
import de.mysportsmate.officebreak.ui.components.ConfirmResetStatsDialog
import java.time.LocalDate
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
    ttsEnabled: Boolean,
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
    onTtsEnabledChange: (Boolean) -> Unit,
    customSoundUri: String?,
    onCustomSoundSelected: (Uri) -> Unit,
    onCustomSoundCleared: () -> Unit,
    onCustomSoundPreview: (Int) -> Unit,
    isPreviewPlaying: Boolean,
    onStopPreview: () -> Unit,
    workScheduleEnabled: Boolean,
    weekSchedule: List<DaySchedule>,
    onWorkScheduleEnabledChange: (Boolean) -> Unit,
    onDayScheduleChange: (Int, DaySchedule) -> Unit,
    onTrackingEnabledChange: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    onExportToUri: (Uri) -> Unit,
    onImportFromUri: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    var showResetStatsDialog by rememberSaveable { mutableStateOf(false) }
    var showImportConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val soundPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onCustomSoundSelected(uri)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) onExportToUri(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImportFromUri(uri)
    }

    if (showResetStatsDialog) {
        ConfirmResetStatsDialog(
            onConfirm = {
                showResetStatsDialog = false
                onResetStats()
            },
            onDismiss = { showResetStatsDialog = false },
        )
    }

    if (showImportConfirmDialog) {
        ConfirmImportDialog(
            onConfirm = {
                showImportConfirmDialog = false
                importLauncher.launch(arrayOf("application/json", "application/octet-stream"))
            },
            onDismiss = { showImportConfirmDialog = false },
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

            Text(
                text = stringResource(R.string.settings_dynamic_increase_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_work_schedule),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SettingsToggleRow(
                label = stringResource(R.string.settings_work_schedule_enabled),
                checked = workScheduleEnabled,
                onCheckedChange = onWorkScheduleEnabledChange,
            )

            Text(
                text = stringResource(R.string.settings_work_schedule_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (workScheduleEnabled) {
                val dayNames = listOf(
                    stringResource(R.string.day_mon),
                    stringResource(R.string.day_tue),
                    stringResource(R.string.day_wed),
                    stringResource(R.string.day_thu),
                    stringResource(R.string.day_fri),
                    stringResource(R.string.day_sat),
                    stringResource(R.string.day_sun),
                )

                weekSchedule.forEachIndexed { index, day ->
                    DayScheduleRow(
                        dayName = dayNames[index],
                        day = day,
                        effectiveDay = resolveEffectiveSchedule(weekSchedule, index),
                        isFirstEnabled = weekSchedule.indexOfFirst { it.enabled } == index,
                        onDayChange = { onDayScheduleChange(index, it) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_notifications),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            VolumeBar(
                volume = beepVolume,
                onVolumeChange = { value ->
                    onBeepVolumeChange(value)
                    onBeepVolumePreview(value)
                },
                modifier = Modifier.fillMaxWidth(),
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

            SettingsToggleRow(
                label = stringResource(R.string.settings_tts_enabled),
                checked = ttsEnabled,
                onCheckedChange = onTtsEnabledChange,
            )

            Text(
                text = stringResource(R.string.settings_tts_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_notification_sound),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Text(
                text = stringResource(R.string.settings_notification_sound_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (customSoundUri != null) {
                val context = LocalContext.current
                val fileName = remember(customSoundUri) {
                    try {
                        context.contentResolver.query(
                            Uri.parse(customSoundUri),
                            arrayOf(OpenableColumns.DISPLAY_NAME),
                            null, null, null,
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                cursor.getString(
                                    cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME),
                                )
                            } else null
                        }
                    } catch (_: Exception) {
                        null
                    } ?: customSoundUri.substringAfterLast('/')
                }

                Text(
                    text = stringResource(R.string.settings_custom_sound_current, fileName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isPreviewPlaying) onStopPreview()
                            else onCustomSoundPreview(beepVolume)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (isPreviewPlaying) {
                                stringResource(R.string.settings_custom_sound_stop)
                            } else {
                                stringResource(R.string.settings_custom_sound_preview)
                            },
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onStopPreview()
                            onCustomSoundCleared()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(R.string.settings_custom_sound_reset))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { soundPickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.settings_custom_sound_select))
                }
            }

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
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_data),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            OutlinedButton(
                onClick = {
                    exportLauncher.launch("office-break-backup-${LocalDate.now()}.json")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.settings_export_data))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showImportConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.settings_import_data))
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
internal fun DayScheduleRow(
    dayName: String,
    day: DaySchedule,
    effectiveDay: DaySchedule?,
    isFirstEnabled: Boolean,
    onDayChange: (DaySchedule) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val displayDay = if (day.linked && !isFirstEnabled) effectiveDay ?: day else day
    val timesEditable = day.enabled && (!day.linked || isFirstEnabled)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = day.enabled) { expanded = !expanded }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = day.enabled,
                onCheckedChange = { onDayChange(day.copy(enabled = it)) },
                modifier = Modifier.padding(end = 8.dp),
            )

            Text(
                text = dayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = if (day.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )

            if (day.enabled && !isFirstEnabled) {
                IconButton(
                    onClick = {
                        val newLinked = !day.linked
                        onDayChange(day.copy(linked = newLinked))
                        if (!newLinked) expanded = true
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (day.linked) Icons.Outlined.Link else Icons.Outlined.LinkOff,
                        contentDescription = stringResource(R.string.settings_work_schedule_day_linked),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (day.enabled) {
                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (timesEditable) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = "%02d:%02d–%02d:%02d".format(
                            displayDay.workStartHour, displayDay.workStartMinute,
                            displayDay.workEndHour, displayDay.workEndMinute,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (timesEditable) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (expanded && day.enabled && timesEditable) {
            Column(modifier = Modifier.padding(start = 48.dp)) {
                TimePickerRow(
                    label = stringResource(R.string.settings_work_start),
                    hour = day.workStartHour,
                    minute = day.workStartMinute,
                    onTimeSelected = { h, m -> onDayChange(day.copy(workStartHour = h, workStartMinute = m).validated()) },
                )
                TimePickerRow(
                    label = stringResource(R.string.settings_work_end),
                    hour = day.workEndHour,
                    minute = day.workEndMinute,
                    onTimeSelected = { h, m -> onDayChange(day.copy(workEndHour = h, workEndMinute = m).validated()) },
                )
                TimePickerRow(
                    label = stringResource(R.string.settings_lunch_start),
                    hour = day.lunchStartHour,
                    minute = day.lunchStartMinute,
                    onTimeSelected = { h, m -> onDayChange(day.copy(lunchStartHour = h, lunchStartMinute = m).validated()) },
                )
                TimePickerRow(
                    label = stringResource(R.string.settings_lunch_end),
                    hour = day.lunchEndHour,
                    minute = day.lunchEndMinute,
                    onTimeSelected = { h, m -> onDayChange(day.copy(lunchEndHour = h, lunchEndMinute = m).validated()) },
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerRow(
    label: String,
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

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
        OutlinedButton(onClick = { showDialog = true }) {
            Text(text = "%02d:%02d".format(hour, minute))
        }
    }

    if (showDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )

        androidx.compose.ui.window.Dialog(onDismissRequest = { showDialog = false }) {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimePicker(state = timePickerState)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    ) {
                        TextButton(onClick = { showDialog = false }) {
                            Text(text = stringResource(R.string.reset_confirm_no))
                        }
                        TextButton(onClick = {
                            onTimeSelected(timePickerState.hour, timePickerState.minute)
                            showDialog = false
                        }) {
                            Text(text = stringResource(R.string.dialog_confirm_ok))
                        }
                    }
                }
            }
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
