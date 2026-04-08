package de.mysportsmate.officebreak.data

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var statsRepo: StatsRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        settingsDataStore = FakeDataStore()
        statsDataStore = FakeDataStore()
        settingsRepo = SettingsRepository(dataStore = settingsDataStore)
        statsRepo = StatsRepository(dataStore = statsDataStore)
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

    // --- createBackupJson tests ---

    @Test
    fun `createBackupJson produces valid JSON`() = runTest {
        val jsonString = backupManager.createBackupJson(1)
        val decoded = json.decodeFromString<BackupData>(jsonString)
        assertEquals(BackupData.CURRENT_FORMAT_VERSION, decoded.formatVersion)
    }

    @Test
    fun `createBackupJson captures current settings`() = runTest {
        settingsRepo.setTimerHours(2)
        settingsRepo.setTimerMinutes(45)
        settingsRepo.setRepsMin(5)
        settingsRepo.setRepsMax(20)
        settingsRepo.setLanguage("de")
        settingsRepo.setThemeMode("dark")

        val jsonString = backupManager.createBackupJson(10)
        val decoded = json.decodeFromString<BackupData>(jsonString)

        assertEquals(2, decoded.timerHours)
        assertEquals(45, decoded.timerMinutes)
        assertEquals(5, decoded.repsMin)
        assertEquals(20, decoded.repsMax)
        assertEquals("de", decoded.language)
        assertEquals("dark", decoded.themeMode)
    }

    @Test
    fun `createBackupJson captures stats data`() = runTest {
        val record = BreakRecord(
            exerciseName = "Push Ups",
            reps = 10,
            timestampMillis = 1000L,
            dateString = "2026-03-30",
        )
        statsRepo.recordBreak(record)

        val jsonString = backupManager.createBackupJson(1)
        val decoded = json.decodeFromString<BackupData>(jsonString)

        assertEquals(1, decoded.breakRecords.size)
        assertEquals("Push Ups", decoded.breakRecords[0].exerciseName)
        assertEquals(1, decoded.statsSnapshot.totalBreaksAllTime)
    }

    @Test
    fun `createBackupJson includes appVersionCode`() = runTest {
        val jsonString = backupManager.createBackupJson(42)
        val decoded = json.decodeFromString<BackupData>(jsonString)
        assertEquals(42, decoded.appVersionCode)
    }

    @Test
    fun `createBackupJson includes exportTimestamp`() = runTest {
        val jsonString = backupManager.createBackupJson(1)
        val decoded = json.decodeFromString<BackupData>(jsonString)
        assertTrue(decoded.exportTimestamp > 0)
    }

    @Test
    fun `createBackupJson roundtrip through restoreFromJson preserves settings`() = runTest {
        settingsRepo.setTimerHours(1)
        settingsRepo.setTimerMinutes(45)
        settingsRepo.setRepsMin(5)
        settingsRepo.setRepsMax(20)
        settingsRepo.setRepsLinked(false)
        settingsRepo.setBeepVolume(60)

        val exported = backupManager.createBackupJson(1)

        // Create fresh repos to restore into
        val newSettingsStore = FakeDataStore()
        val newStatsStore = FakeDataStore()
        val newSettingsRepo = SettingsRepository(dataStore = newSettingsStore)
        val newStatsRepo = StatsRepository(dataStore = newStatsStore)
        val newManager = BackupManager(newSettingsRepo, newStatsRepo)

        val result = newManager.restoreFromJson(exported)
        assertTrue(result is ImportResult.Success)

        assertEquals(1, newSettingsRepo.timerHours.first())
        assertEquals(45, newSettingsRepo.timerMinutes.first())
        assertEquals(5, newSettingsRepo.repsMin.first())
        assertEquals(20, newSettingsRepo.repsMax.first())
        assertEquals(false, newSettingsRepo.repsLinked.first())
        assertEquals(60, newSettingsRepo.beepVolume.first())
    }

    @Test
    fun `createBackupJson roundtrip through restoreFromJson preserves stats`() = runTest {
        val record = BreakRecord(
            exerciseName = "Squats",
            reps = 15,
            timestampMillis = 2000L,
            dateString = "2026-03-30",
        )
        statsRepo.recordBreak(record)

        val exported = backupManager.createBackupJson(1)

        val newSettingsStore = FakeDataStore()
        val newStatsStore = FakeDataStore()
        val newSettingsRepo = SettingsRepository(dataStore = newSettingsStore)
        val newStatsRepo = StatsRepository(dataStore = newStatsStore)
        val newManager = BackupManager(newSettingsRepo, newStatsRepo)

        val result = newManager.restoreFromJson(exported)
        assertTrue(result is ImportResult.Success)

        val snapshot = newStatsRepo.statsSnapshot.first()
        assertEquals(1, snapshot.totalBreaksAllTime)
        assertEquals(15, snapshot.totalRepsAllTime)
    }

    @Test
    fun `restoreFromJson with valid data restores settings to repository`() = runTest {
        val data = createSampleBackupData().copy(
            timerHours = 2,
            timerMinutes = 30,
            language = "en",
        )
        val encoded = json.encodeToString(data)

        val result = backupManager.restoreFromJson(encoded)
        assertTrue(result is ImportResult.Success)

        assertEquals(2, settingsRepo.timerHours.first())
        assertEquals(30, settingsRepo.timerMinutes.first())
        assertEquals("en", settingsRepo.language.first())
    }

    @Test
    fun `restoreFromJson with valid data restores stats to repository`() = runTest {
        val statsSnapshot = StatsSnapshot(
            totalBreaksAllTime = 42,
            totalRepsAllTime = 420,
        )
        val data = createSampleBackupData().copy(statsSnapshot = statsSnapshot)
        val encoded = json.encodeToString(data)

        val result = backupManager.restoreFromJson(encoded)
        assertTrue(result is ImportResult.Success)

        val restored = statsRepo.statsSnapshot.first()
        assertEquals(42, restored.totalBreaksAllTime)
        assertEquals(420, restored.totalRepsAllTime)
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
