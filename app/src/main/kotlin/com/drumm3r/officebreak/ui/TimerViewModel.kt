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
        private const val KEY_CURRENT_REPS = "current_reps"
        private const val KEY_USED_EXERCISES = "used_exercises"
    }

    private val json = Json { ignoreUnknownKeys = true }

    val timerState: StateFlow<TimerState> = timerStateHolder.state

    val hours: StateFlow<Int> = repository.timerHours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_HOURS)

    val minutes: StateFlow<Int> = repository.timerMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_MINUTES)

    val repsMin: StateFlow<Int> = repository.repsMin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_REPS_MIN)

    val repsMax: StateFlow<Int> = repository.repsMax
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_REPS_MAX)

    val repsLinked: StateFlow<Boolean> = repository.repsLinked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_REPS_LINKED)

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

    private val _currentReps = MutableStateFlow(savedStateHandle.get<Int>(KEY_CURRENT_REPS))
    val currentReps: StateFlow<Int?> = _currentReps.asStateFlow()

    private val usedExerciseNames: MutableSet<String> =
        (savedStateHandle.get<ArrayList<String>>(KEY_USED_EXERCISES) ?: arrayListOf()).toMutableSet()

    init {
        viewModelScope.launch {
            _currentExercise.collect { exercise ->
                savedStateHandle[KEY_CURRENT_EXERCISE] = exercise?.let { json.encodeToString(it) }
            }
        }
        viewModelScope.launch {
            _currentReps.collect { reps ->
                savedStateHandle[KEY_CURRENT_REPS] = reps
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

    fun setRepsMin(value: Int) {
        viewModelScope.launch {
            try {
                val coerced = value.coerceIn(1, 50)
                repository.setRepsMin(coerced)
                if (repsLinked.value) {
                    repository.setRepsMax(coerced)
                } else if (coerced > repsMax.value) {
                    repository.setRepsMax(coerced)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set reps min", e)
            }
        }
    }

    fun setRepsMax(value: Int) {
        viewModelScope.launch {
            try {
                repository.setRepsMax(value.coerceIn(repsMin.value, 50))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set reps max", e)
            }
        }
    }

    fun setRepsLinked(value: Boolean) {
        viewModelScope.launch {
            try {
                repository.setRepsLinked(value)
                if (value) {
                    repository.setRepsMax(repsMin.value)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set reps linked", e)
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
        _currentReps.value = null
        usedExerciseNames.clear()
        savedStateHandle[KEY_USED_EXERCISES] = arrayListOf<String>()
        serviceController.resetTimer()
    }

    fun onTimerExpired() {
        viewModelScope.launch {
            val allExercises = repository.exercises.first()
            val enabledExercises = allExercises.filter { it.isEnabled }
            if (enabledExercises.isNotEmpty()) {
                var available = enabledExercises.filter { it.name !in usedExerciseNames }
                if (available.isEmpty()) {
                    usedExerciseNames.clear()
                    available = enabledExercises
                }
                val picked = available.random()
                usedExerciseNames.add(picked.name)
                savedStateHandle[KEY_USED_EXERCISES] = ArrayList(usedExerciseNames)

                _currentExercise.value = picked
                val min = repsMin.value
                val max = repsMax.value
                _currentReps.value = (min..max).random()
            }
        }
    }

    fun onExerciseDone() {
        _currentExercise.value = null
        _currentReps.value = null
        val totalSeconds = (hours.value * 3600L) + (minutes.value * 60L)
        if (totalSeconds <= 0) return

        serviceController.restartTimer(totalSeconds)
    }
}
