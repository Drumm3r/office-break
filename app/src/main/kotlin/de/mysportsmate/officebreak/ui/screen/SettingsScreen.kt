package de.mysportsmate.officebreak.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.BuildConfig
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.ExerciseMode
import de.mysportsmate.officebreak.data.resolveEffectiveSchedule
import de.mysportsmate.officebreak.data.validated
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.ui.components.ConfirmationDialog
import de.mysportsmate.officebreak.ui.components.LabeledSwitchRow
import de.mysportsmate.officebreak.ui.components.VolumeBar
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme
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
    autoModeByDayEnabled: Boolean,
    onWorkScheduleEnabledChange: (Boolean) -> Unit,
    onAutoModeByDayEnabledChange: (Boolean) -> Unit,
    onDayScheduleChange: (Int, DaySchedule) -> Unit,
    onTrackingEnabledChange: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    onExportToUri: (Uri) -> Unit,
    onImportFromUri: (Uri) -> Unit,
    onOpenKofi: () -> Unit,
    onOpenImprint: () -> Unit,
    onOpenPrivacy: () -> Unit,
    cloudBackupEnabled: Boolean,
    onCloudBackupEnabledChange: (Boolean) -> Unit,
    devModeEnabled: Boolean,
    settingsDump: String?,
    onDevModeEnabledChange: (Boolean) -> Unit,
    onResetDonationPrompt: () -> Unit,
    onResetOnboarding: () -> Unit,
    onShowDataStoreDump: () -> Unit,
    onDismissSettingsDump: () -> Unit,
    onClearAllData: () -> Unit,
    onBack: () -> Unit,
) {
    var showResetStatsDialog by rememberSaveable { mutableStateOf(false) }
    var showImportConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    var devTapCount by rememberSaveable { mutableStateOf(0) }
    val settingsContext = LocalContext.current
    val devModeEnabledToast = stringResource(R.string.dev_mode_enabled_toast)
    val devModeDisabledToast = stringResource(R.string.dev_mode_disabled_toast)
    val devResetDoneToast = stringResource(R.string.dev_reset_done)
    val devTapTarget = 7
    val showToast: (String) -> Unit = { msg ->
        Toast.makeText(settingsContext, msg, Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(devTapCount) {
        if (devTapCount in 1 until devTapTarget) {
            kotlinx.coroutines.delay(3_000)
            devTapCount = 0
        }
    }

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
        ConfirmationDialog(
            titleRes = R.string.reset_stats_confirm_title,
            messageRes = R.string.reset_stats_confirm_message,
            confirmRes = R.string.reset_stats_confirm_yes,
            dismissRes = R.string.reset_confirm_no,
            onConfirm = {
                showResetStatsDialog = false
                onResetStats()
            },
            onDismiss = { showResetStatsDialog = false },
        )
    }

    if (showImportConfirmDialog) {
        ConfirmationDialog(
            titleRes = R.string.import_confirm_title,
            messageRes = R.string.import_confirm_message,
            confirmRes = R.string.import_confirm_yes,
            dismissRes = R.string.reset_confirm_no,
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
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenKofi)
                    .padding(vertical = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_support_project),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_support_project_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() },
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
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() },
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
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() },
            )

            LabeledSwitchRow(
                label = stringResource(R.string.settings_keep_screen_on),
                checked = keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
            )

            LabeledSwitchRow(
                label = stringResource(R.string.settings_auto_restart),
                checked = autoRestart,
                onCheckedChange = onAutoRestartChange,
            )

            LabeledSwitchRow(
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
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() },
            )

            LabeledSwitchRow(
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

                LabeledSwitchRow(
                    label = stringResource(R.string.settings_auto_mode_by_day),
                    checked = autoModeByDayEnabled,
                    onCheckedChange = onAutoModeByDayEnabledChange,
                )

                Text(
                    text = stringResource(R.string.settings_auto_mode_by_day_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                weekSchedule.forEachIndexed { index, day ->
                    DayScheduleRow(
                        dayName = dayNames[index],
                        day = day,
                        effectiveDay = resolveEffectiveSchedule(weekSchedule, index),
                        isFirstEnabled = weekSchedule.indexOfFirst { it.enabled } == index,
                        showModeSelector = autoModeByDayEnabled,
                        dayNames = dayNames,
                        weekSchedule = weekSchedule,
                        dayIndex = index,
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
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() },
            )

            VolumeBar(
                volume = beepVolume,
                onVolumeChange = { value ->
                    onBeepVolumeChange(value)
                    onBeepVolumePreview(value)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledSwitchRow(
                label = stringResource(R.string.settings_vibration),
                checked = vibrationEnabled,
                onCheckedChange = onVibrationEnabledChange,
            )

            BeepCountSlider(
                beepCount = beepCount,
                enabled = beepVolume > 0,
                onBeepCountChange = onBeepCountChange,
            )

            LabeledSwitchRow(
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
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = null) {
                        if (devModeEnabled) return@clickable
                        devTapCount++
                        if (devTapCount >= devTapTarget) {
                            devTapCount = 0
                            onDevModeEnabledChange(true)
                            showToast(devModeEnabledToast)
                        }
                    }
                    .padding(bottom = 8.dp)
                    .semantics { heading() },
            )

            LabeledSwitchRow(
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
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() },
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

            if (BuildConfig.FLAVOR == "gplay") {
                Spacer(modifier = Modifier.height(16.dp))

                LabeledSwitchRow(
                    label = stringResource(R.string.settings_cloud_backup_enabled),
                    checked = cloudBackupEnabled,
                    onCheckedChange = onCloudBackupEnabledChange,
                )

                Text(
                    text = stringResource(R.string.settings_cloud_backup_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (devModeEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.dev_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .semantics { heading() },
                )

                DevModeActionRow(
                    title = stringResource(R.string.dev_reset_donation),
                    summary = stringResource(R.string.dev_reset_donation_summary),
                    onClick = {
                        onResetDonationPrompt()
                        showToast(devResetDoneToast)
                    },
                )

                DevModeActionRow(
                    title = stringResource(R.string.dev_reset_onboarding),
                    summary = stringResource(R.string.dev_reset_onboarding_summary),
                    onClick = {
                        onResetOnboarding()
                        showToast(devResetDoneToast)
                    },
                )

                DevModeActionRow(
                    title = stringResource(R.string.dev_show_dump),
                    summary = stringResource(R.string.dev_show_dump_summary),
                    onClick = onShowDataStoreDump,
                )

                DevModeActionRow(
                    title = stringResource(R.string.dev_clear_all),
                    summary = stringResource(R.string.dev_clear_all_summary),
                    destructive = true,
                    onClick = { showClearAllDialog = true },
                )

                DevModeActionRow(
                    title = stringResource(R.string.dev_disable),
                    summary = stringResource(R.string.dev_disable_summary),
                    onClick = {
                        onDevModeEnabledChange(false)
                        showToast(devModeDisabledToast)
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPrivacy)
                    .padding(vertical = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.settings_privacy),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenImprint)
                    .padding(vertical = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.settings_imprint),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showClearAllDialog) {
        ConfirmationDialog(
            titleRes = R.string.dev_clear_all_confirm_title,
            messageRes = R.string.dev_clear_all_confirm_message,
            confirmRes = R.string.dev_clear_all_confirm_yes,
            dismissRes = R.string.reset_confirm_no,
            onConfirm = {
                showClearAllDialog = false
                onClearAllData()
            },
            onDismiss = { showClearAllDialog = false },
        )
    }

    if (settingsDump != null) {
        SettingsDumpDialog(
            dump = settingsDump,
            onDismiss = onDismissSettingsDump,
        )
    }
}

@Composable
private fun DevModeActionRow(
    title: String,
    summary: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsDumpDialog(
    dump: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dumpCopiedToast = stringResource(R.string.dev_dump_copied)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dev_dump_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = dump,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("office-break state", dump))
                    Toast.makeText(
                        context,
                        dumpCopiedToast,
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            ) {
                Text(stringResource(R.string.dev_dump_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dev_dump_close))
            }
        },
    )
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
            label = { Text(stringResource(R.string.settings_language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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
            val beepCountLabel = stringResource(R.string.settings_beep_count)
            Slider(
                value = beepCount.toFloat(),
                onValueChange = { onBeepCountChange(it.roundToInt()) },
                valueRange = 1f..5f,
                steps = 3,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = beepCountLabel },
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun DayScheduleRow(
    dayName: String,
    day: DaySchedule,
    effectiveDay: DaySchedule?,
    isFirstEnabled: Boolean,
    showModeSelector: Boolean = false,
    dayNames: List<String> = emptyList(),
    weekSchedule: List<DaySchedule> = emptyList(),
    dayIndex: Int = -1,
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
                    modifier = Modifier.size(48.dp),
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
                        text = "%02d:%02d-%02d:%02d".format(
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
            if (showModeSelector) {
                DayModeSelector(
                    selected = day.defaultMode,
                    onSelect = { onDayChange(day.copy(defaultMode = it)) },
                )
            }
        }

        if (expanded && day.enabled && !timesEditable && showModeSelector) {
            val sourceIndex = (1..6).firstOrNull { i ->
                val idx = (dayIndex - i + 7) % 7
                weekSchedule.getOrNull(idx)?.let { it.enabled && !it.linked } == true
            }?.let { (dayIndex - it + 7) % 7 } ?: dayIndex
            val sourceName = dayNames.getOrNull(sourceIndex) ?: ""
            Column(modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 8.dp)) {
                Text(
                    text = stringResource(R.string.settings_day_mode_inherited, sourceName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = modeLabel(effectiveDay?.defaultMode ?: day.defaultMode),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DayModeSelector(
    selected: ExerciseMode,
    onSelect: (ExerciseMode) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.settings_day_mode_label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
        ) {
            ExerciseMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    modifier = Modifier.fillMaxHeight(),
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ExerciseMode.entries.size,
                    ),
                    icon = {},
                ) {
                    Text(
                        text = modeShortLabel(mode),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun modeShortLabel(mode: ExerciseMode): String = when (mode) {
    ExerciseMode.HOME_WORKOUT -> stringResource(R.string.exercise_mode_short_home_workout)
    ExerciseMode.HOME_MOBILITY -> stringResource(R.string.exercise_mode_short_home_mobility)
    ExerciseMode.OFFICE -> stringResource(R.string.exercise_mode_short_office)
}

@Composable
private fun modeLabel(mode: ExerciseMode): String = when (mode) {
    ExerciseMode.HOME_WORKOUT -> stringResource(R.string.exercise_mode_home_workout)
    ExerciseMode.HOME_MOBILITY -> stringResource(R.string.exercise_mode_home_mobility)
    ExerciseMode.OFFICE -> stringResource(R.string.exercise_mode_office)
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    OfficeBreakTheme {
        SettingsScreen(
            language = SettingsRepository.LANGUAGE_SYSTEM,
            beepVolume = 80,
            vibrationEnabled = true,
            themeMode = SettingsRepository.THEME_SYSTEM,
            keepScreenOn = false,
            autoRestart = true,
            dynamicIncreaseEnabled = false,
            beepCount = 3,
            ttsEnabled = false,
            trackingEnabled = true,
            onLanguageChange = {},
            onBeepVolumeChange = {},
            onBeepVolumePreview = {},
            onVibrationEnabledChange = {},
            onThemeModeChange = {},
            onKeepScreenOnChange = {},
            onAutoRestartChange = {},
            onDynamicIncreaseEnabledChange = {},
            onBeepCountChange = {},
            onTtsEnabledChange = {},
            customSoundUri = null,
            onCustomSoundSelected = {},
            onCustomSoundCleared = {},
            onCustomSoundPreview = {},
            isPreviewPlaying = false,
            onStopPreview = {},
            workScheduleEnabled = true,
            weekSchedule = DEFAULT_WEEK_SCHEDULE,
            autoModeByDayEnabled = false,
            onWorkScheduleEnabledChange = {},
            onAutoModeByDayEnabledChange = {},
            onDayScheduleChange = { _, _ -> },
            onTrackingEnabledChange = {},
            onResetStats = {},
            onExportToUri = {},
            onImportFromUri = {},
            onOpenKofi = {},
            onOpenImprint = {},
            onOpenPrivacy = {},
            cloudBackupEnabled = true,
            onCloudBackupEnabledChange = {},
            devModeEnabled = false,
            settingsDump = null,
            onDevModeEnabledChange = {},
            onResetDonationPrompt = {},
            onResetOnboarding = {},
            onShowDataStoreDump = {},
            onDismissSettingsDump = {},
            onClearAllData = {},
            onBack = {},
        )
    }
}
