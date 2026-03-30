package de.mysportsmate.officebreak.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupManagerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private lateinit var settingsDataStore: FakeDataStore
    private lateinit var statsDataStore: FakeDataStore
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        settingsDataStore = FakeDataStore()
        statsDataStore = FakeDataStore()
        val settingsRepo = SettingsRepository(dataStore = settingsDataStore)
        val statsRepo = StatsRepository(dataStore = statsDataStore)
        backupManager = BackupManager(settingsRepo, statsRepo)
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

    // --- Invalid import tests via BackupManager.restoreFromJson ---

    @Test
    fun `restoreFromJson with empty string returns error`() = runTest {
        val result = backupManager.restoreFromJson("")
        assertTrue(result is ImportResult.Error)
    }

    @Test
    fun `restoreFromJson with random non-JSON text returns error`() = runTest {
        val result = backupManager.restoreFromJson("this is not json at all!!")
        assertTrue(result is ImportResult.Error)
    }

    @Test
    fun `restoreFromJson with wrong JSON structure returns error`() = runTest {
        val result = backupManager.restoreFromJson("""{"foo": "bar"}""")
        assertTrue(result is ImportResult.Error)
    }

    @Test
    fun `restoreFromJson with future formatVersion returns error`() = runTest {
        val futureData = createSampleBackupData().copy(formatVersion = 999)
        val encoded = json.encodeToString(futureData)
        val result = backupManager.restoreFromJson(encoded)
        assertTrue(result is ImportResult.Error)
    }

    @Test
    fun `restoreFromJson with valid data returns success`() = runTest {
        val validData = createSampleBackupData()
        val encoded = json.encodeToString(validData)
        val result = backupManager.restoreFromJson(encoded)
        assertTrue(result is ImportResult.Success)
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
