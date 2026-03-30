package de.mysportsmate.officebreak.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import de.mysportsmate.officebreak.MainDispatcherRule
import de.mysportsmate.officebreak.data.Exercise
import de.mysportsmate.officebreak.data.FakeDataStore
import de.mysportsmate.officebreak.data.FitnessLevel
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.StatsRepository
import de.mysportsmate.officebreak.service.TimerState
import de.mysportsmate.officebreak.service.TimerStateHolder
import de.mysportsmate.officebreak.widget.WidgetUpdater
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var application: Application
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var dataStore: FakeDataStore
    private lateinit var statsDataStore: FakeDataStore
    private lateinit var repository: SettingsRepository
    private lateinit var statsRepository: StatsRepository
    private lateinit var timerStateHolder: TimerStateHolder
    private lateinit var serviceController: FakeTimerServiceController
    private lateinit var viewModel: TimerViewModel

    private val defaultExercises = listOf(
        Exercise(name = "Push Ups", nameResKey = "exercise_push_ups"),
        Exercise(name = "Squats", nameResKey = "exercise_squats"),
    )

    @Before
    fun setUp() {
        mockkObject(WidgetUpdater)
        coEvery { WidgetUpdater.requestUpdate(any()) } returns Unit
        application = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        dataStore = FakeDataStore()
        statsDataStore = FakeDataStore()
        repository = SettingsRepository(
            dataStore = dataStore,
            defaultExercises = defaultExercises,
        )
        statsRepository = StatsRepository(dataStore = statsDataStore)
        timerStateHolder = TimerStateHolder()
        serviceController = FakeTimerServiceController()
        viewModel = TimerViewModel(
            application = application,
            savedStateHandle = savedStateHandle,
            repository = repository,
            statsRepository = statsRepository,
            timerStateHolder = timerStateHolder,
            serviceController = serviceController,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(WidgetUpdater)
    }

    private fun collectFlows() = listOf(
        viewModel.hours,
        viewModel.minutes,
        viewModel.repsMin,
        viewModel.repsMax,
        viewModel.repsLinked,
        viewModel.exercises,
        viewModel.language,
    )

    @Test
    fun `timerState reflects holder state`() {
        assertEquals(TimerState.Idle, viewModel.timerState.value)

        timerStateHolder.update(TimerState.Running(remainingSeconds = 10, totalSeconds = 60))
        assertEquals(TimerState.Running(remainingSeconds = 10, totalSeconds = 60), viewModel.timerState.value)
    }

    @Test
    fun `hours emits default from repository`() = runTest {
        viewModel.hours.test {
            assertEquals(SettingsRepository.DEFAULT_HOURS, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `minutes emits default from repository`() = runTest {
        viewModel.minutes.test {
            assertEquals(SettingsRepository.DEFAULT_MINUTES, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `repsMin emits default from repository`() = runTest {
        viewModel.repsMin.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MIN, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `repsMax emits default from repository`() = runTest {
        viewModel.repsMax.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MAX, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `repsLinked emits default from repository`() = runTest {
        viewModel.repsLinked.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_LINKED, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setHours writes to repository`() = runTest {
        viewModel.hours.test {
            assertEquals(SettingsRepository.DEFAULT_HOURS, awaitItem())

            viewModel.setHours(2)
            assertEquals(2, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setMinutes writes to repository`() = runTest {
        viewModel.minutes.test {
            assertEquals(SettingsRepository.DEFAULT_MINUTES, awaitItem())

            viewModel.setMinutes(45)
            assertEquals(45, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setRepsMin writes to repository`() = runTest {
        viewModel.repsMin.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MIN, awaitItem())

            viewModel.setRepsMin(20)
            assertEquals(20, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setRepsMin when linked also updates repsMax`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setRepsMin(25)
        advanceUntilIdle()

        assertEquals(25, viewModel.repsMin.value)
        assertEquals(25, viewModel.repsMax.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `setRepsMax writes to repository`() = runTest {
        viewModel.repsMax.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MAX, awaitItem())

            viewModel.setRepsMax(30)
            assertEquals(30, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setRepsLinked snaps max to min when linking`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setRepsLinked(false)
        advanceUntilIdle()
        viewModel.setRepsMin(15)
        advanceUntilIdle()
        viewModel.setRepsMax(30)
        advanceUntilIdle()

        viewModel.setRepsLinked(true)
        advanceUntilIdle()

        assertEquals(15, viewModel.repsMax.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `setRepsMax cannot go below repsMin`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setRepsLinked(false)
        advanceUntilIdle()
        viewModel.setRepsMin(20)
        advanceUntilIdle()

        viewModel.setRepsMax(5)
        advanceUntilIdle()

        assertEquals(20, viewModel.repsMax.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `setRepsMin raises repsMax when min exceeds max`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setRepsLinked(false)
        advanceUntilIdle()
        viewModel.setRepsMin(10)
        advanceUntilIdle()
        viewModel.setRepsMax(15)
        advanceUntilIdle()

        viewModel.setRepsMin(25)
        advanceUntilIdle()

        assertEquals(25, viewModel.repsMin.value)
        assertEquals(25, viewModel.repsMax.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `setLanguage writes to repository`() = runTest {
        viewModel.language.test {
            assertEquals(SettingsRepository.LANGUAGE_SYSTEM, awaitItem())

            viewModel.setLanguage(SettingsRepository.LANGUAGE_DE)
            assertEquals(SettingsRepository.LANGUAGE_DE, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startTimer calls service controller with correct duration`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }

        viewModel.setHours(1)
        viewModel.setMinutes(30)
        advanceUntilIdle()

        viewModel.startTimer()

        assertEquals(1, serviceController.calls.size)
        val call = serviceController.calls.first()
        assertTrue(call is FakeTimerServiceController.Call.Start)
        assertEquals(5400L, (call as FakeTimerServiceController.Call.Start).durationSeconds)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `startTimer does nothing when total seconds is zero`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }

        viewModel.setHours(0)
        viewModel.setMinutes(0)
        advanceUntilIdle()

        viewModel.startTimer()

        assertTrue(serviceController.calls.isEmpty())

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `resetTimer clears exercise and calls service controller`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.onTimerExpired()
        assertNotNull(viewModel.currentExercise.value)

        viewModel.resetTimer()

        assertNull(viewModel.currentExercise.value)
        assertEquals(1, serviceController.calls.size)
        assertTrue(serviceController.calls.first() is FakeTimerServiceController.Call.Reset)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onTimerExpired picks enabled exercise`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.onTimerExpired()

        val exercise = viewModel.currentExercise.value
        assertNotNull(exercise)
        assertTrue(exercise!! in defaultExercises)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onTimerExpired cycles through all exercises before repeating`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        val pickedNames = mutableListOf<String>()

        viewModel.onTimerExpired()
        pickedNames.add(viewModel.currentExercise.value!!.name)
        viewModel.onExerciseDone()
        advanceUntilIdle()

        viewModel.onTimerExpired()
        pickedNames.add(viewModel.currentExercise.value!!.name)

        assertEquals(2, pickedNames.toSet().size)
        assertTrue(pickedNames.all { name -> defaultExercises.any { it.name == name } })

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onTimerExpired resets shuffle bag when all exercises used`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.onTimerExpired()
        viewModel.onExerciseDone()
        advanceUntilIdle()

        viewModel.onTimerExpired()
        viewModel.onExerciseDone()
        advanceUntilIdle()

        viewModel.onTimerExpired()
        val thirdExercise = viewModel.currentExercise.value
        assertNotNull(thirdExercise)
        assertTrue(thirdExercise!! in defaultExercises)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `resetTimer clears shuffle bag`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.onTimerExpired()
        val firstPick = viewModel.currentExercise.value!!.name

        viewModel.resetTimer()
        advanceUntilIdle()

        val pickedAfterReset = mutableSetOf<String>()
        repeat(defaultExercises.size) {
            viewModel.onTimerExpired()
            pickedAfterReset.add(viewModel.currentExercise.value!!.name)
            viewModel.onExerciseDone()
            advanceUntilIdle()
        }

        assertEquals(defaultExercises.size, pickedAfterReset.size)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onTimerExpired produces reps within min max range`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setRepsLinked(false)
        advanceUntilIdle()
        viewModel.setRepsMin(5)
        advanceUntilIdle()
        viewModel.setRepsMax(15)
        advanceUntilIdle()

        viewModel.onTimerExpired()

        val reps = viewModel.currentReps.value
        assertNotNull(reps)
        assertTrue("reps $reps should be in 5..15", reps!! in 5..15)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `currentReps cleared on exerciseDone`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.onTimerExpired()
        assertNotNull(viewModel.currentReps.value)

        viewModel.onExerciseDone()
        assertNull(viewModel.currentReps.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `currentReps cleared on resetTimer`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.onTimerExpired()
        assertNotNull(viewModel.currentReps.value)

        viewModel.resetTimer()
        assertNull(viewModel.currentReps.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onTimerExpired does nothing when no exercises enabled`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }

        repository.setExercises(defaultExercises.map { it.copy(isEnabled = false) })
        advanceUntilIdle()

        viewModel.onTimerExpired()

        assertNull(viewModel.currentExercise.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onExerciseDone clears exercise and restarts timer`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }

        viewModel.setMinutes(15)
        advanceUntilIdle()

        viewModel.onTimerExpired()
        advanceUntilIdle()
        assertNotNull(viewModel.currentExercise.value)

        viewModel.onExerciseDone()
        advanceUntilIdle()

        assertNull(viewModel.currentExercise.value)
        val restartCall = serviceController.calls.last()
        assertTrue(restartCall is FakeTimerServiceController.Call.Restart)
        assertEquals(900L, (restartCall as FakeTimerServiceController.Call.Restart).durationSeconds)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `toggleExercise flips isEnabled`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.toggleExercise(0)
        advanceUntilIdle()

        assertEquals(false, viewModel.exercises.value[0].isEnabled)
        assertEquals(true, viewModel.exercises.value[1].isEnabled)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `addExercise appends to list`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.addExercise("Plank")
        advanceUntilIdle()

        val exercises = viewModel.exercises.value
        assertEquals(3, exercises.size)
        assertEquals("Plank", exercises.last().name)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `addExercise ignores blank name`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.addExercise("   ")
        advanceUntilIdle()

        assertEquals(2, viewModel.exercises.value.size)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `removeExercise removes from list`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.removeExercise(0)
        advanceUntilIdle()

        val exercises = viewModel.exercises.value
        assertEquals(1, exercises.size)
        assertEquals("Squats", exercises[0].name)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `removeExercise guards last exercise`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }

        repository.setExercises(listOf(Exercise(name = "Only One")))
        advanceUntilIdle()

        viewModel.removeExercise(0)
        advanceUntilIdle()

        assertEquals(1, viewModel.exercises.value.size)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onboardingCompleted emits null then false for fresh install`() = runTest {
        viewModel.onboardingCompleted.test {
            val first = awaitItem()
            if (first == null) {
                assertEquals(false, awaitItem())
            } else {
                assertEquals(false, first)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `completeOnboarding applies fitness level presets`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        val onboardingCollector = launch { viewModel.onboardingCompleted.collect {} }
        advanceUntilIdle()

        val selectedExercises = listOf(
            Exercise(name = "Push Ups", isEnabled = true, nameResKey = "exercise_push_ups"),
            Exercise(name = "Squats", isEnabled = false, nameResKey = "exercise_squats"),
        )

        viewModel.completeOnboarding(FitnessLevel.MODERATE, selectedExercises)
        advanceUntilIdle()

        assertEquals(0, viewModel.hours.value)
        assertEquals(45, viewModel.minutes.value)
        assertEquals(10, viewModel.repsMin.value)
        assertEquals(10, viewModel.repsMax.value)
        assertEquals(true, viewModel.repsLinked.value)
        assertEquals(selectedExercises, viewModel.exercises.value)
        assertEquals(true, viewModel.onboardingCompleted.value)

        onboardingCollector.cancel()
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `completeOnboarding applies beginner presets`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        val onboardingCollector = launch { viewModel.onboardingCompleted.collect {} }
        advanceUntilIdle()

        viewModel.completeOnboarding(FitnessLevel.BEGINNER, defaultExercises)
        advanceUntilIdle()

        assertEquals(1, viewModel.hours.value)
        assertEquals(0, viewModel.minutes.value)
        assertEquals(5, viewModel.repsMin.value)
        assertEquals(5, viewModel.repsMax.value)

        onboardingCollector.cancel()
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `completeOnboarding applies athletic presets`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        val onboardingCollector = launch { viewModel.onboardingCompleted.collect {} }
        advanceUntilIdle()

        viewModel.completeOnboarding(FitnessLevel.ATHLETIC, defaultExercises)
        advanceUntilIdle()

        assertEquals(0, viewModel.hours.value)
        assertEquals(30, viewModel.minutes.value)
        assertEquals(15, viewModel.repsMin.value)
        assertEquals(15, viewModel.repsMax.value)

        onboardingCollector.cancel()
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `currentExercise survives SavedStateHandle roundtrip`() {
        val exercise = Exercise(name = "Push Ups", isEnabled = true)
        val json = Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(exercise)

        val restoredHandle = SavedStateHandle(mapOf("current_exercise" to encoded))
        val restoredVm = TimerViewModel(
            application = application,
            savedStateHandle = restoredHandle,
            repository = repository,
            statsRepository = statsRepository,
            timerStateHolder = timerStateHolder,
            serviceController = serviceController,
        )

        assertEquals(exercise, restoredVm.currentExercise.value)
    }

    // --- Input coercion / clamping tests ---

    @Test
    fun `setHours clamps negative to 0`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // Default hours is 0, so first set to non-zero to verify clamping
        viewModel.setHours(5)
        advanceUntilIdle()
        assertEquals(5, viewModel.hours.value)

        viewModel.setHours(-1)
        advanceUntilIdle()
        assertEquals(0, viewModel.hours.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `setHours clamps above 23 to 23`() = runTest {
        viewModel.hours.test {
            assertEquals(SettingsRepository.DEFAULT_HOURS, awaitItem())
            viewModel.setHours(100)
            assertEquals(23, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setMinutes clamps negative to 0`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setMinutes(15)
        advanceUntilIdle()
        assertEquals(15, viewModel.minutes.value)

        viewModel.setMinutes(-5)
        advanceUntilIdle()
        assertEquals(0, viewModel.minutes.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `setMinutes clamps above 59 to 59`() = runTest {
        viewModel.minutes.test {
            assertEquals(SettingsRepository.DEFAULT_MINUTES, awaitItem())
            viewModel.setMinutes(999)
            assertEquals(59, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setRepsMin clamps zero to 1`() = runTest {
        viewModel.repsMin.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MIN, awaitItem())
            viewModel.setRepsMin(0)
            assertEquals(1, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setRepsMin clamps above 50 to 50`() = runTest {
        viewModel.repsMin.test {
            assertEquals(SettingsRepository.DEFAULT_REPS_MIN, awaitItem())
            viewModel.setRepsMin(100)
            assertEquals(50, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setRepsMax clamps to repsMin when set below`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setRepsLinked(false)
        advanceUntilIdle()
        viewModel.setRepsMin(15)
        advanceUntilIdle()

        viewModel.setRepsMax(3)
        advanceUntilIdle()

        assertEquals(15, viewModel.repsMax.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `setBeepVolume clamps negative to 0`() = runTest {
        viewModel.beepVolume.test {
            assertEquals(SettingsRepository.DEFAULT_BEEP_VOLUME, awaitItem())
            viewModel.setBeepVolume(-10)
            assertEquals(0, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setBeepVolume clamps above 100 to 100`() = runTest {
        viewModel.beepVolume.test {
            assertEquals(SettingsRepository.DEFAULT_BEEP_VOLUME, awaitItem())
            viewModel.setBeepVolume(200)
            assertEquals(100, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setBeepCount clamps zero to 1`() = runTest {
        viewModel.beepCount.test {
            assertEquals(SettingsRepository.DEFAULT_BEEP_COUNT, awaitItem())
            viewModel.setBeepCount(0)
            assertEquals(1, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setBeepCount clamps above 5 to 5`() = runTest {
        viewModel.beepCount.test {
            assertEquals(SettingsRepository.DEFAULT_BEEP_COUNT, awaitItem())
            viewModel.setBeepCount(99)
            assertEquals(5, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Rejected / no-op operations ---

    @Test
    fun `addExercise rejects empty name`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.addExercise("")
        advanceUntilIdle()

        assertEquals(2, viewModel.exercises.value.size)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `addExercise truncates name longer than 100 chars`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        val longName = "A".repeat(150)
        viewModel.addExercise(longName)
        advanceUntilIdle()

        val added = viewModel.exercises.value.last()
        assertEquals(100, added.name.length)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `toggleExercise ignores negative index`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        val before = viewModel.exercises.value.toList()
        viewModel.toggleExercise(-1)
        advanceUntilIdle()

        assertEquals(before, viewModel.exercises.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `toggleExercise ignores out of bounds index`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        val before = viewModel.exercises.value.toList()
        viewModel.toggleExercise(999)
        advanceUntilIdle()

        assertEquals(before, viewModel.exercises.value)

        collectors.forEach { it.cancel() }
    }

    // --- Settings persistence ---

    @Test
    fun `setThemeMode writes to repository`() = runTest {
        viewModel.themeMode.test {
            assertEquals(SettingsRepository.THEME_SYSTEM, awaitItem())
            viewModel.setThemeMode("dark")
            assertEquals("dark", awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setAutoRestart writes to repository`() = runTest {
        viewModel.autoRestart.test {
            assertEquals(SettingsRepository.DEFAULT_AUTO_RESTART, awaitItem())
            viewModel.setAutoRestart(false)
            assertEquals(false, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setBeepVolume writes valid value to repository`() = runTest {
        viewModel.beepVolume.test {
            assertEquals(SettingsRepository.DEFAULT_BEEP_VOLUME, awaitItem())
            viewModel.setBeepVolume(50)
            assertEquals(50, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setBeepCount writes valid value to repository`() = runTest {
        viewModel.beepCount.test {
            assertEquals(SettingsRepository.DEFAULT_BEEP_COUNT, awaitItem())
            viewModel.setBeepCount(4)
            assertEquals(4, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Dynamic difficulty boundaries ---

    @Test
    fun `acceptIncreaseReps caps at MAX_REPS`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setRepsLinked(false)
        advanceUntilIdle()
        viewModel.setRepsMin(49)
        advanceUntilIdle()
        viewModel.setRepsMax(49)
        advanceUntilIdle()

        viewModel.acceptIncreaseReps()
        advanceUntilIdle()

        assertEquals(50, viewModel.repsMax.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `acceptDecreaseInterval floors at MIN_INTERVAL_MINUTES`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setHours(0)
        advanceUntilIdle()
        viewModel.setMinutes(7)
        advanceUntilIdle()

        viewModel.acceptDecreaseInterval()
        advanceUntilIdle()

        val totalMinutes = viewModel.hours.value * 60 + viewModel.minutes.value
        assertEquals(5, totalMinutes)

        collectors.forEach { it.cancel() }
    }
}
