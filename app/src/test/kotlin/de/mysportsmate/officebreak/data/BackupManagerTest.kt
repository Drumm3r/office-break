package de.mysportsmate.officebreak.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Test
    fun `valid BackupData JSON can be deserialized`() {
        val original = createSampleBackupData()
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<BackupData>(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `invalid JSON fails deserialization`() {
        val invalidJson = "{ not valid json }"
        val result = try {
            json.decodeFromString<BackupData>(invalidJson)
            null
        } catch (_: Exception) {
            "error"
        }
        assertEquals("error", result)
    }

    @Test
    fun `empty JSON object uses defaults for nested types`() {
        val minimalJson = """
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
            "achievementState": {}
        }
        """.trimIndent()

        val decoded = json.decodeFromString<BackupData>(minimalJson)
        assertEquals(0, decoded.statsSnapshot.totalBreaksAllTime)
        assertTrue(decoded.achievementState.unlockedIds.isEmpty())
        assertTrue(decoded.breakRecords.isEmpty())
    }

    @Test
    fun `future format version is detectable`() {
        val data = createSampleBackupData().copy(formatVersion = 99)
        assertTrue(data.formatVersion > BackupData.CURRENT_FORMAT_VERSION)
    }

    @Test
    fun `current format version is accepted`() {
        val data = createSampleBackupData()
        assertTrue(data.formatVersion <= BackupData.CURRENT_FORMAT_VERSION)
    }

    @Test
    fun `roundtrip preserves all settings fields`() {
        val data = createSampleBackupData().copy(
            timerHours = 1,
            timerMinutes = 45,
            repsMin = 5,
            repsMax = 15,
            repsLinked = false,
            language = "de",
            themeMode = "dark",
            beepVolume = 50,
            vibrationEnabled = false,
            beepCount = 2,
            keepScreenOn = true,
            autoRestart = false,
            dynamicIncreaseEnabled = false,
            breaksSinceLastIncrease = 7,
        )
        val encoded = json.encodeToString(data)
        val decoded = json.decodeFromString<BackupData>(encoded)

        assertEquals(1, decoded.timerHours)
        assertEquals(45, decoded.timerMinutes)
        assertEquals(5, decoded.repsMin)
        assertEquals(15, decoded.repsMax)
        assertEquals(false, decoded.repsLinked)
        assertEquals("de", decoded.language)
        assertEquals("dark", decoded.themeMode)
        assertEquals(50, decoded.beepVolume)
        assertEquals(false, decoded.vibrationEnabled)
        assertEquals(2, decoded.beepCount)
        assertEquals(true, decoded.keepScreenOn)
        assertEquals(false, decoded.autoRestart)
        assertEquals(false, decoded.dynamicIncreaseEnabled)
        assertEquals(7, decoded.breaksSinceLastIncrease)
    }

    private fun createSampleBackupData() = BackupData(
        formatVersion = 1,
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
