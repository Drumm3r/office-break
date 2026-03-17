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
        private const val KEY_LAST_PICKED_NAME = "last_picked_name"
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

    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_SOUND_ENABLED)

    val vibrationEnabled: StateFlow<Boolean> = repository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_VIBRATION_ENABLED)

    val themeMode: StateFlow<String> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.THEME_SYSTEM)

    val keepScreenOn: StateFlow<Boolean> = repository.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_KEEP_SCREEN_ON)

    val autoRestart: StateFlow<Boolean> = repository.autoRestart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_AUTO_RESTART)

    val beepCount: StateFlow<Int> = repository.beepCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_BEEP_COUNT)

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

    private var lastPickedName: String? = savedStateHandle.get<String>(KEY_LAST_PICKED_NAME)

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

    fun setSoundEnabled(value: Boolean) {
        viewModelScope.launch {
            try {
                repository.setSoundEnabled(value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set sound enabled", e)
            }
        }
    }

    fun setVibrationEnabled(value: Boolean) {
        viewModelScope.launch {
            try {
                repository.setVibrationEnabled(value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set vibration enabled", e)
            }
        }
    }

    fun setThemeMode(value: String) {
        viewModelScope.launch {
            try {
                repository.setThemeMode(value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set theme mode", e)
            }
        }
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch {
            try {
                repository.setKeepScreenOn(value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set keep screen on", e)
            }
        }
    }

    fun setAutoRestart(value: Boolean) {
        viewModelScope.launch {
            try {
                repository.setAutoRestart(value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set auto restart", e)
            }
        }
    }

    fun setBeepCount(value: Int) {
        viewModelScope.launch {
            try {
                repository.setBeepCount(value.coerceIn(1, 5))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set beep count", e)
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
        lastPickedName = null
        savedStateHandle[KEY_USED_EXERCISES] = arrayListOf<String>()
        savedStateHandle[KEY_LAST_PICKED_NAME] = null
        serviceController.resetTimer()
    }

    fun onTimerExpired() {
        viewModelScope.launch {
            val allExercises = repository.exercises.first()
            val enabledExercises = allExercises.filter { it.isEnabled }
            if (enabledExercises.isNotEmpty()) {
                if (lastPickedName != null && lastPickedName !in usedExerciseNames) {
                    usedExerciseNames.add(lastPickedName!!)
                }

                var available = enabledExercises.filter { it.name !in usedExerciseNames }
                if (available.isEmpty()) {
                    usedExerciseNames.clear()
                    if (lastPickedName != null && enabledExercises.size > 1) {
                        usedExerciseNames.add(lastPickedName!!)
                    }
                    available = enabledExercises.filter { it.name !in usedExerciseNames }
                }
                val picked = available.random()
                usedExerciseNames.add(picked.name)
                lastPickedName = picked.name
                savedStateHandle[KEY_USED_EXERCISES] = ArrayList(usedExerciseNames)
                savedStateHandle[KEY_LAST_PICKED_NAME] = picked.name

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
        if (!autoRestart.value) {
            serviceController.resetTimer()
            return
        }
        val totalSeconds = (hours.value * 3600L) + (minutes.value * 60L)
        if (totalSeconds <= 0) return

        serviceController.restartTimer(totalSeconds)
    }
}
