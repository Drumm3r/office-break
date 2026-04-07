package de.mysportsmate.officebreak.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import de.mysportsmate.officebreak.R
import android.content.Intent
import de.mysportsmate.officebreak.data.AchievementDefinition
import de.mysportsmate.officebreak.data.AchievementState
import de.mysportsmate.officebreak.data.BackupManager
import de.mysportsmate.officebreak.data.BreakRecord
import de.mysportsmate.officebreak.data.Exercise
import de.mysportsmate.officebreak.data.FitnessLevel
import de.mysportsmate.officebreak.data.ImportResult
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.StatsRepository
import de.mysportsmate.officebreak.data.StatsSnapshot
import de.mysportsmate.officebreak.service.DefaultTimerServiceController
import de.mysportsmate.officebreak.service.WorkScheduleManager
import de.mysportsmate.officebreak.service.TimerServiceController
import de.mysportsmate.officebreak.service.TimerState
import de.mysportsmate.officebreak.service.TimerStateHolder
import de.mysportsmate.officebreak.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import de.mysportsmate.officebreak.data.AppJson
import java.time.LocalDate

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data object ExportSuccess : BackupUiState
    data object ImportSuccess : BackupUiState
    data class Error(val message: String) : BackupUiState
}

sealed interface DynamicIncreaseOffer {
    data class Both(val newReps: Int, val newIntervalMinutes: Int) : DynamicIncreaseOffer
    data class RepsOnly(val newReps: Int) : DynamicIncreaseOffer
    data class IntervalOnly(val newIntervalMinutes: Int) : DynamicIncreaseOffer
}

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
        const val REPS_INCREASE = 2
        const val INTERVAL_DECREASE_MINUTES = 5
        const val MAX_REPS = 50
        const val MIN_INTERVAL_MINUTES = 5
        private const val TAG = "TimerViewModel"
        private const val KEY_CURRENT_EXERCISE = "current_exercise"
        private const val KEY_CURRENT_REPS = "current_reps"
        private const val WORK_DAY_MINUTES = 480
        private const val WORK_DAYS_FOR_INCREASE = 3
        private const val MIN_THRESHOLD = 5
    }

    private val json = AppJson
    private val backupManager = BackupManager(repository, statsRepository)

    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

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

    val ttsEnabled: StateFlow<Boolean> = repository.ttsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_TTS_ENABLED)

    val customSoundUri: StateFlow<String?> = repository.customSoundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val workScheduleEnabled: StateFlow<Boolean> = repository.workScheduleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_WORK_SCHEDULE_ENABLED)

    val workStartHour: StateFlow<Int> = repository.workStartHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_WORK_START_HOUR)

    val workStartMinute: StateFlow<Int> = repository.workStartMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_WORK_START_MINUTE)

    val workEndHour: StateFlow<Int> = repository.workEndHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_WORK_END_HOUR)

    val workEndMinute: StateFlow<Int> = repository.workEndMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_WORK_END_MINUTE)

    val lunchStartHour: StateFlow<Int> = repository.lunchStartHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_LUNCH_START_HOUR)

    val lunchStartMinute: StateFlow<Int> = repository.lunchStartMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_LUNCH_START_MINUTE)

    val lunchEndHour: StateFlow<Int> = repository.lunchEndHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_LUNCH_END_HOUR)

    val lunchEndMinute: StateFlow<Int> = repository.lunchEndMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_LUNCH_END_MINUTE)

    val dynamicIncreaseEnabled: StateFlow<Boolean> = repository.dynamicIncreaseEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_DYNAMIC_INCREASE_ENABLED)

    private val breaksSinceLastIncrease: StateFlow<Int> = repository.breaksSinceLastIncrease
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_BREAKS_SINCE_LAST_INCREASE)

    private val _dynamicIncreaseOffer = MutableStateFlow<DynamicIncreaseOffer?>(null)
    val dynamicIncreaseOffer: StateFlow<DynamicIncreaseOffer?> = _dynamicIncreaseOffer.asStateFlow()

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
                try {
                    track.write(samples, 0, samples.size)
                    track.play()
                    kotlinx.coroutines.delay(beepDurationMs.toLong() + 50)
                } finally {
                    track.stop()
                    track.release()
                }
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

    fun setCustomSoundUri(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                repository.setCustomSoundUri(uri.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set custom sound URI", e)
            }
        }
    }

    fun clearCustomSound() {
        viewModelScope.launch {
            try {
                repository.setCustomSoundUri(null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear custom sound", e)
            }
        }
    }

    fun playPreviewSound(volume: Int) {
        val uri = customSoundUri.value
        if (volume <= 0) return
        if (uri != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val app = getApplication<Application>()
                    val player = android.media.MediaPlayer().apply {
                        setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        )
                        setDataSource(app, android.net.Uri.parse(uri))
                        prepare()
                        val vol = volume / 100f
                        setVolume(vol, vol)
                    }
                    player.setOnCompletionListener { it.release() }
                    player.start()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play custom sound preview, falling back to beep", e)
                    playPreviewBeep(volume)
                }
            }
        } else {
            playPreviewBeep(volume)
        }
    }

    fun setTtsEnabled(value: Boolean) {
        viewModelScope.launch {
            try {
                repository.setTtsEnabled(value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set TTS enabled", e)
            }
        }
    }

    fun setWorkScheduleEnabled(value: Boolean) {
        viewModelScope.launch {
            try {
                repository.setWorkScheduleEnabled(value)
                val app = getApplication<Application>()
                if (value) {
                    WorkScheduleManager.scheduleNextWorkStartReminder(
                        app, workStartHour.value, workStartMinute.value,
                    )
                } else {
                    WorkScheduleManager.cancelWorkStartReminder(app)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set work schedule enabled", e)
            }
        }
    }

    fun setWorkStartTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                repository.setWorkStartHour(hour)
                repository.setWorkStartMinute(minute)
                if (workScheduleEnabled.value) {
                    WorkScheduleManager.scheduleNextWorkStartReminder(
                        getApplication(), hour, minute,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set work start time", e)
            }
        }
    }

    fun setWorkEndTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                repository.setWorkEndHour(hour)
                repository.setWorkEndMinute(minute)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set work end time", e)
            }
        }
    }

    fun setLunchStartTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                repository.setLunchStartHour(hour)
                repository.setLunchStartMinute(minute)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set lunch start time", e)
            }
        }
    }

    fun setLunchEndTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                repository.setLunchEndHour(hour)
                repository.setLunchEndMinute(minute)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set lunch end time", e)
            }
        }
    }

    fun setDynamicIncreaseEnabled(value: Boolean) {
        viewModelScope.launch {
            try {
                repository.setDynamicIncreaseEnabled(value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set dynamic increase enabled", e)
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
        viewModelScope.launch {
            repository.setWidgetTimerStatus("running")
            WidgetUpdater.requestUpdate(getApplication())
        }
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
                        WidgetUpdater.requestUpdate(getApplication())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to record break stats", e)
                }

                if (dynamicIncreaseEnabled.value) {
                    try {
                        val newCount = breaksSinceLastIncrease.value + 1
                        repository.setBreaksSinceLastIncrease(newCount)

                        if (newCount >= computeIncreaseThreshold()) {
                            val offer = computeIncreaseOffer()
                            if (offer != null) {
                                _dynamicIncreaseOffer.value = offer
                                return@launch
                            }
                            repository.setBreaksSinceLastIncrease(0)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to check dynamic increase", e)
                    }
                }

                restartOrResetTimer()
            }
        } else {
            viewModelScope.launch { restartOrResetTimer() }
        }
    }

    fun acceptIncreaseReps() {
        viewModelScope.launch {
            try {
                val newMin = (repsMin.value + REPS_INCREASE).coerceAtMost(MAX_REPS)
                val newMax = (repsMax.value + REPS_INCREASE).coerceAtMost(MAX_REPS)
                repository.setRepsMin(newMin)
                repository.setRepsMax(newMax)
                repository.setBreaksSinceLastIncrease(0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to increase reps", e)
            }
            _dynamicIncreaseOffer.value = null
            restartOrResetTimer()
        }
    }

    fun acceptDecreaseInterval() {
        viewModelScope.launch {
            try {
                val totalMinutes = hours.value * 60 + minutes.value
                val newTotalMinutes = (totalMinutes - INTERVAL_DECREASE_MINUTES).coerceAtLeast(MIN_INTERVAL_MINUTES)
                repository.setTimerHours(newTotalMinutes / 60)
                repository.setTimerMinutes(newTotalMinutes % 60)
                repository.setBreaksSinceLastIncrease(0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrease interval", e)
            }
            _dynamicIncreaseOffer.value = null
            restartOrResetTimer()
        }
    }

    fun declineDynamicIncrease() {
        viewModelScope.launch {
            try {
                repository.setBreaksSinceLastIncrease(0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset increase counter", e)
            }
            _dynamicIncreaseOffer.value = null
            restartOrResetTimer()
        }
    }

    private fun computeIncreaseThreshold(): Int {
        val intervalMinutes = hours.value * 60 + minutes.value
        if (intervalMinutes <= 0) return MIN_THRESHOLD

        return maxOf(MIN_THRESHOLD, (WORK_DAY_MINUTES / intervalMinutes) * WORK_DAYS_FOR_INCREASE)
    }

    private fun computeIncreaseOffer(): DynamicIncreaseOffer? {
        val currentMax = repsMax.value
        val totalMinutes = hours.value * 60 + minutes.value
        val canIncreaseReps = currentMax + REPS_INCREASE <= MAX_REPS
        val canDecreaseInterval = totalMinutes - INTERVAL_DECREASE_MINUTES >= MIN_INTERVAL_MINUTES

        return when {
            canIncreaseReps && canDecreaseInterval -> DynamicIncreaseOffer.Both(
                newReps = currentMax + REPS_INCREASE,
                newIntervalMinutes = totalMinutes - INTERVAL_DECREASE_MINUTES,
            )
            canIncreaseReps -> DynamicIncreaseOffer.RepsOnly(newReps = currentMax + REPS_INCREASE)
            canDecreaseInterval -> DynamicIncreaseOffer.IntervalOnly(
                newIntervalMinutes = totalMinutes - INTERVAL_DECREASE_MINUTES,
            )
            else -> null
        }
    }

    private suspend fun restartOrResetTimer() {
        if (!autoRestart.value) {
            serviceController.resetTimer()
            repository.setWidgetTimerStatus("idle")
            WidgetUpdater.requestUpdate(getApplication())
            return
        }
        val totalSeconds = (hours.value * 3600L) + (minutes.value * 60L)
        if (totalSeconds <= 0) return

        serviceController.restartTimer(totalSeconds, language.value)
        repository.setWidgetTimerStatus("running")
        WidgetUpdater.requestUpdate(getApplication())
    }

    fun dismissAchievementCelebration() {
        _newlyUnlockedAchievements.value = emptyList()
    }

    fun exportData(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val versionCode = app.packageManager
                    .getPackageInfo(app.packageName, 0).longVersionCode.toInt()
                val jsonString = backupManager.createBackupJson(versionCode)

                app.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonString.toByteArray(Charsets.UTF_8))
                } ?: throw Exception("Could not open file for writing")

                _backupState.value = BackupUiState.ExportSuccess
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export data", e)
                _backupState.value = BackupUiState.Error(
                    app.getString(R.string.backup_error, e.message ?: "Unknown error"),
                )
            }
        }
    }

    fun importData(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonString = app.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: throw Exception("Could not open file for reading")

                when (val result = backupManager.restoreFromJson(jsonString)) {
                    is ImportResult.Success -> {
                        WidgetUpdater.requestUpdate(app)
                        _backupState.value = BackupUiState.ImportSuccess
                    }
                    is ImportResult.Error -> {
                        _backupState.value = BackupUiState.Error(app.getString(result.messageResId))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import data", e)
                _backupState.value = BackupUiState.Error(
                    app.getString(R.string.backup_error, e.message ?: "Unknown error"),
                )
            }
        }
    }

    fun clearBackupState() {
        _backupState.value = BackupUiState.Idle
    }

    fun applyWorkSchedule(
        enabled: Boolean,
        startH: Int, startM: Int,
        endH: Int, endM: Int,
        lunchStartH: Int, lunchStartM: Int,
        lunchEndH: Int, lunchEndM: Int,
    ) {
        viewModelScope.launch {
            try {
                repository.setWorkScheduleEnabled(enabled)
                repository.setWorkStartHour(startH)
                repository.setWorkStartMinute(startM)
                repository.setWorkEndHour(endH)
                repository.setWorkEndMinute(endM)
                repository.setLunchStartHour(lunchStartH)
                repository.setLunchStartMinute(lunchStartM)
                repository.setLunchEndHour(lunchEndH)
                repository.setLunchEndMinute(lunchEndM)
                if (enabled) {
                    WorkScheduleManager.scheduleNextWorkStartReminder(
                        getApplication(), startH, startM,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply work schedule", e)
            }
        }
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
                WidgetUpdater.requestUpdate(getApplication())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset stats", e)
            }
        }
    }
}
