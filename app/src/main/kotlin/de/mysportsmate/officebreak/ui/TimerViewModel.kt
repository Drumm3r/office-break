package de.mysportsmate.officebreak.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import de.mysportsmate.officebreak.data.AchievementDefinition
import de.mysportsmate.officebreak.data.AchievementState
import de.mysportsmate.officebreak.data.BreakRecord
import de.mysportsmate.officebreak.data.Exercise
import de.mysportsmate.officebreak.data.FitnessLevel
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.StatsRepository
import de.mysportsmate.officebreak.data.StatsSnapshot
import de.mysportsmate.officebreak.service.DefaultTimerServiceController
import de.mysportsmate.officebreak.service.TimerServiceController
import de.mysportsmate.officebreak.service.TimerState
import de.mysportsmate.officebreak.service.TimerStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

class TimerViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repository: SettingsRepository = SettingsRepository(application),
    private val statsRepository: StatsRepository = StatsRepository(application),
    private val timerStateHolder: TimerStateHolder = TimerStateHolder.instance,
    private val serviceController: TimerServiceController = DefaultTimerServiceController(application),
) : AndroidViewModel(application) {

    companion object {
        const val MAX_EXERCISE_NAME_LENGTH = 100
        private const val TAG = "TimerViewModel"
        private const val KEY_CURRENT_EXERCISE = "current_exercise"
        private const val KEY_CURRENT_REPS = "current_reps"
    }

    private val json = Json { ignoreUnknownKeys = true }

    val onboardingCompleted: StateFlow<Boolean?> = repository.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    val beepVolume: StateFlow<Int> = repository.beepVolume
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_BEEP_VOLUME)

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

    val trackingEnabled: StateFlow<Boolean> = statsRepository.trackingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsRepository.DEFAULT_TRACKING_ENABLED)

    val statsSnapshot: StateFlow<StatsSnapshot> = statsRepository.statsSnapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsSnapshot())

    val achievementState: StateFlow<AchievementState> = statsRepository.achievementState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementState())

    val breakRecords: StateFlow<List<BreakRecord>> = statsRepository.breakRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _newlyUnlockedAchievements = MutableStateFlow<List<AchievementDefinition>>(emptyList())
    val newlyUnlockedAchievements: StateFlow<List<AchievementDefinition>> = _newlyUnlockedAchievements.asStateFlow()

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

    private val usedExerciseNames: MutableSet<String> = mutableSetOf()
    private var lastPickedName: String? = null

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
        viewModelScope.launch {
            usedExerciseNames.addAll(repository.usedExerciseNames.first())
            lastPickedName = repository.lastPickedName.first()
        }
        viewModelScope.launch {
            try {
                statsRepository.runYearlyCompaction()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to run yearly compaction", e)
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

    fun setBeepVolume(value: Int) {
        viewModelScope.launch {
            try {
                repository.setBeepVolume(value.coerceIn(0, 100))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set beep volume", e)
            }
        }
    }

    fun playPreviewBeep(volume: Int) {
        if (volume <= 0) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sampleRate = 44100
                val beepDurationMs = 150
                val frequency = 1000.0
                val amplitude = volume / 100.0

                val beepSamples = (sampleRate * beepDurationMs) / 1000
                val samples = ShortArray(beepSamples)
                for (i in 0 until beepSamples) {
                    val angle = 2.0 * Math.PI * frequency * i / sampleRate
                    samples[i] = (Math.sin(angle) * Short.MAX_VALUE * amplitude).toInt().toShort()
                }

                val track = android.media.AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        android.media.AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(android.media.AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                kotlinx.coroutines.delay(beepDurationMs.toLong() + 50)
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play preview beep", e)
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
                statsRepository.markCustomExerciseCreated()
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

        serviceController.startTimer(totalSeconds, language.value)
    }

    fun resetTimer() {
        _currentExercise.value = null
        _currentReps.value = null
        usedExerciseNames.clear()
        lastPickedName = null
        viewModelScope.launch {
            repository.setUsedExerciseNames(emptySet())
            repository.setLastPickedName(null)
        }
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
                repository.setUsedExerciseNames(usedExerciseNames)
                repository.setLastPickedName(picked.name)

                _currentExercise.value = picked
                val min = repsMin.value
                val max = repsMax.value
                _currentReps.value = (min..max).random()
            }
        }
    }

    fun onExerciseDone() {
        val exercise = _currentExercise.value
        val reps = _currentReps.value
        _currentExercise.value = null
        _currentReps.value = null

        if (exercise != null && reps != null) {
            viewModelScope.launch {
                try {
                    val enabled = statsRepository.trackingEnabled.first()
                    if (enabled) {
                        val now = System.currentTimeMillis()
                        val record = BreakRecord(
                            exerciseName = exercise.name,
                            reps = reps,
                            timestampMillis = now,
                            dateString = LocalDate.now().toString(),
                        )
                        val newAchievements = statsRepository.recordBreak(record)
                        if (newAchievements.isNotEmpty()) {
                            _newlyUnlockedAchievements.value = newAchievements
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to record break stats", e)
                }
            }
        }

        if (!autoRestart.value) {
            serviceController.resetTimer()
            return
        }
        val totalSeconds = (hours.value * 3600L) + (minutes.value * 60L)
        if (totalSeconds <= 0) return

        serviceController.restartTimer(totalSeconds, language.value)
    }

    fun dismissAchievementCelebration() {
        _newlyUnlockedAchievements.value = emptyList()
    }

    fun completeOnboarding(level: FitnessLevel, selectedExercises: List<Exercise>) {
        viewModelScope.launch {
            try {
                repository.setTimerHours(level.hours)
                repository.setTimerMinutes(level.minutes)
                repository.setRepsMin(level.reps)
                repository.setRepsMax(level.reps)
                repository.setRepsLinked(true)
                repository.setExercises(selectedExercises)
                repository.setOnboardingCompleted(true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete onboarding", e)
            }
        }
    }

    fun setTrackingEnabled(value: Boolean) {
        viewModelScope.launch {
            try {
                statsRepository.setTrackingEnabled(value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set tracking enabled", e)
            }
        }
    }

    fun resetStats() {
        viewModelScope.launch {
            try {
                statsRepository.resetAllStats()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset stats", e)
            }
        }
    }
}
