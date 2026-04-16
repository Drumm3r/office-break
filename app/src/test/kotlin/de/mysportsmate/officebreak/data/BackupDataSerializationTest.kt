package de.mysportsmate.officebreak.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDataSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `BackupData roundtrip serialization`() {
        val data = createSampleBackupData()
        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)
        assertEquals(data, decoded)
    }

    @Test
    fun `BackupData with unknown keys is deserialized`() {
        val rawJson = """
        {
            "formatVersion": 1,
            "exportTimestamp": 1000,
            "appVersionCode": 5,
            "timerHours": 0,
            "timerMinutes": 30,
            "repsMin": 10,
            "repsMax": 10,
            "repsLinked": true,
            "exercises": [],
            "language": "system",
            "themeMode": "system",
            "beepVolume": 80,
            "vibrationEnabled": true,
            "beepCount": 3,
            "keepScreenOn": false,
            "autoRestart": true,
            "dynamicIncreaseEnabled": true,
            "breaksSinceLastIncrease": 0,
            "trackingEnabled": true,
            "breakRecords": [],
            "dailyAggregates": [],
            "yearlyAggregates": [],
            "statsSnapshot": {},
            "achievementState": {},
            "futureField": "should be ignored"
        }
        """.trimIndent()

        val decoded = json.decodeFromString<BackupData>(rawJson)
        assertEquals(1, decoded.formatVersion)
        assertEquals(30, decoded.timerMinutes)
    }

    @Test
    fun `BackupData preserves exercises`() {
        val exercises = listOf(
            Exercise(name = "Push Ups", isEnabled = true, nameResKey = "exercise_push_ups"),
            Exercise(name = "Custom", isEnabled = false),
        )
        val data = createSampleBackupData().copy(exercises = exercises)
        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)
        assertEquals(2, decoded.exercises.size)
        assertEquals("Push Ups", decoded.exercises[0].name)
        assertTrue(decoded.exercises[0].isEnabled)
        assertEquals("Custom", decoded.exercises[1].name)
        assertEquals(false, decoded.exercises[1].isEnabled)
    }

    @Test
    fun `BackupData preserves stats snapshot`() {
        val snapshot = StatsSnapshot(
            totalBreaksAllTime = 42,
            totalRepsAllTime = 520,
            currentStreakDays = 5,
            longestStreakDays = 12,
            perExerciseCounts = mapOf("Push Ups" to 20),
            perExerciseReps = mapOf("Push Ups" to 240),
            uniqueExercisesUsed = setOf("Push Ups"),
            hasCreatedCustomExercise = true,
        )
        val data = createSampleBackupData().copy(statsSnapshot = snapshot)
        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)
        assertEquals(snapshot, decoded.statsSnapshot)
    }

    @Test
    fun `BackupData preserves achievement state`() {
        val state = AchievementState(
            unlockedIds = setOf("breaks_1", "breaks_10"),
            unlockTimestamps = mapOf("breaks_1" to 1000L, "breaks_10" to 2000L),
        )
        val data = createSampleBackupData().copy(achievementState = state)
        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)
        assertEquals(state, decoded.achievementState)
    }

    @Test
    fun `BackupData preserves break records and aggregates`() {
        val records = listOf(
            BreakRecord("Push Ups", 10, 1000L, "2026-03-30"),
            BreakRecord("Squats", 15, 2000L, "2026-03-30"),
        )
        val dailyAggregates = listOf(
            DailyAggregate("2026-03-29", 5, 60, mapOf("Push Ups" to 5), mapOf("Push Ups" to 60)),
        )
        val yearlyAggregates = listOf(
            YearlyAggregate(2025, 200, 2400, mapOf("Push Ups" to 200), mapOf("Push Ups" to 2400), 180),
        )

        val data = createSampleBackupData().copy(
            breakRecords = records,
            dailyAggregates = dailyAggregates,
            yearlyAggregates = yearlyAggregates,
        )
        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)
        assertEquals(records, decoded.breakRecords)
        assertEquals(dailyAggregates, decoded.dailyAggregates)
        assertEquals(yearlyAggregates, decoded.yearlyAggregates)
    }

    @Test
    fun `v2 BackupData roundtrip with exercise modes`() {
        val data = createSampleBackupData().copy(
            formatVersion = 2,
            exerciseMode = ExerciseMode.OFFICE.name,
            exercisesHomeWorkout = listOf(Exercise(name = "Push Ups")),
            exercisesHomeMobility = listOf(Exercise(name = "Cat-Cow Stretch")),
            exercisesOffice = listOf(Exercise(name = "Neck Stretch")),
        )
        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)
        assertEquals(ExerciseMode.OFFICE.name, decoded.exerciseMode)
        assertEquals(1, decoded.exercisesHomeWorkout.size)
        assertEquals(1, decoded.exercisesHomeMobility.size)
        assertEquals(1, decoded.exercisesOffice.size)
    }

    @Test
    fun `v1 BackupData without mode fields deserializes with defaults`() {
        val rawJson = """
        {
            "formatVersion": 1,
            "exportTimestamp": 1000,
            "appVersionCode": 5,
            "timerHours": 0,
            "timerMinutes": 30,
            "repsMin": 10,
            "repsMax": 10,
            "repsLinked": true,
            "exercises": [{"name":"Push Ups","isEnabled":true}],
            "language": "system",
            "themeMode": "system",
            "beepVolume": 80,
            "vibrationEnabled": true,
            "beepCount": 3,
            "keepScreenOn": false,
            "autoRestart": true,
            "dynamicIncreaseEnabled": true,
            "breaksSinceLastIncrease": 0,
            "trackingEnabled": true,
            "breakRecords": [],
            "dailyAggregates": [],
            "yearlyAggregates": [],
            "statsSnapshot": {},
            "achievementState": {}
        }
        """.trimIndent()

        val decoded = json.decodeFromString<BackupData>(rawJson)
        assertEquals(ExerciseMode.HOME_WORKOUT.name, decoded.exerciseMode)
        assertTrue(decoded.exercisesHomeWorkout.isEmpty())
        assertTrue(decoded.exercisesHomeMobility.isEmpty())
        assertTrue(decoded.exercisesOffice.isEmpty())
        assertEquals(1, decoded.exercises.size)
    }

    private fun createSampleBackupData() = BackupData(
        formatVersion = 2,
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
