package de.mysportsmate.officebreak.data

import de.mysportsmate.officebreak.R
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import android.util.Log

class BackupManager(
    private val settingsRepository: SettingsRepository,
    private val statsRepository: StatsRepository,
) {

    private val json = Json(from = AppJson) { prettyPrint = true }

    suspend fun createBackupJson(appVersionCode: Int): String {
        val settings = settingsRepository.snapshotForExport()
        val stats = statsRepository.snapshotForExport()

        val data = BackupData(
            exportTimestamp = System.currentTimeMillis(),
            appVersionCode = appVersionCode,

            timerHours = settings.timerHours,
            timerMinutes = settings.timerMinutes,
            repsMin = settings.repsMin,
            repsMax = settings.repsMax,
            repsLinked = settings.repsLinked,
            exercises = settings.exercises,
            language = settings.language,
            themeMode = settings.themeMode,
            beepVolume = settings.beepVolume,
            vibrationEnabled = settings.vibrationEnabled,
            beepCount = settings.beepCount,
            keepScreenOn = settings.keepScreenOn,
            autoRestart = settings.autoRestart,
            dynamicIncreaseEnabled = settings.dynamicIncreaseEnabled,
            breaksSinceLastIncrease = settings.breaksSinceLastIncrease,

            trackingEnabled = stats.trackingEnabled,
            breakRecords = stats.breakRecords,
            dailyAggregates = stats.dailyAggregates,
            yearlyAggregates = stats.yearlyAggregates,
            statsSnapshot = stats.statsSnapshot,
            achievementState = stats.achievementState,
        )

        return json.encodeToString(BackupData.serializer(), data)
    }

    suspend fun restoreFromJson(jsonString: String): ImportResult {
        val data = try {
            json.decodeFromString<BackupData>(jsonString)
        } catch (_: SerializationException) {
            return ImportResult.Error(R.string.import_error_invalid_format)
        } catch (_: IllegalArgumentException) {
            return ImportResult.Error(R.string.import_error_invalid_format)
        }

        if (data.formatVersion > BackupData.CURRENT_FORMAT_VERSION) {
            return ImportResult.Error(R.string.import_error_newer_version)
        }

        settingsRepository.restoreFromBackup(data)
        statsRepository.restoreFromBackup(data)

        return ImportResult.Success
    }
}
