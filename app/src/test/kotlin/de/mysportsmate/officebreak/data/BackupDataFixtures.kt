package de.mysportsmate.officebreak.data

/**
 * Shared minimal BackupData instance for test cases that don't need full control
 * over every field. Use `.copy(...)` to override specific fields per test.
 */
object BackupDataFixtures {
    fun minimal(): BackupData = BackupData(
        exportTimestamp = 1000L,
        appVersionCode = 5,
        timerHours = 0,
        timerMinutes = 30,
        repsMin = 10,
        repsMax = 10,
        repsLinked = true,
        exercises = emptyList(),
        language = "system",
        themeMode = "system",
        beepVolume = 80,
        vibrationEnabled = true,
        beepCount = 3,
        keepScreenOn = false,
        autoRestart = true,
        dynamicIncreaseEnabled = true,
        breaksSinceLastIncrease = 0,
        trackingEnabled = true,
        breakRecords = emptyList(),
        dailyAggregates = emptyList(),
        yearlyAggregates = emptyList(),
        statsSnapshot = StatsSnapshot(),
        achievementState = AchievementState(),
    )
}
