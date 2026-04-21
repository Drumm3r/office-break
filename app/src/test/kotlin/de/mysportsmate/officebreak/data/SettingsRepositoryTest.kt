package de.mysportsmate.officebreak.data

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var dataStore: FakeDataStore
    private lateinit var repository: SettingsRepository

    private val defaultHomeWorkout = listOf(
        Exercise(name = "Push Ups", nameResKey = "exercise_push_ups"),
        Exercise(name = "Squats", nameResKey = "exercise_squats"),
    )

    private val defaultHomeMobility = listOf(
        Exercise(name = "Cat-Cow Stretch", nameResKey = "exercise_cat_cow"),
    )

    private val defaultOffice = listOf(
        Exercise(name = "Neck Stretch", nameResKey = "exercise_neck_stretch"),
    )

    private val defaultExercisesByMode = mapOf(
        ExerciseMode.HOME_WORKOUT to defaultHomeWorkout,
        ExerciseMode.HOME_MOBILITY to defaultHomeMobility,
        ExerciseMode.OFFICE to defaultOffice,
    )

    @Before
    fun setUp() {
        dataStore = FakeDataStore()
        repository = SettingsRepository(
            dataStore = dataStore,
            defaultExercisesByMode = defaultExercisesByMode,
        )
    }

    @Test
    fun `timerHours emits default when empty`() = runTest {
        repository.timerHours.test {
            assertEquals(SettingsRepository.DEFAULT_HOURS, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setTimerHours persists and re-emits`() = runTest {
        repository.timerHours.test {
            assertEquals(SettingsRepository.DEFAULT_HOURS, awaitItem())

            repository.setTimerHours(2)
            assertEquals(2, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `timerMinutes emits default when empty`() = runTest {
        repository.timerMinutes.test {
            assertEquals(SettingsRepository.DEFAULT_MINUTES, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setTimerMinutes persists and re-emits`() = runTest {
        repository.timerMinutes.test {
            assertEquals(SettingsRepository.DEFAULT_MINUTES, awaitItem())

            repository.setTimerMinutes(45)
            assertEquals(45, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `repsMin emits default when empty`() = runTest {
        repository.repsMin.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MIN, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setRepsMin persists and re-emits`() = runTest {
        repository.repsMin.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MIN, awaitItem())

            repository.setRepsMin(20)
            assertEquals(20, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `repsMax emits default when empty`() = runTest {
        repository.repsMax.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MAX, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setRepsMax persists and re-emits`() = runTest {
        repository.repsMax.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MAX, awaitItem())

            repository.setRepsMax(30)
            assertEquals(30, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `repsLinked emits default when empty`() = runTest {
        repository.repsLinked.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_LINKED, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setRepsLinked persists and re-emits`() = runTest {
        repository.repsLinked.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_LINKED, awaitItem())

            repository.setRepsLinked(false)
            assertEquals(false, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `exercises emits defaults when empty`() = runTest {
        repository.exercises.test {
            assertEquals(defaultHomeWorkout, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setExercises persists and re-emits`() = runTest {
        val newExercises = listOf(Exercise(name = "Custom Exercise", isEnabled = false))

        repository.exercises.test {
            assertEquals(defaultHomeWorkout, awaitItem())

            repository.setExercises(newExercises)
            assertEquals(newExercises, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `exercises falls back to defaults on corrupt JSON`() = runTest {
        val corruptStore = FakeDataStore()
        corruptStore.updateData { prefs ->
            val mutable = prefs.toMutablePreferences()
            mutable[androidx.datastore.preferences.core.stringPreferencesKey("exercises")] = "not valid json"
            mutable
        }
        val repo = SettingsRepository(dataStore = corruptStore, defaultExercisesByMode = defaultExercisesByMode)

        repo.exercises.test {
            assertEquals(defaultHomeWorkout, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `language emits default when empty`() = runTest {
        repository.language.test {
            assertEquals(SettingsRepository.LANGUAGE_SYSTEM, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setLanguage persists and re-emits`() = runTest {
        repository.language.test {
            assertEquals(SettingsRepository.LANGUAGE_SYSTEM, awaitItem())

            repository.setLanguage(SettingsRepository.LANGUAGE_DE)
            assertEquals(SettingsRepository.LANGUAGE_DE, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `usedExerciseNames emits empty set when empty`() = runTest {
        repository.usedExerciseNames.test {
            assertEquals(emptySet<String>(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setUsedExerciseNames persists and re-emits`() = runTest {
        repository.usedExerciseNames.test {
            assertEquals(emptySet<String>(), awaitItem())

            repository.setUsedExerciseNames(setOf("Push Ups", "Squats"))
            assertEquals(setOf("Push Ups", "Squats"), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `lastPickedName emits null when empty`() = runTest {
        repository.lastPickedName.test {
            assertEquals(null, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setLastPickedName persists and re-emits`() = runTest {
        repository.lastPickedName.test {
            assertEquals(null, awaitItem())

            repository.setLastPickedName("Push Ups")
            assertEquals("Push Ups", awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onboardingCompleted emits false when datastore is empty`() = runTest {
        repository.onboardingCompleted.test {
            assertEquals(false, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onboardingCompleted emits true when other prefs exist`() = runTest {
        repository.setTimerHours(1)

        repository.onboardingCompleted.test {
            assertEquals(true, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setOnboardingCompleted persists and re-emits`() = runTest {
        repository.onboardingCompleted.test {
            assertEquals(false, awaitItem())

            repository.setOnboardingCompleted(true)
            assertEquals(true, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setLastPickedName clears with null`() = runTest {
        repository.lastPickedName.test {
            assertEquals(null, awaitItem())

            repository.setLastPickedName("Push Ups")
            assertEquals("Push Ups", awaitItem())

            repository.setLastPickedName(null)
            assertEquals(null, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Additional Flow coverage ---

    @Test
    fun `weekSchedule emits DEFAULT_WEEK_SCHEDULE when empty`() = runTest {
        repository.weekSchedule.test {
            assertEquals(DEFAULT_WEEK_SCHEDULE, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setWeekSchedule persists and re-emits`() = runTest {
        val customSchedule = listOf(
            DaySchedule(enabled = true, linked = false, workStartHour = 9),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
        )

        repository.weekSchedule.test {
            assertEquals(DEFAULT_WEEK_SCHEDULE, awaitItem())

            repository.setWeekSchedule(customSchedule)
            assertEquals(customSchedule, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `weekSchedule falls back to default on corrupt JSON`() = runTest {
        val corruptStore = FakeDataStore()
        corruptStore.updateData { prefs ->
            val mutable = prefs.toMutablePreferences()
            mutable[androidx.datastore.preferences.core.stringPreferencesKey("week_schedule")] = "not valid json"
            mutable
        }
        val repo = SettingsRepository(dataStore = corruptStore, defaultExercisesByMode = defaultExercisesByMode)

        repo.weekSchedule.test {
            assertEquals(DEFAULT_WEEK_SCHEDULE, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `weekSchedule migrates old flat keys when present`() = runTest {
        val migrateStore = FakeDataStore()
        migrateStore.updateData { prefs ->
            val mutable = prefs.toMutablePreferences()
            mutable[androidx.datastore.preferences.core.intPreferencesKey("work_start_hour")] = 9
            mutable[androidx.datastore.preferences.core.intPreferencesKey("work_start_minute")] = 30
            mutable[androidx.datastore.preferences.core.intPreferencesKey("work_end_hour")] = 18
            mutable[androidx.datastore.preferences.core.intPreferencesKey("work_end_minute")] = 0
            mutable[androidx.datastore.preferences.core.intPreferencesKey("lunch_start_hour")] = 12
            mutable[androidx.datastore.preferences.core.intPreferencesKey("lunch_start_minute")] = 30
            mutable[androidx.datastore.preferences.core.intPreferencesKey("lunch_end_hour")] = 13
            mutable[androidx.datastore.preferences.core.intPreferencesKey("lunch_end_minute")] = 30
            mutable
        }
        val repo = SettingsRepository(dataStore = migrateStore, defaultExercisesByMode = defaultExercisesByMode)

        repo.weekSchedule.test {
            val schedule = awaitItem()
            assertEquals(7, schedule.size)
            // Monday is the base day (non-linked)
            val monday = schedule[0]
            assertEquals(9, monday.workStartHour)
            assertEquals(30, monday.workStartMinute)
            assertEquals(18, monday.workEndHour)
            assertEquals(12, monday.lunchStartHour)
            assertEquals(30, monday.lunchStartMinute)
            assertEquals(13, monday.lunchEndHour)
            assertEquals(30, monday.lunchEndMinute)
            assertTrue(!monday.linked)
            assertTrue(monday.enabled)
            // Tue-Fri are linked
            for (i in 1..4) {
                assertTrue(schedule[i].linked)
                assertTrue(schedule[i].enabled)
            }
            // Weekend off
            assertTrue(!schedule[5].enabled)
            assertTrue(!schedule[6].enabled)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `dynamicIncreaseEnabled emits default when empty`() = runTest {
        repository.dynamicIncreaseEnabled.test {
            assertEquals(SettingsRepository.DEFAULT_DYNAMIC_INCREASE_ENABLED, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setDynamicIncreaseEnabled persists and re-emits`() = runTest {
        repository.dynamicIncreaseEnabled.test {
            assertEquals(true, awaitItem())

            repository.setDynamicIncreaseEnabled(false)
            assertEquals(false, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `breaksSinceLastIncrease emits default when empty`() = runTest {
        repository.breaksSinceLastIncrease.test {
            assertEquals(SettingsRepository.DEFAULT_BREAKS_SINCE_LAST_INCREASE, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setBreaksSinceLastIncrease persists and re-emits`() = runTest {
        repository.breaksSinceLastIncrease.test {
            assertEquals(0, awaitItem())

            repository.setBreaksSinceLastIncrease(7)
            assertEquals(7, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `ttsEnabled emits default when empty`() = runTest {
        repository.ttsEnabled.test {
            assertEquals(SettingsRepository.DEFAULT_TTS_ENABLED, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setTtsEnabled persists and re-emits`() = runTest {
        repository.ttsEnabled.test {
            assertEquals(false, awaitItem())

            repository.setTtsEnabled(true)
            assertEquals(true, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `vibrationEnabled emits default when empty`() = runTest {
        repository.vibrationEnabled.test {
            assertEquals(SettingsRepository.DEFAULT_VIBRATION_ENABLED, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setVibrationEnabled persists and re-emits`() = runTest {
        repository.vibrationEnabled.test {
            assertEquals(true, awaitItem())

            repository.setVibrationEnabled(false)
            assertEquals(false, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `keepScreenOn emits default when empty`() = runTest {
        repository.keepScreenOn.test {
            assertEquals(SettingsRepository.DEFAULT_KEEP_SCREEN_ON, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setKeepScreenOn persists and re-emits`() = runTest {
        repository.keepScreenOn.test {
            assertEquals(false, awaitItem())

            repository.setKeepScreenOn(true)
            assertEquals(true, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `workScheduleEnabled emits default when empty`() = runTest {
        repository.workScheduleEnabled.test {
            assertEquals(SettingsRepository.DEFAULT_WORK_SCHEDULE_ENABLED, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setWorkScheduleEnabled persists and re-emits`() = runTest {
        repository.workScheduleEnabled.test {
            assertEquals(false, awaitItem())

            repository.setWorkScheduleEnabled(true)
            assertEquals(true, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `customSoundUri emits null when empty`() = runTest {
        repository.customSoundUri.test {
            assertNull(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setCustomSoundUri persists and re-emits`() = runTest {
        repository.customSoundUri.test {
            assertNull(awaitItem())

            repository.setCustomSoundUri("content://test/sound.mp3")
            assertEquals("content://test/sound.mp3", awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setCustomSoundUri with null removes value`() = runTest {
        repository.customSoundUri.test {
            assertNull(awaitItem())

            repository.setCustomSoundUri("content://test/sound.mp3")
            assertEquals("content://test/sound.mp3", awaitItem())

            repository.setCustomSoundUri(null)
            assertNull(awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `beepVolume emits default when empty`() = runTest {
        repository.beepVolume.test {
            assertEquals(SettingsRepository.DEFAULT_BEEP_VOLUME, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `beepCount emits default when empty`() = runTest {
        repository.beepCount.test {
            assertEquals(SettingsRepository.DEFAULT_BEEP_COUNT, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `themeMode emits default when empty`() = runTest {
        repository.themeMode.test {
            assertEquals(SettingsRepository.THEME_SYSTEM, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `autoRestart emits default when empty`() = runTest {
        repository.autoRestart.test {
            assertEquals(SettingsRepository.DEFAULT_AUTO_RESTART, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `usedExerciseNames falls back to empty set on corrupt JSON`() = runTest {
        val corruptStore = FakeDataStore()
        corruptStore.updateData { prefs ->
            val mutable = prefs.toMutablePreferences()
            mutable[androidx.datastore.preferences.core.stringPreferencesKey("used_exercise_names")] = "not valid"
            mutable
        }
        val repo = SettingsRepository(dataStore = corruptStore, defaultExercisesByMode = defaultExercisesByMode)

        repo.usedExerciseNames.test {
            assertEquals(emptySet<String>(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Snapshot / Restore ---

    @Test
    fun `snapshotForExport uses defaults when datastore is empty`() = runTest {
        val snapshot = repository.snapshotForExport()
        assertEquals(SettingsRepository.DEFAULT_HOURS, snapshot.timerHours)
        assertEquals(SettingsRepository.DEFAULT_MINUTES, snapshot.timerMinutes)
        assertEquals(SettingsRepository.DEFAULT_REPS_MIN, snapshot.repsMin)
        assertEquals(SettingsRepository.DEFAULT_REPS_MAX, snapshot.repsMax)
        assertEquals(SettingsRepository.DEFAULT_REPS_LINKED, snapshot.repsLinked)
        assertEquals(SettingsRepository.LANGUAGE_SYSTEM, snapshot.language)
        assertEquals(SettingsRepository.THEME_SYSTEM, snapshot.themeMode)
        assertEquals(SettingsRepository.DEFAULT_BEEP_VOLUME, snapshot.beepVolume)
        assertEquals(SettingsRepository.DEFAULT_VIBRATION_ENABLED, snapshot.vibrationEnabled)
        assertEquals(SettingsRepository.DEFAULT_BEEP_COUNT, snapshot.beepCount)
        assertEquals(SettingsRepository.DEFAULT_KEEP_SCREEN_ON, snapshot.keepScreenOn)
        assertEquals(SettingsRepository.DEFAULT_AUTO_RESTART, snapshot.autoRestart)
        assertEquals(SettingsRepository.DEFAULT_DYNAMIC_INCREASE_ENABLED, snapshot.dynamicIncreaseEnabled)
        assertEquals(SettingsRepository.DEFAULT_BREAKS_SINCE_LAST_INCREASE, snapshot.breaksSinceLastIncrease)
        assertEquals(SettingsRepository.DEFAULT_TTS_ENABLED, snapshot.ttsEnabled)
        assertNull(snapshot.customSoundUri)
        assertEquals(SettingsRepository.DEFAULT_WORK_SCHEDULE_ENABLED, snapshot.workScheduleEnabled)
    }

    @Test
    fun `snapshotForExport captures all current settings`() = runTest {
        repository.setTimerHours(2)
        repository.setTimerMinutes(45)
        repository.setRepsMin(5)
        repository.setRepsMax(20)
        repository.setRepsLinked(false)
        repository.setLanguage("de")
        repository.setThemeMode("dark")
        repository.setBeepVolume(50)
        repository.setVibrationEnabled(false)
        repository.setBeepCount(4)
        repository.setKeepScreenOn(true)
        repository.setAutoRestart(false)
        repository.setDynamicIncreaseEnabled(false)
        repository.setBreaksSinceLastIncrease(7)
        repository.setTtsEnabled(true)
        repository.setCustomSoundUri("content://test/uri")
        repository.setWorkScheduleEnabled(true)

        val snapshot = repository.snapshotForExport()
        assertEquals(2, snapshot.timerHours)
        assertEquals(45, snapshot.timerMinutes)
        assertEquals(5, snapshot.repsMin)
        assertEquals(20, snapshot.repsMax)
        assertEquals(false, snapshot.repsLinked)
        assertEquals("de", snapshot.language)
        assertEquals("dark", snapshot.themeMode)
        assertEquals(50, snapshot.beepVolume)
        assertEquals(false, snapshot.vibrationEnabled)
        assertEquals(4, snapshot.beepCount)
        assertEquals(true, snapshot.keepScreenOn)
        assertEquals(false, snapshot.autoRestart)
        assertEquals(false, snapshot.dynamicIncreaseEnabled)
        assertEquals(7, snapshot.breaksSinceLastIncrease)
        assertEquals(true, snapshot.ttsEnabled)
        assertEquals("content://test/uri", snapshot.customSoundUri)
        assertEquals(true, snapshot.workScheduleEnabled)
    }

    @Test
    fun `restoreFromBackup persists all settings fields`() = runTest {
        val backupData = BackupData(
            exportTimestamp = 1000L,
            appVersionCode = 5,
            timerHours = 1,
            timerMinutes = 45,
            repsMin = 5,
            repsMax = 15,
            repsLinked = false,
            exercises = listOf(Exercise(name = "Plank")),
            language = "de",
            themeMode = "dark",
            beepVolume = 50,
            vibrationEnabled = false,
            beepCount = 2,
            keepScreenOn = true,
            autoRestart = false,
            dynamicIncreaseEnabled = false,
            breaksSinceLastIncrease = 7,
            ttsEnabled = true,
            workScheduleEnabled = true,
            weekSchedule = DEFAULT_WEEK_SCHEDULE,
            trackingEnabled = true,
            breakRecords = emptyList(),
            dailyAggregates = emptyList(),
            yearlyAggregates = emptyList(),
            statsSnapshot = StatsSnapshot(),
            achievementState = AchievementState(),
        )

        repository.restoreFromBackup(backupData)

        assertEquals(1, repository.timerHours.first())
        assertEquals(45, repository.timerMinutes.first())
        assertEquals(5, repository.repsMin.first())
        assertEquals(15, repository.repsMax.first())
        assertEquals(false, repository.repsLinked.first())
        assertEquals("de", repository.language.first())
        assertEquals("dark", repository.themeMode.first())
        assertEquals(50, repository.beepVolume.first())
        assertEquals(false, repository.vibrationEnabled.first())
        assertEquals(2, repository.beepCount.first())
        assertEquals(true, repository.keepScreenOn.first())
        assertEquals(false, repository.autoRestart.first())
        assertEquals(false, repository.dynamicIncreaseEnabled.first())
        assertEquals(7, repository.breaksSinceLastIncrease.first())
        assertEquals(true, repository.ttsEnabled.first())
        assertEquals(true, repository.workScheduleEnabled.first())
        assertEquals(true, repository.onboardingCompleted.first())
    }

    @Test
    fun `restoreFromBackup with weekSchedule persists schedule`() = runTest {
        val customSchedule = listOf(
            DaySchedule(enabled = true, linked = false, workStartHour = 10),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
        )
        val backupData = BackupDataFixtures.minimal().copy(weekSchedule = customSchedule)

        repository.restoreFromBackup(backupData)

        assertEquals(customSchedule, repository.weekSchedule.first())
    }

    @Test
    fun `restoreFromBackup with empty weekSchedule migrates from flat fields`() = runTest {
        val backupData = BackupDataFixtures.minimal().copy(
            weekSchedule = emptyList(),
            workStartHour = 9,
            workStartMinute = 30,
            workEndHour = 18,
            workEndMinute = 0,
            lunchStartHour = 12,
            lunchStartMinute = 30,
            lunchEndHour = 13,
            lunchEndMinute = 30,
        )

        repository.restoreFromBackup(backupData)

        val schedule = repository.weekSchedule.first()
        assertEquals(7, schedule.size)
        assertEquals(9, schedule[0].workStartHour)
        assertEquals(30, schedule[0].workStartMinute)
        assertEquals(18, schedule[0].workEndHour)
        assertTrue(!schedule[0].linked)
        assertTrue(schedule[1].linked)
        assertTrue(!schedule[5].enabled)
    }

    @Test
    fun `restoreFromBackup does not restore customSoundUri`() = runTest {
        repository.setCustomSoundUri("content://original/sound")
        val backupData = BackupDataFixtures.minimal().copy(customSoundUri = "content://backup/sound")

        repository.restoreFromBackup(backupData)

        // customSoundUri is device-specific, original value should persist
        assertEquals("content://original/sound", repository.customSoundUri.first())
    }

    @Test
    fun `restoreFromBackup sets onboardingCompleted to true`() = runTest {
        assertEquals(false, repository.onboardingCompleted.first())

        repository.restoreFromBackup(BackupDataFixtures.minimal())

        assertEquals(true, repository.onboardingCompleted.first())
    }

    // --- Exercise name-to-resKey migration ---

    @Test
    fun `exercises flow adds nameResKey for known English names`() = runTest {
        repository.setExercises(listOf(Exercise(name = "Push Ups", nameResKey = null)))

        repository.exercises.test {
            val exercises = awaitItem()
            assertEquals("exercise_push_ups", exercises[0].nameResKey)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `exercises flow adds nameResKey for known German names`() = runTest {
        repository.setExercises(listOf(Exercise(name = "Liegestütze", nameResKey = null)))

        repository.exercises.test {
            val exercises = awaitItem()
            assertEquals("exercise_push_ups", exercises[0].nameResKey)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `exercises flow preserves existing nameResKey`() = runTest {
        repository.setExercises(listOf(Exercise(name = "Push Ups", nameResKey = "custom_key")))

        repository.exercises.test {
            val exercises = awaitItem()
            assertEquals("custom_key", exercises[0].nameResKey)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `exercises flow leaves unknown names without nameResKey`() = runTest {
        repository.setExercises(listOf(Exercise(name = "My Rare Exercise", nameResKey = null)))

        repository.exercises.test {
            val exercises = awaitItem()
            assertNull(exercises[0].nameResKey)
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Exercise Mode ---

    @Test
    fun `exerciseMode emits HOME_WORKOUT when empty`() = runTest {
        repository.exerciseMode.test {
            assertEquals(ExerciseMode.HOME_WORKOUT, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setExerciseMode persists and re-emits`() = runTest {
        repository.exerciseMode.test {
            assertEquals(ExerciseMode.HOME_WORKOUT, awaitItem())

            repository.setExerciseMode(ExerciseMode.OFFICE)
            assertEquals(ExerciseMode.OFFICE, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `exercises flow reflects active mode`() = runTest {
        repository.exercises.test {
            assertEquals(defaultHomeWorkout, awaitItem())

            repository.setExerciseMode(ExerciseMode.OFFICE)
            assertEquals(defaultOffice, awaitItem())

            repository.setExerciseMode(ExerciseMode.HOME_MOBILITY)
            assertEquals(defaultHomeMobility, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setExercises writes to active mode key`() = runTest {
        repository.setExerciseMode(ExerciseMode.OFFICE)
        val custom = listOf(Exercise(name = "Custom Office"))
        repository.setExercises(custom)

        // Office mode should show custom
        assertEquals(custom, repository.exercisesForMode(ExerciseMode.OFFICE).first())
        // Home workout should still show defaults
        assertEquals(defaultHomeWorkout, repository.exercisesForMode(ExerciseMode.HOME_WORKOUT).first())
    }

    @Test
    fun `exercisesForMode returns defaults when key absent`() = runTest {
        assertEquals(defaultHomeMobility, repository.exercisesForMode(ExerciseMode.HOME_MOBILITY).first())
    }

    @Test
    fun `exercisesForMode HOME_WORKOUT falls back to legacy exercises key`() = runTest {
        // Simulate a legacy install: write to the old "exercises" key directly
        val legacyExercises = listOf(Exercise(name = "Legacy Push Ups"))
        dataStore.updateData { prefs ->
            val mutable = prefs.toMutablePreferences()
            mutable[androidx.datastore.preferences.core.stringPreferencesKey("exercises")] =
                AppJson.encodeToString(legacyExercises)
            mutable
        }

        val exercises = repository.exercisesForMode(ExerciseMode.HOME_WORKOUT).first()
        assertEquals("Legacy Push Ups", exercises[0].name)
    }

    @Test
    fun `restoreFromBackup restores per-mode exercises from v2`() = runTest {
        val homeWorkout = listOf(Exercise(name = "Plank", nameResKey = "exercise_plank"))
        val homeMobility = listOf(Exercise(name = "Cat-Cow Stretch", nameResKey = "exercise_cat_cow"))
        val office = listOf(Exercise(name = "Neck Stretch", nameResKey = "exercise_neck_stretch"))

        val backupData = BackupDataFixtures.minimal().copy(
            exerciseMode = ExerciseMode.OFFICE.name,
            exercisesHomeWorkout = homeWorkout,
            exercisesHomeMobility = homeMobility,
            exercisesOffice = office,
        )

        repository.restoreFromBackup(backupData)

        assertEquals(ExerciseMode.OFFICE, repository.exerciseMode.first())
        assertEquals(homeWorkout, repository.exercisesForMode(ExerciseMode.HOME_WORKOUT).first())
        assertEquals(homeMobility, repository.exercisesForMode(ExerciseMode.HOME_MOBILITY).first())
        assertEquals(office, repository.exercisesForMode(ExerciseMode.OFFICE).first())
    }

    @Test
    fun `restoreFromBackup maps v1 exercises to HOME_WORKOUT`() = runTest {
        // Use exercises with nameResKey to avoid migration adding it
        val v1Exercises = listOf(
            Exercise(name = "Push Ups", nameResKey = "exercise_push_ups"),
            Exercise(name = "Squats", nameResKey = "exercise_squats"),
        )
        val backupData = BackupDataFixtures.minimal().copy(
            exercises = v1Exercises,
            exercisesHomeWorkout = emptyList(),
        )

        repository.restoreFromBackup(backupData)

        assertEquals(ExerciseMode.HOME_WORKOUT, repository.exerciseMode.first())
        assertEquals(v1Exercises, repository.exercisesForMode(ExerciseMode.HOME_WORKOUT).first())
    }

    // --- autoModeByDayEnabled ---

    @Test
    fun `autoModeByDayEnabled emits default false when empty`() = runTest {
        repository.autoModeByDayEnabled.test {
            assertEquals(SettingsRepository.DEFAULT_AUTO_MODE_BY_DAY, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setAutoModeByDayEnabled true persists and re-emits`() = runTest {
        repository.autoModeByDayEnabled.test {
            assertEquals(false, awaitItem())
            repository.setAutoModeByDayEnabled(enabled = true, seedMode = ExerciseMode.OFFICE)
            assertEquals(true, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setAutoModeByDayEnabled false does not seed`() = runTest {
        // Start with known custom schedule - Wed has HOME_MOBILITY
        val schedule = DEFAULT_WEEK_SCHEDULE.toMutableList().apply {
            this[2] = this[2].copy(defaultMode = ExerciseMode.HOME_MOBILITY)
        }
        repository.setWeekSchedule(schedule)

        repository.setAutoModeByDayEnabled(enabled = false, seedMode = ExerciseMode.OFFICE)
        val persisted = repository.weekSchedule.first()
        assertEquals(ExerciseMode.HOME_MOBILITY, persisted[2].defaultMode)
    }

    @Test
    fun `setAutoModeByDayEnabled false to true seeds all days with seedMode`() = runTest {
        repository.setAutoModeByDayEnabled(enabled = true, seedMode = ExerciseMode.OFFICE)
        val persisted = repository.weekSchedule.first()
        assertEquals(7, persisted.size)
        persisted.forEach { day ->
            assertEquals(ExerciseMode.OFFICE, day.defaultMode)
        }
    }

    @Test
    fun `setAutoModeByDayEnabled true when already true does not re-seed`() = runTest {
        // Enable first with OFFICE
        repository.setAutoModeByDayEnabled(enabled = true, seedMode = ExerciseMode.OFFICE)
        // User customizes Wed
        val schedule = repository.weekSchedule.first().toMutableList().apply {
            this[2] = this[2].copy(defaultMode = ExerciseMode.HOME_MOBILITY)
        }
        repository.setWeekSchedule(schedule)

        // Re-enable with HOME_WORKOUT - should NOT overwrite
        repository.setAutoModeByDayEnabled(enabled = true, seedMode = ExerciseMode.HOME_WORKOUT)
        val persisted = repository.weekSchedule.first()
        assertEquals(ExerciseMode.HOME_MOBILITY, persisted[2].defaultMode)
        assertEquals(ExerciseMode.OFFICE, persisted[0].defaultMode)
    }

    @Test
    fun `setAutoModeByDayEnabled true with null seedMode does not overwrite schedule`() = runTest {
        val schedule = DEFAULT_WEEK_SCHEDULE.toMutableList().apply {
            this[0] = this[0].copy(defaultMode = ExerciseMode.HOME_MOBILITY)
        }
        repository.setWeekSchedule(schedule)

        repository.setAutoModeByDayEnabled(enabled = true, seedMode = null)
        val persisted = repository.weekSchedule.first()
        assertEquals(ExerciseMode.HOME_MOBILITY, persisted[0].defaultMode)
    }

    // --- onboardingCompleted fallback ---

    @Test
    fun `onboardingCompleted emits false when DataStore is empty`() = runTest {
        assertEquals(false, repository.onboardingCompleted.first())
    }

    @Test
    fun `onboardingCompleted emits false when only autoModeByDay written`() = runTest {
        // autoMode is not a usage-indicator - onboarding should still show
        repository.setAutoModeByDayEnabled(enabled = false)
        assertEquals(false, repository.onboardingCompleted.first())
    }

    @Test
    fun `onboardingCompleted emits true when explicitly set`() = runTest {
        repository.setOnboardingCompleted(true)
        assertEquals(true, repository.onboardingCompleted.first())
    }

    @Test
    fun `onboardingCompleted emits true when timer hours present (legacy migration)`() = runTest {
        repository.setTimerHours(1)
        assertEquals(true, repository.onboardingCompleted.first())
    }
}
