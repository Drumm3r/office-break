package com.drumm3r.officebreak.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.drumm3r.officebreak.data.Exercise
import com.drumm3r.officebreak.data.SettingsRepository
import com.drumm3r.officebreak.service.DefaultTimerServiceController
import com.drumm3r.officebreak.service.TimerServiceController
import com.drumm3r.officebreak.service.TimerState
import com.drumm3r.officebreak.service.TimerStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TimerViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repository: SettingsRepository = SettingsRepository(application),
    private val timerStateHolder: TimerStateHolder = TimerStateHolder.instance,
    private val serviceController: TimerServiceController = DefaultTimerServiceController(application),
) : AndroidViewModel(application) {

    companion object {
        const val MAX_EXERCISE_NAME_LENGTH = 100
        private const val TAG = "TimerViewModel"
        private const val KEY_CURRENT_EXERCISE = "current_exercise"
    }

    private val json = Json { ignoreUnknownKeys = true }

    val timerState: StateFlow<TimerState> = timerStateHolder.state

    val hours: StateFlow<Int> = repository.timerHours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_HOURS)

    val minutes: StateFlow<Int> = repository.timerMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_MINUTES)

    val reps: StateFlow<Int> = repository.reps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_REPS)

    val exercises: StateFlow<List<Exercise>> = repository.exercises
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val language: StateFlow<String> = repository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.LANGUAGE_SYSTEM)

    private val _currentExercise = MutableStateFlow(
        savedStateHandle.get<String>(KEY_CURRENT_EXERCISE)?.let {
            try {
                json.decodeFromString<Exercise>(it)
            } catch (_: Exception) {
                null
            }
        },
    )
    val currentExercise: StateFlow<Exercise?> = _currentExercise.asStateFlow()

    init {
        viewModelScope.launch {
            _currentExercise.collect { exercise ->
                savedStateHandle[KEY_CURRENT_EXERCISE] = exercise?.let { json.encodeToString(it) }
            }
        }
    }

    fun setHours(value: Int) {
        viewModelScope.launch {
            try {
                repository.setTimerHours(value.coerceIn(0, 23))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set hours", e)
            }
        }
    }

    fun setMinutes(value: Int) {
        viewModelScope.launch {
            try {
                repository.setTimerMinutes(value.coerceIn(0, 59))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set minutes", e)
            }
        }
    }

    fun setReps(value: Int) {
        viewModelScope.launch {
            try {
                repository.setReps(value.coerceIn(1, 100))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set reps", e)
            }
        }
    }

    fun setLanguage(value: String) {
        viewModelScope.launch {
            try {
                repository.setLanguage(value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set language", e)
            }
        }
    }

    fun toggleExercise(index: Int) {
        viewModelScope.launch {
            try {
                val current = exercises.value.toMutableList()
                if (index in current.indices) {
                    current[index] = current[index].copy(isEnabled = !current[index].isEnabled)
                    repository.setExercises(current)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle exercise", e)
            }
        }
    }

    fun addExercise(name: String) {
        val trimmed = name.trim().take(MAX_EXERCISE_NAME_LENGTH)
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            try {
                val current = exercises.value.toMutableList()
                current.add(Exercise(name = trimmed))
                repository.setExercises(current)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add exercise", e)
            }
        }
    }

    fun removeExercise(index: Int) {
        viewModelScope.launch {
            try {
                val current = exercises.value.toMutableList()
                if (current.size > 1 && index in current.indices) {
                    current.removeAt(index)
                    repository.setExercises(current)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove exercise", e)
            }
        }
    }

    fun startTimer() {
        val totalSeconds = (hours.value * 3600L) + (minutes.value * 60L)
        if (totalSeconds <= 0) return

        serviceController.startTimer(totalSeconds)
    }

    fun resetTimer() {
        _currentExercise.value = null
        serviceController.resetTimer()
    }

    fun onTimerExpired() {
        viewModelScope.launch {
            val allExercises = repository.exercises.first()
            val enabledExercises = allExercises.filter { it.isEnabled }
            if (enabledExercises.isNotEmpty()) {
                _currentExercise.value = enabledExercises.random()
            }
        }
    }

    fun onExerciseDone() {
        _currentExercise.value = null
        val totalSeconds = (hours.value * 3600L) + (minutes.value * 60L)
        if (totalSeconds <= 0) return

        serviceController.restartTimer(totalSeconds)
    }
}
