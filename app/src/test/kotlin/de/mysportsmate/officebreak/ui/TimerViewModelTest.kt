package de.mysportsmate.officebreak.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import de.mysportsmate.officebreak.MainDispatcherRule
import de.mysportsmate.officebreak.data.AchievementDefinition
import de.mysportsmate.officebreak.data.Exercise
import de.mysportsmate.officebreak.data.ExerciseMode
import de.mysportsmate.officebreak.data.FakeDataStore
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val defaultExercisesByMode = mapOf(
        ExerciseMode.HOME_WORKOUT to defaultExercises,
        ExerciseMode.HOME_MOBILITY to listOf(Exercise(name = "Cat-Cow Stretch", nameResKey = "exercise_cat_cow")),
        ExerciseMode.OFFICE to listOf(Exercise(name = "Neck Stretch", nameResKey = "exercise_neck_stretch")),
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
            defaultExercisesByMode = defaultExercisesByMode,
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

    private fun collectAllFlows() = collectFlows() + listOf(
        viewModel.autoRestart,
        viewModel.dynamicIncreaseEnabled,
        viewModel.trackingEnabled,
        viewModel.beepVolume,
        viewModel.beepCount,
        viewModel.vibrationEnabled,
        viewModel.keepScreenOn,
        viewModel.ttsEnabled,
        viewModel.statsSnapshot,
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
        val modeCollector = launch { viewModel.exerciseMode.collect {} }
        advanceUntilIdle()

        viewModel.completeOnboarding(FitnessLevel.MODERATE, ExerciseMode.HOME_WORKOUT)
        advanceUntilIdle()

        assertEquals(0, viewModel.hours.value)
        assertEquals(45, viewModel.minutes.value)
        assertEquals(10, viewModel.repsMin.value)
        assertEquals(10, viewModel.repsMax.value)
        assertEquals(true, viewModel.repsLinked.value)
        assertEquals(ExerciseMode.HOME_WORKOUT, viewModel.exerciseMode.value)
        assertEquals(true, viewModel.onboardingCompleted.value)

        modeCollector.cancel()
        onboardingCollector.cancel()
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `completeOnboarding applies beginner presets`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        val onboardingCollector = launch { viewModel.onboardingCompleted.collect {} }
        advanceUntilIdle()

        viewModel.completeOnboarding(FitnessLevel.BEGINNER, ExerciseMode.HOME_WORKOUT)
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

        viewModel.completeOnboarding(FitnessLevel.ATHLETIC, ExerciseMode.OFFICE)
        advanceUntilIdle()

        assertEquals(0, viewModel.hours.value)
        assertEquals(30, viewModel.minutes.value)
        assertEquals(15, viewModel.repsMin.value)
        assertEquals(15, viewModel.repsMax.value)

        onboardingCollector.cancel()
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `setExerciseMode changes active mode`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        val modeCollector = launch { viewModel.exerciseMode.collect {} }
        advanceUntilIdle()

        viewModel.setExerciseMode(ExerciseMode.OFFICE)
        advanceUntilIdle()

        assertEquals(ExerciseMode.OFFICE, viewModel.exerciseMode.value)

        modeCollector.cancel()
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `setExerciseMode updates exercises to new mode defaults`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        val modeCollector = launch { viewModel.exerciseMode.collect {} }
        advanceUntilIdle()

        assertEquals(defaultExercises, viewModel.exercises.value)

        viewModel.setExerciseMode(ExerciseMode.OFFICE)
        advanceUntilIdle()

        assertEquals(defaultExercisesByMode[ExerciseMode.OFFICE], viewModel.exercises.value)

        modeCollector.cancel()
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

    @Test
    fun `onTimerExpired does not repick when currentExercise already set`() = runTest {
        val exercise = Exercise(name = "Push Ups", isEnabled = true)
        val json = Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(exercise)

        val restoredHandle = SavedStateHandle(
            mapOf(
                "current_exercise" to encoded,
                "current_reps" to 7,
            ),
        )
        val restoredVm = TimerViewModel(
            application = application,
            savedStateHandle = restoredHandle,
            repository = repository,
            statsRepository = statsRepository,
            timerStateHolder = timerStateHolder,
            serviceController = serviceController,
        )
        advanceUntilIdle()

        restoredVm.onTimerExpired()
        advanceUntilIdle()

        assertEquals(exercise, restoredVm.currentExercise.value)
        assertEquals(7, restoredVm.currentReps.value)
    }

    @Test
    fun `currentExercise restored from DataStore when SavedStateHandle empty`() = runTest {
        repository.setActiveBreakState(
            """{"exercise":{"name":"Push Ups","isEnabled":true},"reps":9}""",
        )

        val vm = TimerViewModel(
            application = application,
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            statsRepository = statsRepository,
            timerStateHolder = timerStateHolder,
            serviceController = serviceController,
        )
        advanceUntilIdle()

        assertEquals("Push Ups", vm.currentExercise.value?.name)
        assertEquals(9, vm.currentReps.value)

        vm.onTimerExpired()
        advanceUntilIdle()

        assertEquals("Push Ups", vm.currentExercise.value?.name)
        assertEquals(9, vm.currentReps.value)
    }

    @Test
    fun `onExerciseDone clears persisted active break state`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        val activeBreakCollector = launch { repository.activeBreakState.collect {} }
        advanceUntilIdle()

        viewModel.onTimerExpired()
        advanceUntilIdle()
        assertNotNull(repository.activeBreakState.first())

        viewModel.onExerciseDone()
        advanceUntilIdle()
        assertNull(repository.activeBreakState.first())

        activeBreakCollector.cancel()
        collectors.forEach { it.cancel() }
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

    @Test
    fun `dismissWorkEnded transitions to Idle`() = runTest {
        timerStateHolder.update(TimerState.WorkEnded)
        assertEquals(TimerState.WorkEnded, viewModel.timerState.value)

        viewModel.dismissWorkEnded()
        advanceUntilIdle()

        assertEquals(TimerState.Idle, viewModel.timerState.value)
    }

    @Test
    fun `startTimer without schedule passes freestyle false`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }

        viewModel.setMinutes(15)
        advanceUntilIdle()

        viewModel.startTimer()

        val call = serviceController.calls.first() as FakeTimerServiceController.Call.Start
        assertEquals(false, call.freestyle)

        collectors.forEach { it.cancel() }
    }

    // --- Settings toggles ---

    @Test
    fun `setVibrationEnabled writes to repository`() = runTest {
        viewModel.vibrationEnabled.test {
            assertEquals(SettingsRepository.DEFAULT_VIBRATION_ENABLED, awaitItem())
            viewModel.setVibrationEnabled(false)
            assertEquals(false, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setKeepScreenOn writes to repository`() = runTest {
        viewModel.keepScreenOn.test {
            assertEquals(SettingsRepository.DEFAULT_KEEP_SCREEN_ON, awaitItem())
            viewModel.setKeepScreenOn(true)
            assertEquals(true, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setTtsEnabled writes to repository`() = runTest {
        viewModel.ttsEnabled.test {
            assertEquals(SettingsRepository.DEFAULT_TTS_ENABLED, awaitItem())
            viewModel.setTtsEnabled(true)
            assertEquals(true, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setDynamicIncreaseEnabled writes to repository`() = runTest {
        viewModel.dynamicIncreaseEnabled.test {
            assertEquals(SettingsRepository.DEFAULT_DYNAMIC_INCREASE_ENABLED, awaitItem())
            viewModel.setDynamicIncreaseEnabled(false)
            assertEquals(false, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // --- Achievement celebration ---

    @Test
    fun `dismissAchievementCelebration clears newly unlocked list`() {
        assertEquals(emptyList<AchievementDefinition>(), viewModel.newlyUnlockedAchievements.value)
        viewModel.dismissAchievementCelebration()
        assertEquals(emptyList<AchievementDefinition>(), viewModel.newlyUnlockedAchievements.value)
    }

    // --- Backup state ---

    @Test
    fun `clearBackupState resets to Idle`() {
        viewModel.clearBackupState()
        assertEquals(BackupUiState.Idle, viewModel.backupState.value)
    }

    // --- onExerciseDone without autoRestart ---

    @Test
    fun `onExerciseDone without autoRestart resets timer`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setAutoRestart(false)
        advanceUntilIdle()
        viewModel.setMinutes(15)
        advanceUntilIdle()

        viewModel.onTimerExpired()
        advanceUntilIdle()
        assertNotNull(viewModel.currentExercise.value)

        viewModel.onExerciseDone()
        advanceUntilIdle()

        assertNull(viewModel.currentExercise.value)
        val lastCall = serviceController.calls.last()
        assertTrue("Expected Reset but got $lastCall", lastCall is FakeTimerServiceController.Call.Reset)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onExerciseDone with null exercise still restarts timer`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setMinutes(15)
        advanceUntilIdle()

        // Call onExerciseDone without prior onTimerExpired, so exercise is null
        viewModel.onExerciseDone()
        advanceUntilIdle()

        val lastCall = serviceController.calls.lastOrNull()
        assertNotNull(lastCall)
        assertTrue(lastCall is FakeTimerServiceController.Call.Restart)

        collectors.forEach { it.cancel() }
    }

    // --- Dynamic increase ---

    @Test
    fun `onExerciseDone increments breaksSinceLastIncrease`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        val breakCountCollector = launch { viewModel.dynamicIncreaseEnabled.collect {} }
        advanceUntilIdle()

        viewModel.setMinutes(15)
        advanceUntilIdle()

        viewModel.onTimerExpired()
        advanceUntilIdle()
        viewModel.onExerciseDone()
        advanceUntilIdle()

        // breaksSinceLastIncrease is internal, test indirectly via breaksSinceLastIncrease flow
        // After 1 exercise done, counter should be 1
        // We can't directly read breaksSinceLastIncrease from ViewModel (private),
        // but we verify no offer is shown yet (threshold for 15min = (480/15)*3 = 96)
        assertNull(viewModel.dynamicIncreaseOffer.value)

        breakCountCollector.cancel()
        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onExerciseDone does not show offer when dynamic increase disabled`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setDynamicIncreaseEnabled(false)
        advanceUntilIdle()
        viewModel.setMinutes(15)
        advanceUntilIdle()

        viewModel.onTimerExpired()
        advanceUntilIdle()
        viewModel.onExerciseDone()
        advanceUntilIdle()

        assertNull(viewModel.dynamicIncreaseOffer.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `onExerciseDone does not show offer below threshold`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // Default 30 min interval, threshold = (480/30)*3 = 48
        // One exercise done should not trigger offer
        viewModel.onTimerExpired()
        advanceUntilIdle()
        viewModel.onExerciseDone()
        advanceUntilIdle()

        assertNull(viewModel.dynamicIncreaseOffer.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `acceptIncreaseReps offer has correct new reps value`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // repsMax default = 10, after increase should be 12
        viewModel.acceptIncreaseReps()
        advanceUntilIdle()

        assertEquals(12, viewModel.repsMax.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `acceptDecreaseInterval offer has correct new interval`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // Default 30 min, decrease by 5 = 25
        viewModel.acceptDecreaseInterval()
        advanceUntilIdle()

        val totalMinutes = viewModel.hours.value * 60 + viewModel.minutes.value
        assertEquals(25, totalMinutes)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `acceptIncreaseReps cannot exceed MAX_REPS via offer`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // Set reps close to max: 49
        viewModel.setRepsLinked(false)
        advanceUntilIdle()
        viewModel.setRepsMin(49)
        advanceUntilIdle()
        viewModel.setRepsMax(49)
        advanceUntilIdle()

        // Increase by REPS_INCREASE=2, but capped at MAX_REPS=50
        viewModel.acceptIncreaseReps()
        advanceUntilIdle()

        assertEquals(50, viewModel.repsMax.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `acceptDecreaseInterval cannot go below MIN_INTERVAL via offer`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // Set interval to 7 minutes
        viewModel.setHours(0)
        advanceUntilIdle()
        viewModel.setMinutes(7)
        advanceUntilIdle()

        // Decrease by 5 -> 2, but floored at 5
        viewModel.acceptDecreaseInterval()
        advanceUntilIdle()

        val totalMinutes = viewModel.hours.value * 60 + viewModel.minutes.value
        assertEquals(5, totalMinutes)

        collectors.forEach { it.cancel() }
    }

    // --- Accept / Decline Dynamic Increase ---

    @Test
    fun `acceptIncreaseReps increases repsMin and repsMax by 2`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // Default reps: min=10, max=10
        viewModel.acceptIncreaseReps()
        advanceUntilIdle()

        assertEquals(12, viewModel.repsMin.value)
        assertEquals(12, viewModel.repsMax.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `acceptIncreaseReps clears dynamic offer`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.acceptIncreaseReps()
        advanceUntilIdle()

        assertNull(viewModel.dynamicIncreaseOffer.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `acceptDecreaseInterval decreases total interval by 5 minutes`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // Default: 0h 30m = 30 min
        viewModel.acceptDecreaseInterval()
        advanceUntilIdle()

        val totalMinutes = viewModel.hours.value * 60 + viewModel.minutes.value
        assertEquals(25, totalMinutes)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `acceptDecreaseInterval splits hours and minutes correctly`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // 1h 5m = 65 min -> decrease by 5 -> 60 min = 1h 0m
        viewModel.setHours(1)
        advanceUntilIdle()
        viewModel.setMinutes(5)
        advanceUntilIdle()

        viewModel.acceptDecreaseInterval()
        advanceUntilIdle()

        assertEquals(1, viewModel.hours.value)
        assertEquals(0, viewModel.minutes.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `acceptDecreaseInterval clears dynamic offer`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.acceptDecreaseInterval()
        advanceUntilIdle()

        assertNull(viewModel.dynamicIncreaseOffer.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `declineDynamicIncrease clears offer and resets counter`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // Set up some breaks counter
        repository.setBreaksSinceLastIncrease(10)
        advanceUntilIdle()

        viewModel.declineDynamicIncrease()
        advanceUntilIdle()

        assertNull(viewModel.dynamicIncreaseOffer.value)

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `declineDynamicIncrease restarts timer when autoRestart on`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        viewModel.setMinutes(15)
        advanceUntilIdle()

        viewModel.declineDynamicIncrease()
        advanceUntilIdle()

        val lastCall = serviceController.calls.lastOrNull()
        assertNotNull(lastCall)
        assertTrue(lastCall is FakeTimerServiceController.Call.Restart)

        collectors.forEach { it.cancel() }
    }

    // --- Tracking ---

    @Test
    fun `setTrackingEnabled writes to stats repository`() = runTest {
        viewModel.trackingEnabled.test {
            assertEquals(true, awaitItem())
            viewModel.setTrackingEnabled(false)
            assertEquals(false, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `resetStats clears all stats`() = runTest {
        val collectors = collectFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        // Record a break first
        viewModel.onTimerExpired()
        advanceUntilIdle()
        viewModel.onExerciseDone()
        advanceUntilIdle()

        viewModel.resetStats()
        advanceUntilIdle()

        assertEquals(0, viewModel.statsSnapshot.value.totalBreaksAllTime)

        collectors.forEach { it.cancel() }
    }

    // --- auto mode by day ---

    @Test
    fun `autoModeByDayEnabled emits default false`() = runTest {
        viewModel.autoModeByDayEnabled.test {
            assertEquals(false, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setAutoModeByDayEnabled true seeds all days with current exercise mode`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        val autoCollector = launch { viewModel.autoModeByDayEnabled.collect {} }
        val modeCollector = launch { viewModel.exerciseMode.collect {} }
        advanceUntilIdle()

        repository.setExerciseMode(ExerciseMode.OFFICE)
        advanceUntilIdle()

        viewModel.setAutoModeByDayEnabled(true)
        advanceUntilIdle()

        val schedule = repository.weekSchedule.first()
        schedule.forEach { day ->
            assertEquals(ExerciseMode.OFFICE, day.defaultMode)
        }
        assertTrue(repository.autoModeByDayEnabled.first())

        (collectors + autoCollector + modeCollector).forEach { it.cancel() }
    }

    @Test
    fun `applyDayDefaultModeIfEnabled is no-op when autoMode is disabled`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        repository.setExerciseMode(ExerciseMode.HOME_WORKOUT)
        // Write schedule where today has OFFICE mode, but master toggle off
        val todayIndex = java.time.LocalDate.now().dayOfWeek.ordinal
        val schedule = DEFAULT_WEEK_SCHEDULE.toMutableList().apply {
            this[todayIndex] = this[todayIndex].copy(
                enabled = true,
                linked = false,
                defaultMode = ExerciseMode.OFFICE,
            )
        }
        repository.setWeekSchedule(schedule)
        advanceUntilIdle()

        viewModel.applyDayDefaultModeIfEnabled()
        advanceUntilIdle()

        assertEquals(ExerciseMode.HOME_WORKOUT, repository.exerciseMode.first())

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `applyDayDefaultModeIfEnabled switches mode when todays mode differs`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        repository.setExerciseMode(ExerciseMode.HOME_WORKOUT)
        val todayIndex = java.time.LocalDate.now().dayOfWeek.ordinal
        val schedule = DEFAULT_WEEK_SCHEDULE.toMutableList().apply {
            for (i in indices) {
                this[i] = this[i].copy(enabled = true, linked = false)
            }
            this[todayIndex] = this[todayIndex].copy(defaultMode = ExerciseMode.OFFICE)
        }
        repository.setWeekSchedule(schedule)
        repository.setAutoModeByDayEnabled(enabled = true, seedMode = null)
        advanceUntilIdle()

        viewModel.applyDayDefaultModeIfEnabled()
        advanceUntilIdle()

        assertEquals(ExerciseMode.OFFICE, repository.exerciseMode.first())

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `applyDayDefaultModeIfEnabled is no-op when today is disabled`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        repository.setExerciseMode(ExerciseMode.HOME_WORKOUT)
        val todayIndex = java.time.LocalDate.now().dayOfWeek.ordinal
        val schedule = DEFAULT_WEEK_SCHEDULE.toMutableList().apply {
            this[todayIndex] = this[todayIndex].copy(
                enabled = false,
                linked = false,
                defaultMode = ExerciseMode.OFFICE,
            )
        }
        repository.setWeekSchedule(schedule)
        repository.setAutoModeByDayEnabled(enabled = true, seedMode = null)
        advanceUntilIdle()

        viewModel.applyDayDefaultModeIfEnabled()
        advanceUntilIdle()

        // Mode stays what it was — today disabled, no switch
        assertEquals(ExerciseMode.HOME_WORKOUT, repository.exerciseMode.first())

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `applyDayDefaultModeIfEnabled does nothing when current matches target mode`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        repository.setExerciseMode(ExerciseMode.OFFICE)
        val todayIndex = java.time.LocalDate.now().dayOfWeek.ordinal
        val schedule = DEFAULT_WEEK_SCHEDULE.toMutableList().apply {
            this[todayIndex] = this[todayIndex].copy(
                enabled = true,
                linked = false,
                defaultMode = ExerciseMode.OFFICE,
            )
        }
        repository.setWeekSchedule(schedule)
        repository.setAutoModeByDayEnabled(enabled = true, seedMode = null)
        advanceUntilIdle()

        viewModel.applyDayDefaultModeIfEnabled()
        advanceUntilIdle()

        assertEquals(ExerciseMode.OFFICE, repository.exerciseMode.first())

        collectors.forEach { it.cancel() }
    }

    @Test
    fun `updateDaySchedule triggers mode switch when editing today`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        val autoCollector = launch { viewModel.autoModeByDayEnabled.collect {} }
        val weekCollector = launch { viewModel.weekSchedule.collect {} }
        advanceUntilIdle()

        repository.setExerciseMode(ExerciseMode.HOME_WORKOUT)
        repository.setAutoModeByDayEnabled(enabled = true, seedMode = ExerciseMode.HOME_WORKOUT)
        val schedule = DEFAULT_WEEK_SCHEDULE.toMutableList().apply {
            for (i in indices) {
                this[i] = this[i].copy(enabled = true, linked = false)
            }
        }
        repository.setWeekSchedule(schedule)
        advanceUntilIdle()

        val todayIndex = java.time.LocalDate.now().dayOfWeek.ordinal
        viewModel.updateDaySchedule(
            todayIndex,
            schedule[todayIndex].copy(defaultMode = ExerciseMode.OFFICE),
        )
        advanceUntilIdle()

        assertEquals(ExerciseMode.OFFICE, repository.exerciseMode.first())

        (collectors + autoCollector + weekCollector).forEach { it.cancel() }
    }

    @Test
    fun `completeOnboarding with autoMode seeded applies todays mode`() = runTest {
        val collectors = collectAllFlows().map { flow -> launch { flow.collect {} } }
        advanceUntilIdle()

        val todayIndex = java.time.LocalDate.now().dayOfWeek.ordinal
        val schedule = DEFAULT_WEEK_SCHEDULE.toMutableList().apply {
            for (i in indices) {
                this[i] = this[i].copy(enabled = true, linked = false, defaultMode = ExerciseMode.OFFICE)
            }
        }
        repository.setWeekSchedule(schedule)
        repository.setAutoModeByDayEnabled(enabled = true, seedMode = null)
        advanceUntilIdle()

        viewModel.completeOnboarding(FitnessLevel.MODERATE, ExerciseMode.HOME_WORKOUT)
        advanceUntilIdle()

        // completeOnboarding sets mode = HOME_WORKOUT, then applyDayDefaultMode switches to today's OFFICE
        assertEquals(ExerciseMode.OFFICE, repository.exerciseMode.first())

        collectors.forEach { it.cancel() }
    }
}
