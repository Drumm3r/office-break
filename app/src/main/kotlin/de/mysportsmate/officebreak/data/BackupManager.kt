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
            ttsEnabled = settings.ttsEnabled,
            customSoundUri = settings.customSoundUri,
            workScheduleEnabled = settings.workScheduleEnabled,
            weekSchedule = settings.weekSchedule,

            exerciseMode = settings.exerciseMode.name,
            exercisesHomeWorkout = settings.exercisesHomeWorkout,
            exercisesHomeMobility = settings.exercisesHomeMobility,
            exercisesOffice = settings.exercisesOffice,

            autoModeByDayEnabled = settings.autoModeByDayEnabled,

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

        val sanitized = data.copy(
            breakRecords = data.breakRecords.take(MAX_BREAK_RECORDS),
            dailyAggregates = data.dailyAggregates.take(MAX_DAILY_AGGREGATES),
            yearlyAggregates = data.yearlyAggregates.take(MAX_YEARLY_AGGREGATES),
            exercises = data.exercises.clampForImport(),
            exercisesHomeWorkout = data.exercisesHomeWorkout.clampForImport(),
            exercisesHomeMobility = data.exercisesHomeMobility.clampForImport(),
            exercisesOffice = data.exercisesOffice.clampForImport(),
        )

        settingsRepository.restoreFromBackup(sanitized)
        statsRepository.restoreFromBackup(sanitized)

        return ImportResult.Success
    }

    private fun List<Exercise>.clampForImport(): List<Exercise> =
        take(MAX_EXERCISES_PER_MODE).map { exercise ->
            exercise.copy(name = exercise.name.take(MAX_EXERCISE_NAME_LENGTH))
        }
}
