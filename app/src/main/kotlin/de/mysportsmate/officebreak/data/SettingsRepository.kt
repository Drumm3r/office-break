package de.mysportsmate.officebreak.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import android.util.Log

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val defaultExercisesByMode: Map<ExerciseMode, List<Exercise>> = emptyMap(),
) {

    constructor(context: Context) : this(
        dataStore = context.dataStore,
        defaultExercisesByMode = ExerciseMode.entries.associateWith { mode ->
            ExerciseConfig.defaultExercises(context, mode)
        },
    )

    private val json = AppJson

    val timerHours: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_TIMER_HOURS] ?: DEFAULT_HOURS
    }

    val timerMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_TIMER_MINUTES] ?: DEFAULT_MINUTES
    }

    val repsMin: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_REPS_MIN] ?: DEFAULT_REPS_MIN
    }

    val repsMax: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_REPS_MAX] ?: DEFAULT_REPS_MAX
    }

    val repsLinked: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_REPS_LINKED] ?: DEFAULT_REPS_LINKED
    }

    val exerciseMode: Flow<ExerciseMode> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_EXERCISE_MODE]
        if (raw != null) {
            try {
                ExerciseMode.valueOf(raw)
            } catch (_: IllegalArgumentException) {
                ExerciseMode.HOME_WORKOUT
            }
        } else {
            ExerciseMode.HOME_WORKOUT
        }
    }

    fun exercisesForMode(mode: ExerciseMode): Flow<List<Exercise>> = dataStore.data.map { prefs ->
        val key = exercisesKey(mode)
        val raw = prefs[key]
        val list = if (raw != null) {
            try {
                json.decodeFromString<List<Exercise>>(raw)
            } catch (e: Exception) {
                Log.w("SettingsRepository", "Failed to decode exercises for $mode, using defaults", e)
                defaultExercisesByMode[mode] ?: emptyList()
            }
        } else if (mode == ExerciseMode.HOME_WORKOUT) {
            // Migration: fall back to legacy KEY_EXERCISES for HOME_WORKOUT
            val legacyRaw = prefs[KEY_EXERCISES]
            if (legacyRaw != null) {
                try {
                    json.decodeFromString<List<Exercise>>(legacyRaw)
                } catch (e: Exception) {
                    Log.w("SettingsRepository", "Failed to decode legacy exercises", e)
                    defaultExercisesByMode[mode] ?: emptyList()
                }
            } else {
                defaultExercisesByMode[mode] ?: emptyList()
            }
        } else {
            defaultExercisesByMode[mode] ?: emptyList()
        }

        list.map { exercise ->
            if (exercise.nameResKey == null) {
                val resKey = KNOWN_DEFAULT_NAMES[exercise.name]
                if (resKey != null) exercise.copy(nameResKey = resKey) else exercise
            } else {
                exercise
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    val exercises: Flow<List<Exercise>> = exerciseMode.flatMapLatest { mode ->
        exercisesForMode(mode)
    }

    val language: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: LANGUAGE_SYSTEM
    }

    val beepVolume: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_BEEP_VOLUME] ?: DEFAULT_BEEP_VOLUME
    }

    val vibrationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION_ENABLED] ?: DEFAULT_VIBRATION_ENABLED
    }

    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: THEME_SYSTEM
    }

    val keepScreenOn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_KEEP_SCREEN_ON] ?: DEFAULT_KEEP_SCREEN_ON
    }

    val autoRestart: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_RESTART] ?: DEFAULT_AUTO_RESTART
    }

    val beepCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_BEEP_COUNT] ?: DEFAULT_BEEP_COUNT
    }

    val ttsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_TTS_ENABLED] ?: DEFAULT_TTS_ENABLED
    }

    val customSoundUri: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_SOUND_URI]
    }

    val workScheduleEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_WORK_SCHEDULE_ENABLED] ?: DEFAULT_WORK_SCHEDULE_ENABLED
    }

    val autoModeByDayEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_MODE_BY_DAY] ?: DEFAULT_AUTO_MODE_BY_DAY
    }

    val weekSchedule: Flow<List<DaySchedule>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_WEEK_SCHEDULE]
        if (raw != null) {
            try {
                json.decodeFromString<List<DaySchedule>>(raw)
            } catch (e: Exception) {
                Log.w("SettingsRepository", "Failed to decode week schedule", e)
                DEFAULT_WEEK_SCHEDULE
            }
        } else {
            migrateOldScheduleKeys(prefs)
        }
    }

    private fun migrateOldScheduleKeys(prefs: Preferences): List<DaySchedule> {
        val hasOldKeys = prefs[KEY_WORK_START_HOUR] != null
        if (!hasOldKeys) return DEFAULT_WEEK_SCHEDULE

        val baseDay = DaySchedule(
            enabled = true,
            linked = false,
            workStartHour = prefs[KEY_WORK_START_HOUR] ?: DEFAULT_WORK_START_HOUR,
            workStartMinute = prefs[KEY_WORK_START_MINUTE] ?: DEFAULT_WORK_START_MINUTE,
            workEndHour = prefs[KEY_WORK_END_HOUR] ?: DEFAULT_WORK_END_HOUR,
            workEndMinute = prefs[KEY_WORK_END_MINUTE] ?: DEFAULT_WORK_END_MINUTE,
            lunchStartHour = prefs[KEY_LUNCH_START_HOUR] ?: DEFAULT_LUNCH_START_HOUR,
            lunchStartMinute = prefs[KEY_LUNCH_START_MINUTE] ?: DEFAULT_LUNCH_START_MINUTE,
            lunchEndHour = prefs[KEY_LUNCH_END_HOUR] ?: DEFAULT_LUNCH_END_HOUR,
            lunchEndMinute = prefs[KEY_LUNCH_END_MINUTE] ?: DEFAULT_LUNCH_END_MINUTE,
        )
        val linkedDay = baseDay.copy(linked = true)
        val offDay = DaySchedule(enabled = false, linked = false)
        return listOf(baseDay, linkedDay, linkedDay, linkedDay, linkedDay, offDay, offDay)
    }

    val dynamicIncreaseEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_INCREASE_ENABLED] ?: DEFAULT_DYNAMIC_INCREASE_ENABLED
    }

    val breaksSinceLastIncrease: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_BREAKS_SINCE_LAST_INCREASE] ?: DEFAULT_BREAKS_SINCE_LAST_INCREASE
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: (prefs[KEY_TIMER_HOURS] != null)
    }

    val usedExerciseNames: Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_USED_EXERCISE_NAMES]
        if (raw != null) {
            try {
                json.decodeFromString<List<String>>(raw).toSet()
            } catch (e: Exception) {
                Log.w("SettingsRepository", "Failed to decode used exercise names", e)
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    val lastPickedName: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_PICKED_NAME]
    }

    suspend fun setTimerHours(hours: Int) {
        dataStore.edit { it[KEY_TIMER_HOURS] = hours }
    }

    suspend fun setTimerMinutes(minutes: Int) {
        dataStore.edit { it[KEY_TIMER_MINUTES] = minutes }
    }

    suspend fun setRepsMin(value: Int) {
        dataStore.edit { it[KEY_REPS_MIN] = value }
    }

    suspend fun setRepsMax(value: Int) {
        dataStore.edit { it[KEY_REPS_MAX] = value }
    }

    suspend fun setRepsLinked(value: Boolean) {
        dataStore.edit { it[KEY_REPS_LINKED] = value }
    }

    suspend fun setExerciseMode(mode: ExerciseMode) {
        dataStore.edit { it[KEY_EXERCISE_MODE] = mode.name }
    }

    suspend fun setExercises(exercises: List<Exercise>) {
        val mode = dataStore.data.first()[KEY_EXERCISE_MODE]
            ?.let {
                try {
                    ExerciseMode.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    ExerciseMode.HOME_WORKOUT
                }
            }
            ?: ExerciseMode.HOME_WORKOUT
        dataStore.edit { it[exercisesKey(mode)] = json.encodeToString(exercises) }
    }

    suspend fun setExercisesForMode(mode: ExerciseMode, exercises: List<Exercise>) {
        dataStore.edit { it[exercisesKey(mode)] = json.encodeToString(exercises) }
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setBeepVolume(value: Int) {
        dataStore.edit { it[KEY_BEEP_VOLUME] = value }
    }

    suspend fun setVibrationEnabled(value: Boolean) {
        dataStore.edit { it[KEY_VIBRATION_ENABLED] = value }
    }

    suspend fun setThemeMode(value: String) {
        dataStore.edit { it[KEY_THEME_MODE] = value }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        dataStore.edit { it[KEY_KEEP_SCREEN_ON] = value }
    }

    suspend fun setAutoRestart(value: Boolean) {
        dataStore.edit { it[KEY_AUTO_RESTART] = value }
    }

    suspend fun setBeepCount(value: Int) {
        dataStore.edit { it[KEY_BEEP_COUNT] = value }
    }

    suspend fun setTtsEnabled(value: Boolean) {
        dataStore.edit { it[KEY_TTS_ENABLED] = value }
    }

    suspend fun setWorkScheduleEnabled(value: Boolean) {
        dataStore.edit { it[KEY_WORK_SCHEDULE_ENABLED] = value }
    }

    suspend fun setWeekSchedule(schedule: List<DaySchedule>) {
        dataStore.edit { it[KEY_WEEK_SCHEDULE] = json.encodeToString(schedule) }
    }

    suspend fun setAutoModeByDayEnabled(enabled: Boolean, seedMode: ExerciseMode? = null) {
        dataStore.edit { prefs ->
            val wasEnabled = prefs[KEY_AUTO_MODE_BY_DAY] ?: DEFAULT_AUTO_MODE_BY_DAY
            prefs[KEY_AUTO_MODE_BY_DAY] = enabled
            if (enabled && !wasEnabled && seedMode != null) {
                val existing = prefs[KEY_WEEK_SCHEDULE]?.let { raw ->
                    try {
                        json.decodeFromString<List<DaySchedule>>(raw)
                    } catch (_: Exception) {
                        null
                    }
                } ?: DEFAULT_WEEK_SCHEDULE
                val seeded = existing.map { it.copy(defaultMode = seedMode) }
                prefs[KEY_WEEK_SCHEDULE] = json.encodeToString(seeded)
            }
        }
    }

    suspend fun setCustomSoundUri(uri: String?) {
        dataStore.edit {
            if (uri != null) {
                it[KEY_CUSTOM_SOUND_URI] = uri
            } else {
                it.remove(KEY_CUSTOM_SOUND_URI)
            }
        }
    }

    suspend fun setDynamicIncreaseEnabled(value: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_INCREASE_ENABLED] = value }
    }

    suspend fun setBreaksSinceLastIncrease(value: Int) {
        dataStore.edit { it[KEY_BREAKS_SINCE_LAST_INCREASE] = value }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setUsedExerciseNames(names: Set<String>) {
        dataStore.edit { it[KEY_USED_EXERCISE_NAMES] = json.encodeToString(names.toList()) }
    }

    suspend fun setLastPickedName(name: String?) {
        dataStore.edit {
            if (name != null) {
                it[KEY_LAST_PICKED_NAME] = name
            } else {
                it.remove(KEY_LAST_PICKED_NAME)
            }
        }
    }

    suspend fun snapshotForExport(): SettingsExportSnapshot {
        val prefs = dataStore.data.first()
        val activeMode = prefs[KEY_EXERCISE_MODE]
            ?.let {
                try {
                    ExerciseMode.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    ExerciseMode.HOME_WORKOUT
                }
            }
            ?: ExerciseMode.HOME_WORKOUT
        val activeExercises = exercises.first()

        return SettingsExportSnapshot(
            timerHours = prefs[KEY_TIMER_HOURS] ?: DEFAULT_HOURS,
            timerMinutes = prefs[KEY_TIMER_MINUTES] ?: DEFAULT_MINUTES,
            repsMin = prefs[KEY_REPS_MIN] ?: DEFAULT_REPS_MIN,
            repsMax = prefs[KEY_REPS_MAX] ?: DEFAULT_REPS_MAX,
            repsLinked = prefs[KEY_REPS_LINKED] ?: DEFAULT_REPS_LINKED,
            exercises = activeExercises,
            exerciseMode = activeMode,
            exercisesHomeWorkout = exercisesForMode(ExerciseMode.HOME_WORKOUT).first(),
            exercisesHomeMobility = exercisesForMode(ExerciseMode.HOME_MOBILITY).first(),
            exercisesOffice = exercisesForMode(ExerciseMode.OFFICE).first(),
            language = prefs[KEY_LANGUAGE] ?: LANGUAGE_SYSTEM,
            themeMode = prefs[KEY_THEME_MODE] ?: THEME_SYSTEM,
            beepVolume = prefs[KEY_BEEP_VOLUME] ?: DEFAULT_BEEP_VOLUME,
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: DEFAULT_VIBRATION_ENABLED,
            beepCount = prefs[KEY_BEEP_COUNT] ?: DEFAULT_BEEP_COUNT,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: DEFAULT_KEEP_SCREEN_ON,
            autoRestart = prefs[KEY_AUTO_RESTART] ?: DEFAULT_AUTO_RESTART,
            dynamicIncreaseEnabled = prefs[KEY_DYNAMIC_INCREASE_ENABLED] ?: DEFAULT_DYNAMIC_INCREASE_ENABLED,
            breaksSinceLastIncrease = prefs[KEY_BREAKS_SINCE_LAST_INCREASE] ?: DEFAULT_BREAKS_SINCE_LAST_INCREASE,
            ttsEnabled = prefs[KEY_TTS_ENABLED] ?: DEFAULT_TTS_ENABLED,
            customSoundUri = prefs[KEY_CUSTOM_SOUND_URI],
            workScheduleEnabled = prefs[KEY_WORK_SCHEDULE_ENABLED] ?: DEFAULT_WORK_SCHEDULE_ENABLED,
            weekSchedule = weekSchedule.first(),
            autoModeByDayEnabled = prefs[KEY_AUTO_MODE_BY_DAY] ?: DEFAULT_AUTO_MODE_BY_DAY,
        )
    }

    suspend fun restoreFromBackup(data: BackupData) {
        dataStore.edit { prefs ->
            prefs[KEY_TIMER_HOURS] = data.timerHours.coerceIn(0, 23)
            prefs[KEY_TIMER_MINUTES] = data.timerMinutes.coerceIn(0, 59)
            prefs[KEY_REPS_MIN] = data.repsMin.coerceIn(0, 999)
            prefs[KEY_REPS_MAX] = data.repsMax.coerceIn(0, 999)
            prefs[KEY_REPS_LINKED] = data.repsLinked
            prefs[KEY_LANGUAGE] = data.language
            prefs[KEY_THEME_MODE] = data.themeMode
            prefs[KEY_BEEP_VOLUME] = data.beepVolume.coerceIn(0, 100)
            prefs[KEY_VIBRATION_ENABLED] = data.vibrationEnabled
            prefs[KEY_BEEP_COUNT] = data.beepCount.coerceIn(1, 10)
            prefs[KEY_KEEP_SCREEN_ON] = data.keepScreenOn
            prefs[KEY_AUTO_RESTART] = data.autoRestart
            prefs[KEY_DYNAMIC_INCREASE_ENABLED] = data.dynamicIncreaseEnabled
            prefs[KEY_BREAKS_SINCE_LAST_INCREASE] = data.breaksSinceLastIncrease.coerceIn(0, 10_000)
            prefs[KEY_TTS_ENABLED] = data.ttsEnabled
            // customSoundUri is device-specific, not restored from backup
            prefs[KEY_WORK_SCHEDULE_ENABLED] = data.workScheduleEnabled
            prefs[KEY_AUTO_MODE_BY_DAY] = data.autoModeByDayEnabled
            if (data.weekSchedule.isNotEmpty()) {
                val clamped = data.weekSchedule.take(7).map { it.clamp() }
                prefs[KEY_WEEK_SCHEDULE] = json.encodeToString(clamped)
            } else {
                // Migrate from old flat fields
                val baseDay = DaySchedule(
                    enabled = true, linked = false,
                    workStartHour = data.workStartHour.coerceIn(0, 23),
                    workStartMinute = data.workStartMinute.coerceIn(0, 59),
                    workEndHour = data.workEndHour.coerceIn(0, 23),
                    workEndMinute = data.workEndMinute.coerceIn(0, 59),
                    lunchStartHour = data.lunchStartHour.coerceIn(0, 23),
                    lunchStartMinute = data.lunchStartMinute.coerceIn(0, 59),
                    lunchEndHour = data.lunchEndHour.coerceIn(0, 23),
                    lunchEndMinute = data.lunchEndMinute.coerceIn(0, 59),
                )
                val linkedDay = baseDay.copy(linked = true)
                val offDay = DaySchedule(enabled = false, linked = false)
                prefs[KEY_WEEK_SCHEDULE] = json.encodeToString(
                    listOf(baseDay, linkedDay, linkedDay, linkedDay, linkedDay, offDay, offDay),
                )
            }

            // Restore exercise mode data
            prefs[KEY_EXERCISE_MODE] = data.exerciseMode
            if (data.exercisesHomeWorkout.isNotEmpty()) {
                // v2 backup: restore per-mode exercise lists
                prefs[KEY_EXERCISES_HOME_WORKOUT] = json.encodeToString(data.exercisesHomeWorkout)
                prefs[KEY_EXERCISES_HOME_MOBILITY] = json.encodeToString(data.exercisesHomeMobility)
                prefs[KEY_EXERCISES_OFFICE] = json.encodeToString(data.exercisesOffice)
            } else {
                // v1 backup: map exercises to HOME_WORKOUT, others get defaults
                prefs[KEY_EXERCISES_HOME_WORKOUT] = json.encodeToString(data.exercises)
            }
            // Keep legacy key populated for safety
            prefs[KEY_EXERCISES] = json.encodeToString(data.exercises)

            prefs[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    data class SettingsExportSnapshot(
        val timerHours: Int,
        val timerMinutes: Int,
        val repsMin: Int,
        val repsMax: Int,
        val repsLinked: Boolean,
        val exercises: List<Exercise>,
        val exerciseMode: ExerciseMode,
        val exercisesHomeWorkout: List<Exercise>,
        val exercisesHomeMobility: List<Exercise>,
        val exercisesOffice: List<Exercise>,
        val language: String,
        val themeMode: String,
        val beepVolume: Int,
        val vibrationEnabled: Boolean,
        val beepCount: Int,
        val keepScreenOn: Boolean,
        val autoRestart: Boolean,
        val dynamicIncreaseEnabled: Boolean,
        val breaksSinceLastIncrease: Int,
        val ttsEnabled: Boolean,
        val customSoundUri: String?,
        val workScheduleEnabled: Boolean,
        val weekSchedule: List<DaySchedule>,
        val autoModeByDayEnabled: Boolean,
    )

    companion object {
        internal val KEY_TIMER_HOURS = intPreferencesKey("timer_hours")
        internal val KEY_TIMER_MINUTES = intPreferencesKey("timer_minutes")
        internal val KEY_REPS_MIN = intPreferencesKey("reps_min")
        internal val KEY_REPS_MAX = intPreferencesKey("reps_max")
        internal val KEY_REPS_LINKED = booleanPreferencesKey("reps_linked")
        internal val KEY_EXERCISES = stringPreferencesKey("exercises")
        internal val KEY_EXERCISE_MODE = stringPreferencesKey("exercise_mode")
        internal val KEY_EXERCISES_HOME_WORKOUT = stringPreferencesKey("exercises_home_workout")
        internal val KEY_EXERCISES_HOME_MOBILITY = stringPreferencesKey("exercises_home_mobility")
        internal val KEY_EXERCISES_OFFICE = stringPreferencesKey("exercises_office")
        internal val KEY_LANGUAGE = stringPreferencesKey("language")
        internal val KEY_BEEP_VOLUME = intPreferencesKey("beep_volume")
        internal val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        internal val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        internal val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        internal val KEY_AUTO_RESTART = booleanPreferencesKey("auto_restart")
        internal val KEY_BEEP_COUNT = intPreferencesKey("beep_count")
        internal val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        internal val KEY_CUSTOM_SOUND_URI = stringPreferencesKey("custom_sound_uri")
        internal val KEY_WORK_SCHEDULE_ENABLED = booleanPreferencesKey("work_schedule_enabled")
        internal val KEY_WEEK_SCHEDULE = stringPreferencesKey("week_schedule")
        internal val KEY_AUTO_MODE_BY_DAY = booleanPreferencesKey("auto_mode_by_day_enabled")
        // Legacy keys for migration from old flat format
        internal val KEY_WORK_START_HOUR = intPreferencesKey("work_start_hour")
        internal val KEY_WORK_START_MINUTE = intPreferencesKey("work_start_minute")
        internal val KEY_WORK_END_HOUR = intPreferencesKey("work_end_hour")
        internal val KEY_WORK_END_MINUTE = intPreferencesKey("work_end_minute")
        internal val KEY_LUNCH_START_HOUR = intPreferencesKey("lunch_start_hour")
        internal val KEY_LUNCH_START_MINUTE = intPreferencesKey("lunch_start_minute")
        internal val KEY_LUNCH_END_HOUR = intPreferencesKey("lunch_end_hour")
        internal val KEY_LUNCH_END_MINUTE = intPreferencesKey("lunch_end_minute")
        internal val KEY_DYNAMIC_INCREASE_ENABLED = booleanPreferencesKey("dynamic_increase_enabled")
        internal val KEY_BREAKS_SINCE_LAST_INCREASE = intPreferencesKey("breaks_since_last_increase")
        internal val KEY_USED_EXERCISE_NAMES = stringPreferencesKey("used_exercise_names")
        internal val KEY_LAST_PICKED_NAME = stringPreferencesKey("last_picked_name")
        internal val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        const val DEFAULT_HOURS = 0
        const val DEFAULT_MINUTES = 30
        const val DEFAULT_REPS_MIN = 10
        const val DEFAULT_REPS_MAX = 10
        const val DEFAULT_REPS_LINKED = true

        const val DEFAULT_BEEP_VOLUME = 80
        const val DEFAULT_VIBRATION_ENABLED = true

        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_DE = "de"
        const val LANGUAGE_EN = "en"

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        const val DEFAULT_KEEP_SCREEN_ON = false
        const val DEFAULT_AUTO_RESTART = true
        const val DEFAULT_BEEP_COUNT = 3

        const val DEFAULT_TTS_ENABLED = false

        const val DEFAULT_WORK_SCHEDULE_ENABLED = false
        const val DEFAULT_AUTO_MODE_BY_DAY = false
        const val DEFAULT_WORK_START_HOUR = 8
        const val DEFAULT_WORK_START_MINUTE = 0
        const val DEFAULT_WORK_END_HOUR = 17
        const val DEFAULT_WORK_END_MINUTE = 0
        const val DEFAULT_LUNCH_START_HOUR = 12
        const val DEFAULT_LUNCH_START_MINUTE = 0
        const val DEFAULT_LUNCH_END_HOUR = 13
        const val DEFAULT_LUNCH_END_MINUTE = 0

        const val DEFAULT_DYNAMIC_INCREASE_ENABLED = true
        const val DEFAULT_BREAKS_SINCE_LAST_INCREASE = 0

        private val KNOWN_DEFAULT_NAMES: Map<String, String> = mapOf(
            // English — Home Workout
            "Push Ups" to "exercise_push_ups",
            "Squats" to "exercise_squats",
            "Deadlifts" to "exercise_deadlifts",
            "Lunges" to "exercise_lunges",
            "Sit Ups" to "exercise_sit_ups",
            "Superman Angels" to "exercise_superman_angels",
            "Plank" to "exercise_plank",
            "Glute Bridge" to "exercise_glute_bridge",
            // English — Home Mobility
            "Cat-Cow Stretch" to "exercise_cat_cow",
            "Child's Pose" to "exercise_childs_pose",
            "Downward Dog" to "exercise_downward_dog",
            "Seated Spinal Twist" to "exercise_seated_twist",
            "Hip Circles" to "exercise_hip_circles",
            "Standing Forward Fold" to "exercise_standing_forward_fold",
            "Thread the Needle" to "exercise_thread_the_needle",
            "Pigeon Stretch" to "exercise_pigeon_stretch",
            // English — Office
            "Shoulder Blade Squeeze" to "exercise_shoulder_blade_squeeze",
            "Chest Opener" to "exercise_chest_opener",
            "Neck Stretch" to "exercise_neck_stretch",
            "Calf Raises" to "exercise_calf_raises",
            "Seated Leg Extension" to "exercise_seated_leg_extension",
            "Wrist Circles" to "exercise_wrist_circles",
            "Ankle Circles" to "exercise_ankle_circles",
            "Seated Cat-Cow" to "exercise_seated_cat_cow",
            "Seated Core Bracing" to "exercise_seated_core_bracing",
            // German — Home Workout
            "Liegestütze" to "exercise_push_ups",
            "Kniebeugen" to "exercise_squats",
            "Kniebeuge" to "exercise_squats",
            "Kreuzheben" to "exercise_deadlifts",
            "Ausfallschritte" to "exercise_lunges",
            "Ausfallschritt" to "exercise_lunges",
            "Hüftheben" to "exercise_glute_bridge",
            // German — Home Mobility
            "Katze-Kuh" to "exercise_cat_cow",
            "Kindhaltung" to "exercise_childs_pose",
            "Herabschauender Hund" to "exercise_downward_dog",
            "Rumpfdrehung" to "exercise_seated_twist",
            "Hüftkreise" to "exercise_hip_circles",
            "Stehende Vorbeuge" to "exercise_standing_forward_fold",
            // German — Office
            "Schulterblätter zusammenziehen" to "exercise_shoulder_blade_squeeze",
            "Brustöffner" to "exercise_chest_opener",
            "Nacken dehnen" to "exercise_neck_stretch",
            "Wadenheben" to "exercise_calf_raises",
            "Sitzende Beinstreckung" to "exercise_seated_leg_extension",
            "Handgelenke kreisen" to "exercise_wrist_circles",
            "Fußgelenke kreisen" to "exercise_ankle_circles",
            "Katze-Kuh im Sitzen" to "exercise_seated_cat_cow",
            "Bauchspannung halten" to "exercise_seated_core_bracing",
        )

        private fun exercisesKey(mode: ExerciseMode): Preferences.Key<String> = when (mode) {
            ExerciseMode.HOME_WORKOUT -> KEY_EXERCISES_HOME_WORKOUT
            ExerciseMode.HOME_MOBILITY -> KEY_EXERCISES_HOME_MOBILITY
            ExerciseMode.OFFICE -> KEY_EXERCISES_OFFICE
        }
    }
}
