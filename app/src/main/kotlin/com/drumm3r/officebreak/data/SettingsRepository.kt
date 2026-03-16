package com.drumm3r.officebreak.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val defaultExercises: List<Exercise> = emptyList(),
) {

    constructor(context: Context) : this(
        dataStore = context.dataStore,
        defaultExercises = ExerciseConfig.defaultExercises(context),
    )

    private val json = Json { ignoreUnknownKeys = true }

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
        if (raw != null) {
            try {
                json.decodeFromString<List<Exercise>>(raw)
            } catch (_: Exception) {
                defaultExercises
            }
        } else {
            defaultExercises
        }
    }

    val language: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: LANGUAGE_SYSTEM
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

    companion object {
        private val KEY_TIMER_HOURS = intPreferencesKey("timer_hours")
        private val KEY_TIMER_MINUTES = intPreferencesKey("timer_minutes")
        private val KEY_REPS_MIN = intPreferencesKey("reps_min")
        private val KEY_REPS_MAX = intPreferencesKey("reps_max")
        private val KEY_REPS_LINKED = booleanPreferencesKey("reps_linked")
        private val KEY_EXERCISES = stringPreferencesKey("exercises")
        private val KEY_LANGUAGE = stringPreferencesKey("language")

        const val DEFAULT_HOURS = 0
        const val DEFAULT_MINUTES = 30
        const val DEFAULT_REPS_MIN = 10
        const val DEFAULT_REPS_MAX = 10
        const val DEFAULT_REPS_LINKED = true

        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_DE = "de"
        const val LANGUAGE_EN = "en"
    }
}
