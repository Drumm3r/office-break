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
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import android.util.Log

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val defaultExercises: List<Exercise> = emptyList(),
) {

    constructor(context: Context) : this(
        dataStore = context.dataStore,
        defaultExercises = ExerciseConfig.defaultExercises(context),
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

    val exercises: Flow<List<Exercise>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_EXERCISES]
        val list = if (raw != null) {
            try {
                json.decodeFromString<List<Exercise>>(raw)
            } catch (e: Exception) {
                Log.w("SettingsRepository", "Failed to decode exercises, using defaults", e)
                defaultExercises
            }
        } else {
            defaultExercises
        }

        list.map { exercise ->
            if (exercise.nameResKey == null) {
                val key = KNOWN_DEFAULT_NAMES[exercise.name]
                if (key != null) exercise.copy(nameResKey = key) else exercise
            } else {
                exercise
            }
        }
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

    val workStartHour: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_WORK_START_HOUR] ?: DEFAULT_WORK_START_HOUR
    }

    val workStartMinute: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_WORK_START_MINUTE] ?: DEFAULT_WORK_START_MINUTE
    }

    val workEndHour: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_WORK_END_HOUR] ?: DEFAULT_WORK_END_HOUR
    }

    val workEndMinute: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_WORK_END_MINUTE] ?: DEFAULT_WORK_END_MINUTE
    }

    val lunchStartHour: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_LUNCH_START_HOUR] ?: DEFAULT_LUNCH_START_HOUR
    }

    val lunchStartMinute: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_LUNCH_START_MINUTE] ?: DEFAULT_LUNCH_START_MINUTE
    }

    val lunchEndHour: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_LUNCH_END_HOUR] ?: DEFAULT_LUNCH_END_HOUR
    }

    val lunchEndMinute: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_LUNCH_END_MINUTE] ?: DEFAULT_LUNCH_END_MINUTE
    }

    val dynamicIncreaseEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_INCREASE_ENABLED] ?: DEFAULT_DYNAMIC_INCREASE_ENABLED
    }

    val breaksSinceLastIncrease: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_BREAKS_SINCE_LAST_INCREASE] ?: DEFAULT_BREAKS_SINCE_LAST_INCREASE
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: prefs.asMap().isNotEmpty()
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

    suspend fun setExercises(exercises: List<Exercise>) {
        dataStore.edit { it[KEY_EXERCISES] = json.encodeToString(exercises) }
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

    suspend fun setWorkStartHour(value: Int) {
        dataStore.edit { it[KEY_WORK_START_HOUR] = value }
    }

    suspend fun setWorkStartMinute(value: Int) {
        dataStore.edit { it[KEY_WORK_START_MINUTE] = value }
    }

    suspend fun setWorkEndHour(value: Int) {
        dataStore.edit { it[KEY_WORK_END_HOUR] = value }
    }

    suspend fun setWorkEndMinute(value: Int) {
        dataStore.edit { it[KEY_WORK_END_MINUTE] = value }
    }

    suspend fun setLunchStartHour(value: Int) {
        dataStore.edit { it[KEY_LUNCH_START_HOUR] = value }
    }

    suspend fun setLunchStartMinute(value: Int) {
        dataStore.edit { it[KEY_LUNCH_START_MINUTE] = value }
    }

    suspend fun setLunchEndHour(value: Int) {
        dataStore.edit { it[KEY_LUNCH_END_HOUR] = value }
    }

    suspend fun setLunchEndMinute(value: Int) {
        dataStore.edit { it[KEY_LUNCH_END_MINUTE] = value }
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

    suspend fun setWidgetTimerStatus(status: String) {
        dataStore.edit { it[KEY_WIDGET_TIMER_STATUS] = status }
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
        val exerciseList = exercises.first()

        return SettingsExportSnapshot(
            timerHours = prefs[KEY_TIMER_HOURS] ?: DEFAULT_HOURS,
            timerMinutes = prefs[KEY_TIMER_MINUTES] ?: DEFAULT_MINUTES,
            repsMin = prefs[KEY_REPS_MIN] ?: DEFAULT_REPS_MIN,
            repsMax = prefs[KEY_REPS_MAX] ?: DEFAULT_REPS_MAX,
            repsLinked = prefs[KEY_REPS_LINKED] ?: DEFAULT_REPS_LINKED,
            exercises = exerciseList,
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
            workStartHour = prefs[KEY_WORK_START_HOUR] ?: DEFAULT_WORK_START_HOUR,
            workStartMinute = prefs[KEY_WORK_START_MINUTE] ?: DEFAULT_WORK_START_MINUTE,
            workEndHour = prefs[KEY_WORK_END_HOUR] ?: DEFAULT_WORK_END_HOUR,
            workEndMinute = prefs[KEY_WORK_END_MINUTE] ?: DEFAULT_WORK_END_MINUTE,
            lunchStartHour = prefs[KEY_LUNCH_START_HOUR] ?: DEFAULT_LUNCH_START_HOUR,
            lunchStartMinute = prefs[KEY_LUNCH_START_MINUTE] ?: DEFAULT_LUNCH_START_MINUTE,
            lunchEndHour = prefs[KEY_LUNCH_END_HOUR] ?: DEFAULT_LUNCH_END_HOUR,
            lunchEndMinute = prefs[KEY_LUNCH_END_MINUTE] ?: DEFAULT_LUNCH_END_MINUTE,
        )
    }

    suspend fun restoreFromBackup(data: BackupData) {
        dataStore.edit { prefs ->
            prefs[KEY_TIMER_HOURS] = data.timerHours
            prefs[KEY_TIMER_MINUTES] = data.timerMinutes
            prefs[KEY_REPS_MIN] = data.repsMin
            prefs[KEY_REPS_MAX] = data.repsMax
            prefs[KEY_REPS_LINKED] = data.repsLinked
            prefs[KEY_EXERCISES] = json.encodeToString(data.exercises)
            prefs[KEY_LANGUAGE] = data.language
            prefs[KEY_THEME_MODE] = data.themeMode
            prefs[KEY_BEEP_VOLUME] = data.beepVolume
            prefs[KEY_VIBRATION_ENABLED] = data.vibrationEnabled
            prefs[KEY_BEEP_COUNT] = data.beepCount
            prefs[KEY_KEEP_SCREEN_ON] = data.keepScreenOn
            prefs[KEY_AUTO_RESTART] = data.autoRestart
            prefs[KEY_DYNAMIC_INCREASE_ENABLED] = data.dynamicIncreaseEnabled
            prefs[KEY_BREAKS_SINCE_LAST_INCREASE] = data.breaksSinceLastIncrease
            prefs[KEY_TTS_ENABLED] = data.ttsEnabled
            // customSoundUri is device-specific, not restored from backup
            prefs[KEY_WORK_SCHEDULE_ENABLED] = data.workScheduleEnabled
            prefs[KEY_WORK_START_HOUR] = data.workStartHour
            prefs[KEY_WORK_START_MINUTE] = data.workStartMinute
            prefs[KEY_WORK_END_HOUR] = data.workEndHour
            prefs[KEY_WORK_END_MINUTE] = data.workEndMinute
            prefs[KEY_LUNCH_START_HOUR] = data.lunchStartHour
            prefs[KEY_LUNCH_START_MINUTE] = data.lunchStartMinute
            prefs[KEY_LUNCH_END_HOUR] = data.lunchEndHour
            prefs[KEY_LUNCH_END_MINUTE] = data.lunchEndMinute
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
        val workStartHour: Int,
        val workStartMinute: Int,
        val workEndHour: Int,
        val workEndMinute: Int,
        val lunchStartHour: Int,
        val lunchStartMinute: Int,
        val lunchEndHour: Int,
        val lunchEndMinute: Int,
    )

    companion object {
        private val KEY_TIMER_HOURS = intPreferencesKey("timer_hours")
        private val KEY_TIMER_MINUTES = intPreferencesKey("timer_minutes")
        private val KEY_REPS_MIN = intPreferencesKey("reps_min")
        private val KEY_REPS_MAX = intPreferencesKey("reps_max")
        private val KEY_REPS_LINKED = booleanPreferencesKey("reps_linked")
        private val KEY_EXERCISES = stringPreferencesKey("exercises")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_BEEP_VOLUME = intPreferencesKey("beep_volume")
        private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_AUTO_RESTART = booleanPreferencesKey("auto_restart")
        private val KEY_BEEP_COUNT = intPreferencesKey("beep_count")
        private val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        private val KEY_CUSTOM_SOUND_URI = stringPreferencesKey("custom_sound_uri")
        private val KEY_WORK_SCHEDULE_ENABLED = booleanPreferencesKey("work_schedule_enabled")
        private val KEY_WORK_START_HOUR = intPreferencesKey("work_start_hour")
        private val KEY_WORK_START_MINUTE = intPreferencesKey("work_start_minute")
        private val KEY_WORK_END_HOUR = intPreferencesKey("work_end_hour")
        private val KEY_WORK_END_MINUTE = intPreferencesKey("work_end_minute")
        private val KEY_LUNCH_START_HOUR = intPreferencesKey("lunch_start_hour")
        private val KEY_LUNCH_START_MINUTE = intPreferencesKey("lunch_start_minute")
        private val KEY_LUNCH_END_HOUR = intPreferencesKey("lunch_end_hour")
        private val KEY_LUNCH_END_MINUTE = intPreferencesKey("lunch_end_minute")
        private val KEY_DYNAMIC_INCREASE_ENABLED = booleanPreferencesKey("dynamic_increase_enabled")
        private val KEY_BREAKS_SINCE_LAST_INCREASE = intPreferencesKey("breaks_since_last_increase")
        private val KEY_WIDGET_TIMER_STATUS = stringPreferencesKey("widget_timer_status")
        private val KEY_USED_EXERCISE_NAMES = stringPreferencesKey("used_exercise_names")
        private val KEY_LAST_PICKED_NAME = stringPreferencesKey("last_picked_name")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

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
            // English
            "Push Ups" to "exercise_push_ups",
            "Squats" to "exercise_squats",
            "Deadlifts" to "exercise_deadlifts",
            "Lunges" to "exercise_lunges",
            "Sit Ups" to "exercise_sit_ups",
            "Superman Angels" to "exercise_superman_angels",
            // German
            "Liegestütze" to "exercise_push_ups",
            "Kniebeuge" to "exercise_squats",
            "Kreuzheben" to "exercise_deadlifts",
            "Ausfallschritt" to "exercise_lunges",
        )
    }
}
