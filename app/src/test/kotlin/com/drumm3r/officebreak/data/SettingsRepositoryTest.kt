package com.drumm3r.officebreak.data

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var dataStore: FakeDataStore
    private lateinit var repository: SettingsRepository

    private val defaultExercises = listOf(
        Exercise(name = "Push Ups"),
        Exercise(name = "Squats"),
    )

    @Before
    fun setUp() {
        dataStore = FakeDataStore()
        repository = SettingsRepository(
            dataStore = dataStore,
            defaultExercises = defaultExercises,
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
    fun `reps emits default when empty`() = runTest {
        repository.reps.test {
            assertEquals(SettingsRepository.DEFAULT_REPS, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setReps persists and re-emits`() = runTest {
        repository.reps.test {
            assertEquals(SettingsRepository.DEFAULT_REPS, awaitItem())

            repository.setReps(20)
            assertEquals(20, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `exercises emits defaults when empty`() = runTest {
        repository.exercises.test {
            assertEquals(defaultExercises, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setExercises persists and re-emits`() = runTest {
        val newExercises = listOf(Exercise(name = "Lunges", isEnabled = false))

        repository.exercises.test {
            assertEquals(defaultExercises, awaitItem())

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
        val repo = SettingsRepository(dataStore = corruptStore, defaultExercises = defaultExercises)

        repo.exercises.test {
            assertEquals(defaultExercises, awaitItem())
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
}
