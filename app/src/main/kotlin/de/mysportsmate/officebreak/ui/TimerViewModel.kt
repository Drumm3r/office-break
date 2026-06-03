package de.mysportsmate.officebreak.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import de.mysportsmate.officebreak.R
import android.content.Intent
import de.mysportsmate.officebreak.data.AchievementDefinition
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.validated
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.resolveEffectiveSchedule
import de.mysportsmate.officebreak.data.AchievementState
import de.mysportsmate.officebreak.data.BackupManager
import de.mysportsmate.officebreak.data.BreakRecord
import de.mysportsmate.officebreak.data.Exercise
import de.mysportsmate.officebreak.data.ExerciseMode
import de.mysportsmate.officebreak.data.FitnessLevel
import de.mysportsmate.officebreak.data.ImportResult
import de.mysportsmate.officebreak.data.MAX_BACKUP_SIZE_BYTES
import de.mysportsmate.officebreak.data.MAX_EXERCISE_NAME_LENGTH
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.ShuffleBag
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

@kotlinx.serialization.Serializable
internal data class ActiveBreakPayload(
    val exercise: Exercise,
    val reps: Int,
)

class TimerViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repository: SettingsRepository = SettingsRepository(application),
    private val statsRepository: StatsRepository = StatsRepository(application),
    private val timerStateHolder: TimerStateHolder = TimerStateHolder.instance,
    private val serviceController: TimerServiceController = DefaultTimerServiceController(application),
) : AndroidViewModel(application) {

    companion object {
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
        const val KOFI_URL = "https://ko-fi.com/drumm3r"
        const val IMPRINT_URL = "https://mysportsmate.de/impressum"
        const val PRIVACY_URL = "https://mysportsmate.de/datenschutz-officebreak"
    }

    private val json = AppJson
    private val backupManager = BackupManager(repository, statsRepository)

    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    private val _addExerciseError = MutableStateFlow<String?>(null)
    val addExerciseError: StateFlow<String?> = _addExerciseError.asStateFlow()

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

    val exerciseMode: StateFlow<ExerciseMode> = repository.exerciseMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseMode.HOME_WORKOUT)

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

    val isMusicPlaying: StateFlow<Boolean> = timerStateHolder.isMusicPlaying

    val workScheduleEnabled: StateFlow<Boolean> = repository.workScheduleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_WORK_SCHEDULE_ENABLED)

    val weekSchedule: StateFlow<List<DaySchedule>> = repository.weekSchedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_WEEK_SCHEDULE)

    val autoModeByDayEnabled: StateFlow<Boolean> = repository.autoModeByDayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_AUTO_MODE_BY_DAY)

    val modeOverrideForToday: StateFlow<ExerciseMode?> = repository.modeOverrideForToday
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    private val installTimestamp: StateFlow<Long> = repository.installTimestamp
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val donationPromptLastShown: StateFlow<Long> = repository.donationPromptLastShown
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private val donationPromptDismissed: StateFlow<Boolean> = repository.donationPromptDismissed
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val devModeEnabled: StateFlow<Boolean> = repository.devModeEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val cloudBackupEnabled: StateFlow<Boolean> = repository.cloudBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_CLOUD_BACKUP_ENABLED)

    private val _settingsDump = MutableStateFlow<String?>(null)
    val settingsDump: StateFlow<String?> = _settingsDump.asStateFlow()

    private val restoredExerciseFromHandle: Exercise? =
        savedStateHandle.get<String>(KEY_CURRENT_EXERCISE)?.let { raw ->
            try {
                json.decodeFromString<Exercise>(raw)
            } catch (_: Exception) {
                null
            }
        }

    private val restoredFromDataStore: ActiveBreakPayload? =
        if (restoredExerciseFromHandle == null) {
            try {
                runBlocking { repository.activeBreakState.first() }?.let { raw ->
                    try {
                        json.decodeFromString<ActiveBreakPayload>(raw)
                    } catch (_: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read persisted active break state", e)
                null
            }
        } else {
            null
        }

    private val _currentExercise = MutableStateFlow(
        restoredExerciseFromHandle ?: restoredFromDataStore?.exercise,
    )
    val currentExercise: StateFlow<Exercise?> = _currentExercise.asStateFlow()

    private val _currentReps = MutableStateFlow(
        savedStateHandle.get<Int>(KEY_CURRENT_REPS) ?: restoredFromDataStore?.reps,
    )
    val currentReps: StateFlow<Int?> = _currentReps.asStateFlow()

    val showDonationPrompt: StateFlow<Boolean> = combine(
        installTimestamp,
        donationPromptLastShown,
        donationPromptDismissed,
        timerState,
        _currentExercise,
    ) { installedAt, lastShown, dismissed, state, activeExercise ->
        val busy = state !is TimerState.Idle || activeExercise != null
        DonationPromptResolver.shouldShow(
            installTimestamp = installedAt,
            lastShown = lastShown,
            dismissed = dismissed,
            timerActive = busy,
            nowMillis = System.currentTimeMillis(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var previewTrack: android.media.AudioTrack? = null
    private var previewPlayer: android.media.MediaPlayer? = null
    private var previewJob: kotlinx.coroutines.Job? = null
    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

    private var _isFreestyle = false
    private var shuffleBag = ShuffleBag()

    private inline fun launchSafely(
        errorMessage: String,
        crossinline block: suspend () -> Unit,
    ): kotlinx.coroutines.Job = viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, errorMessage, e)
        }
    }

    init {
        viewModelScope.launch {
            combine(_currentExercise, _currentReps) { exercise, reps -> exercise to reps }
                .collect { (exercise, reps) ->
                    savedStateHandle[KEY_CURRENT_EXERCISE] = exercise?.let { json.encodeToString(it) }
                    savedStateHandle[KEY_CURRENT_REPS] = reps
                    val payload = if (exercise != null && reps != null) {
                        json.encodeToString(ActiveBreakPayload(exercise, reps))
                    } else {
                        null
                    }
                    try {
                        repository.setActiveBreakState(payload)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to persist active break state", e)
                    }
                }
        }
        viewModelScope.launch {
            shuffleBag = ShuffleBag(
                initialUsed = repository.usedExerciseNames.first(),
                initialLast = repository.lastPickedName.first(),
            )
        }
        viewModelScope.launch {
            try {
                statsRepository.runYearlyCompaction()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to run yearly compaction", e)
            }
        }
        launchSafely("Failed to ensure install timestamp") {
            repository.ensureInstallTimestamp(System.currentTimeMillis())
        }

        try {
            val lifecycleObserver = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    applyDayDefaultModeIfEnabled()
                }
            }
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        } catch (e: Exception) {
            Log.w(TAG, "ProcessLifecycleOwner unavailable (likely unit test)", e)
        }
    }

    fun applyDayDefaultModeIfEnabled() {
        viewModelScope.launch {
            applyDayDefaultModeNow()
        }
    }

    private suspend fun applyDayDefaultModeNow() {
        try {
            val enabled = repository.autoModeByDayEnabled.first()
            if (!enabled) return
            val currentMode = repository.exerciseMode.first()
            val override = repository.modeOverrideForToday.first()
            if (override != null) {
                if (override != currentMode) {
                    repository.setExerciseMode(override)
                    shuffleBag.reset()
                    repository.setUsedExerciseNames(emptySet())
                    repository.setLastPickedName(null)
                }
                return
            }
            val schedule = repository.weekSchedule.first()
            val dayIndex = LocalDate.now().dayOfWeek.ordinal
            val effective = resolveEffectiveSchedule(schedule, dayIndex) ?: return
            if (effective.defaultMode != currentMode) {
                repository.setExerciseMode(effective.defaultMode)
                shuffleBag.reset()
                repository.setUsedExerciseNames(emptySet())
                repository.setLastPickedName(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply day default mode", e)
        }
    }

    fun setAutoModeByDayEnabled(enabled: Boolean) {
        launchSafely("Failed to set auto mode by day enabled") {
            val seed = if (enabled) exerciseMode.value else null
            repository.setAutoModeByDayEnabled(enabled, seed)
            if (enabled) applyDayDefaultModeNow()
        }
    }

    fun setHours(value: Int) {
        launchSafely("Failed to set hours") {
            repository.setTimerHours(value.coerceIn(0, 23))
        }
    }

    fun setMinutes(value: Int) {
        launchSafely("Failed to set minutes") {
            repository.setTimerMinutes(value.coerceIn(0, 59))
        }
    }

    fun setRepsMin(value: Int) {
        launchSafely("Failed to set reps min") {
            val coerced = value.coerceIn(1, 50)
            repository.setRepsMin(coerced)
            if (repsLinked.value) {
                repository.setRepsMax(coerced)
            } else if (coerced > repsMax.value) {
                repository.setRepsMax(coerced)
            }
        }
    }

    fun setRepsMax(value: Int) {
        launchSafely("Failed to set reps max") {
            repository.setRepsMax(value.coerceIn(repsMin.value, 50))
        }
    }

    fun setRepsLinked(value: Boolean) {
        launchSafely("Failed to set reps linked") {
            repository.setRepsLinked(value)
            if (value) {
                repository.setRepsMax(repsMin.value)
            }
        }
    }

    fun setLanguage(value: String) {
        launchSafely("Failed to set language") {
            repository.setLanguage(value)
        }
    }

    fun setBeepVolume(value: Int) {
        launchSafely("Failed to set beep volume") {
            repository.setBeepVolume(value.coerceIn(0, 100))
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        try {
            previewTrack?.stop()
            previewTrack?.release()
        } catch (_: Exception) { }
        previewTrack = null
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) { }
        previewPlayer = null
        _isPreviewPlaying.value = false
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
    }

    fun playPreviewBeep(volume: Int) {
        if (volume <= 0) return
        stopPreview()
        _isPreviewPlaying.value = true
        previewJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
                    .setTransferMode(android.media.AudioTrack.MODE_STREAM)
                    .build()
                previewTrack = track
                try {
                    track.play()
                    track.write(samples, 0, samples.size)
                    kotlinx.coroutines.delay(beepDurationMs.toLong() + 50)
                } finally {
                    try {
                        track.stop()
                        track.release()
                    } catch (_: Exception) { }
                    previewTrack = null
                    _isPreviewPlaying.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play preview beep", e)
                _isPreviewPlaying.value = false
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
        stopPreview()
        launchSafely("Failed to clear custom sound") {
            repository.setCustomSoundUri(null)
        }
    }

    fun playPreviewSound(volume: Int) {
        val uri = customSoundUri.value
        if (volume <= 0) return
        if (uri != null) {
            stopPreview()
            _isPreviewPlaying.value = true
            previewJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
                    previewPlayer = player
                    player.setOnCompletionListener {
                        it.release()
                        previewPlayer = null
                        _isPreviewPlaying.value = false
                    }
                    player.start()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play custom sound preview, falling back to beep", e)
                    _isPreviewPlaying.value = false
                    playPreviewBeep(volume)
                }
            }
        } else {
            playPreviewBeep(volume)
        }
    }

    fun setTtsEnabled(value: Boolean) {
        launchSafely("Failed to set TTS enabled") {
            repository.setTtsEnabled(value)
        }
    }

    fun setWorkScheduleEnabled(value: Boolean) {
        launchSafely("Failed to set work schedule enabled") {
            repository.setWorkScheduleEnabled(value)
            val app = getApplication<Application>()
            if (value) {
                val schedule = weekSchedule.value
                WorkScheduleManager.scheduleNextWorkStartReminder(app, schedule)
            } else {
                WorkScheduleManager.cancelWorkStartReminder(app)
            }
        }
    }

    fun updateDaySchedule(dayIndex: Int, day: DaySchedule) {
        launchSafely("Failed to update day schedule") {
            val current = weekSchedule.value.toMutableList()
            if (dayIndex in current.indices) {
                current[dayIndex] = day.validated()
                repository.setWeekSchedule(current)
                if (workScheduleEnabled.value) {
                    WorkScheduleManager.scheduleNextWorkStartReminder(
                        getApplication(), current,
                    )
                }
                applyDayDefaultModeNow()
            }
        }
    }

    fun setDynamicIncreaseEnabled(value: Boolean) {
        launchSafely("Failed to set dynamic increase enabled") {
            repository.setDynamicIncreaseEnabled(value)
        }
    }

    fun setBeepCount(value: Int) {
        launchSafely("Failed to set beep count") {
            repository.setBeepCount(value.coerceIn(1, 5))
        }
    }

    fun setExerciseMode(mode: ExerciseMode) {
        launchSafely("Failed to set exercise mode") {
            repository.setExerciseMode(mode)
            if (repository.autoModeByDayEnabled.first()) {
                repository.setModeOverrideForToday(mode)
            }
            shuffleBag.reset()
            repository.setUsedExerciseNames(emptySet())
            repository.setLastPickedName(null)
        }
    }

    fun toggleExercise(index: Int) {
        launchSafely("Failed to toggle exercise") {
            val current = exercises.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(isEnabled = !current[index].isEnabled)
                repository.setExercises(current)
            }
        }
    }

    fun addExercise(name: String) {
        val trimmed = name.trim().take(MAX_EXERCISE_NAME_LENGTH)
        if (trimmed.isEmpty()) return

        launchSafely("Failed to add exercise") {
            // Add enabled to active mode, disabled to other modes
            val activeMode = exerciseMode.value
            val perMode = ExerciseMode.entries.associateWith { mode ->
                repository.exercisesForMode(mode).first()
            }

            // Reject duplicate names (case-insensitive) across all modes
            val normalized = trimmed.lowercase()
            val isDuplicate = perMode.values.any { list ->
                list.any { it.name.trim().lowercase() == normalized }
            }
            if (isDuplicate) {
                _addExerciseError.value = getApplication<Application>().getString(R.string.exercise_duplicate_name)
                return@launchSafely
            }

            for (mode in ExerciseMode.entries) {
                val modeExercises = perMode.getValue(mode).toMutableList()
                modeExercises.add(Exercise(name = trimmed, isEnabled = mode == activeMode))
                repository.setExercisesForMode(mode, modeExercises)
            }
            statsRepository.markCustomExerciseCreated()
        }
    }

    fun clearAddExerciseError() {
        _addExerciseError.value = null
    }

    fun removeExercise(index: Int) {
        viewModelScope.launch {
            try {
                val current = exercises.value.toMutableList()
                if (current.size > 1 && index in current.indices) {
                    val removedName = current[index].name
                    current.removeAt(index)
                    repository.setExercises(current)

                    // Remove from other modes too
                    val activeMode = exerciseMode.value
                    for (mode in ExerciseMode.entries) {
                        if (mode != activeMode) {
                            val modeExercises = repository.exercisesForMode(mode).first().toMutableList()
                            modeExercises.removeAll { it.name == removedName }
                            repository.setExercisesForMode(mode, modeExercises)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove exercise", e)
            }
        }
    }

    fun startTimer() {
        val totalSeconds = (hours.value * 3600L) + (minutes.value * 60L)
        if (totalSeconds <= 0) return

        val freestyle = shouldStartAsFreestyle()
        _isFreestyle = freestyle

        serviceController.startTimer(totalSeconds, language.value, freestyle)
    }

    private fun shouldStartAsFreestyle(): Boolean {
        if (!workScheduleEnabled.value) return false
        val schedule = weekSchedule.value
        val dayIndex = java.time.LocalDate.now().dayOfWeek.ordinal
        val todaySchedule = resolveEffectiveSchedule(schedule, dayIndex) ?: return true
        val now = java.time.LocalTime.now()
        val workStart = java.time.LocalTime.of(todaySchedule.workStartHour, todaySchedule.workStartMinute)
        val workEnd = java.time.LocalTime.of(todaySchedule.workEndHour, todaySchedule.workEndMinute)

        return now.isBefore(workStart) || !now.isBefore(workEnd)
    }

    fun resetTimer() {
        _isFreestyle = false
        _currentExercise.value = null
        _currentReps.value = null
        shuffleBag.reset()
        viewModelScope.launch {
            repository.setUsedExerciseNames(emptySet())
            repository.setLastPickedName(null)
            repository.clearModeOverride()
        }
        serviceController.resetTimer()
    }

    fun toggleMusicPlayback() {
        if (timerStateHolder.isMusicPlaying.value) {
            serviceController.pauseMusic()
        } else {
            serviceController.resumeMusic()
        }
    }

    fun dismissWorkEnded() {
        _isFreestyle = false
        timerStateHolder.update(TimerState.Idle)
        viewModelScope.launch {
            try {
                repository.clearModeOverride()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear mode override", e)
            }
        }
        serviceController.resetTimer()
    }

    fun onTimerExpired() {
        if (_currentExercise.value != null) return
        viewModelScope.launch {
            val enabledExercises = repository.exercises.first().filter { it.isEnabled }
            val picked = shuffleBag.pick(enabledExercises) ?: return@launch
            repository.setUsedExerciseNames(shuffleBag.usedNames)
            repository.setLastPickedName(picked.name)

            _currentExercise.value = picked
            val min = repsMin.value
            val max = repsMax.value
            _currentReps.value = (min..max).random()
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
            _isFreestyle = false
            serviceController.resetTimer()

            return
        }
        val totalSeconds = (hours.value * 3600L) + (minutes.value * 60L)
        if (totalSeconds <= 0) return

        serviceController.restartTimer(totalSeconds, language.value, _isFreestyle)
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
                _backupState.value = BackupUiState.Error(app.getString(R.string.backup_error_generic))
            }
        }
    }

    fun importData(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val size = app.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.SIZE),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L
                } ?: -1L

                if (size in 1..MAX_BACKUP_SIZE_BYTES) {
                    // size known and within bounds - proceed
                } else if (size > MAX_BACKUP_SIZE_BYTES) {
                    _backupState.value = BackupUiState.Error(app.getString(R.string.import_error_too_large))
                    return@launch
                }

                val jsonString = app.contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = stream.bufferedReader(Charsets.UTF_8)
                    val sb = StringBuilder()
                    val buf = CharArray(8 * 1024)
                    var total = 0L
                    while (true) {
                        val n = reader.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BACKUP_SIZE_BYTES) {
                            _backupState.value = BackupUiState.Error(app.getString(R.string.import_error_too_large))
                            return@launch
                        }
                        sb.appendRange(buf, 0, n)
                    }
                    sb.toString()
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
                _backupState.value = BackupUiState.Error(app.getString(R.string.backup_error_generic))
            }
        }
    }

    fun clearBackupState() {
        _backupState.value = BackupUiState.Idle
    }

    fun applyWorkSchedule(enabled: Boolean, autoModeByDay: Boolean, schedule: List<DaySchedule>) {
        viewModelScope.launch {
            try {
                repository.setWorkScheduleEnabled(enabled)
                repository.setWeekSchedule(schedule)
                repository.setAutoModeByDayEnabled(autoModeByDay)
                if (enabled) {
                    WorkScheduleManager.scheduleNextWorkStartReminder(
                        getApplication(), schedule,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply work schedule", e)
            }
        }
    }

    fun onDonationShown() {
        launchSafely("Failed to mark donation prompt shown") {
            repository.markDonationPromptShown(System.currentTimeMillis())
        }
    }

    fun onDonationSupport() {
        launchSafely("Failed to mark donation prompt supported") {
            repository.markDonationPromptDismissed()
        }
        openKofiLink()
    }

    fun onDonationLater() {
        launchSafely("Failed to snooze donation prompt") {
            repository.markDonationPromptShown(System.currentTimeMillis())
        }
    }

    fun onDonationDismiss() {
        launchSafely("Failed to dismiss donation prompt") {
            repository.markDonationPromptDismissed()
        }
    }

    fun setDevModeEnabled(enabled: Boolean) {
        launchSafely("Failed to toggle dev mode") {
            repository.setDevModeEnabled(enabled)
        }
    }

    fun setCloudBackupEnabled(enabled: Boolean) {
        launchSafely("Failed to toggle cloud backup") {
            repository.setCloudBackupEnabled(enabled)
        }
    }

    fun resetDonationPromptForTesting() {
        launchSafely("Failed to reset donation prompt") {
            repository.resetDonationPrompt(System.currentTimeMillis())
        }
    }

    fun resetOnboarding() {
        launchSafely("Failed to reset onboarding") {
            repository.setOnboardingCompleted(false)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.clearAll()
                statsRepository.resetAllStats()
                WidgetUpdater.requestUpdate(getApplication())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all data", e)
            }
        }
    }

    fun loadSettingsDump() {
        viewModelScope.launch {
            try {
                val prefs = repository.dumpPreferences()
                val formatted = buildString {
                    append("App: ")
                    append(getApplication<Application>().packageName)
                    append('\n')
                    append("Version: ")
                    append(de.mysportsmate.officebreak.BuildConfig.VERSION_NAME)
                    append(" (")
                    append(de.mysportsmate.officebreak.BuildConfig.VERSION_CODE)
                    append(")\n")
                    append("Build: ")
                    append(if (de.mysportsmate.officebreak.BuildConfig.DEBUG) "debug" else "release")
                    append('\n')
                    append("Git: ")
                    append(de.mysportsmate.officebreak.BuildConfig.GIT_SHA)
                    append('\n')
                    append("Built: ")
                    append(de.mysportsmate.officebreak.BuildConfig.BUILD_TIMESTAMP)
                    append("\n\n=== DataStore ===\n")
                    prefs.toSortedMap().forEach { (key, value) ->
                        append(key).append(" = ").append(value).append('\n')
                    }
                }
                _settingsDump.value = formatted
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dump settings", e)
                _settingsDump.value = "Error: ${e.message}"
            }
        }
    }

    fun clearSettingsDump() {
        _settingsDump.value = null
    }

    fun openKofiLink() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(KOFI_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity available to open Ko-fi link", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Ko-fi link", e)
        }
    }

    fun openImprintLink() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(IMPRINT_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity available to open Imprint link", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Imprint link", e)
        }
    }

    fun openPrivacyLink() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity available to open Privacy link", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Privacy link", e)
        }
    }

    fun completeOnboarding(level: FitnessLevel, mode: ExerciseMode) {
        viewModelScope.launch {
            try {
                repository.setTimerHours(level.hours)
                repository.setTimerMinutes(level.minutes)
                repository.setRepsMin(level.reps)
                repository.setRepsMax(level.reps)
                repository.setRepsLinked(true)
                repository.setExerciseMode(mode)
                repository.setOnboardingCompleted(true)
                applyDayDefaultModeNow()
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
