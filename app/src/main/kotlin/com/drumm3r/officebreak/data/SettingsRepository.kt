package com.drumm3r.officebreak.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    val reps: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_REPS] ?: DEFAULT_REPS
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

    suspend fun setReps(reps: Int) {
        dataStore.edit { it[KEY_REPS] = reps }
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
        private val KEY_REPS = intPreferencesKey("reps")
        private val KEY_EXERCISES = stringPreferencesKey("exercises")
        private val KEY_LANGUAGE = stringPreferencesKey("language")

        const val DEFAULT_HOURS = 0
        const val DEFAULT_MINUTES = 30
        const val DEFAULT_REPS = 10

        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_DE = "de"
        const val LANGUAGE_EN = "en"
    }
}
