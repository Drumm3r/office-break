package com.drumm3r.officebreak.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.drumm3r.officebreak.MainDispatcherRule
import com.drumm3r.officebreak.data.Exercise
import com.drumm3r.officebreak.data.FakeDataStore
import com.drumm3r.officebreak.data.SettingsRepository
import com.drumm3r.officebreak.service.TimerState
import com.drumm3r.officebreak.service.TimerStateHolder
import io.mockk.mockk
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
    private lateinit var repository: SettingsRepository
    private lateinit var timerStateHolder: TimerStateHolder
    private lateinit var serviceController: FakeTimerServiceController
    private lateinit var viewModel: TimerViewModel

    private val defaultExercises = listOf(
        Exercise(name = "Push Ups"),
        Exercise(name = "Squats"),
    )

    @Before
    fun setUp() {
        application = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        dataStore = FakeDataStore()
        repository = SettingsRepository(
            dataStore = dataStore,
            defaultExercises = defaultExercises,
        )
        timerStateHolder = TimerStateHolder()
        serviceController = FakeTimerServiceController()
        viewModel = TimerViewModel(
            application = application,
            savedStateHandle = savedStateHandle,
            repository = repository,
            timerStateHolder = timerStateHolder,
            serviceController = serviceController,
        )
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
        assertNotNull(viewModel.currentExercise.value)

        viewModel.onExerciseDone()

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
    fun `currentExercise survives SavedStateHandle roundtrip`() {
        val exercise = Exercise(name = "Push Ups", isEnabled = true)
        val json = Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(exercise)

        val restoredHandle = SavedStateHandle(mapOf("current_exercise" to encoded))
        val restoredVm = TimerViewModel(
            application = application,
            savedStateHandle = restoredHandle,
            repository = repository,
            timerStateHolder = timerStateHolder,
            serviceController = serviceController,
        )

        assertEquals(exercise, restoredVm.currentExercise.value)
    }
}
