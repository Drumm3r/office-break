package de.mysportsmate.officebreak.data

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val exportTimestamp: Long,
    val appVersionCode: Int,

    // Settings
    val timerHours: Int,
    val timerMinutes: Int,
    val repsMin: Int,
    val repsMax: Int,
    val repsLinked: Boolean,
    val exercises: List<Exercise>,
    val language: String,
    val themeMode: String,
    val beepVolume: Int,
    val vibrationEnabled: Boolean,
    val beepCount: Int,
    val keepScreenOn: Boolean,
    val autoRestart: Boolean,
    val dynamicIncreaseEnabled: Boolean,
    val breaksSinceLastIncrease: Int,

    // Stats
    val trackingEnabled: Boolean,
    val breakRecords: List<BreakRecord>,
    val dailyAggregates: List<DailyAggregate>,
    val yearlyAggregates: List<YearlyAggregate>,
    val statsSnapshot: StatsSnapshot,
    val achievementState: AchievementState,
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

sealed interface ImportResult {
    data object Success : ImportResult
    data class Error(val messageResId: Int, val detail: String? = null) : ImportResult
}
