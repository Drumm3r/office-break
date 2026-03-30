package de.mysportsmate.officebreak.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val Context.statsDataStore: DataStore<Preferences> by preferencesDataStore(name = "stats")

class StatsRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    constructor(context: Context) : this(dataStore = context.statsDataStore)

    private val json = Json { ignoreUnknownKeys = true }

    val trackingEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_TRACKING_ENABLED] ?: DEFAULT_TRACKING_ENABLED
    }

    val statsSnapshot: Flow<StatsSnapshot> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_STATS_SNAPSHOT]
        if (raw != null) {
            try {
                json.decodeFromString<StatsSnapshot>(raw)
            } catch (_: Exception) {
                StatsSnapshot()
            }
        } else {
            StatsSnapshot()
        }
    }

    val achievementState: Flow<AchievementState> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_ACHIEVEMENT_STATE]
        if (raw != null) {
            try {
                json.decodeFromString<AchievementState>(raw)
            } catch (_: Exception) {
                AchievementState()
            }
        } else {
            AchievementState()
        }
    }

    val breakRecords: Flow<List<BreakRecord>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_BREAK_RECORDS]
        if (raw != null) {
            try {
                json.decodeFromString<List<BreakRecord>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val dailyAggregates: Flow<List<DailyAggregate>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_DAILY_AGGREGATES]
        if (raw != null) {
            try {
                json.decodeFromString<List<DailyAggregate>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val yearlyAggregates: Flow<List<YearlyAggregate>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_YEARLY_AGGREGATES]
        if (raw != null) {
            try {
                json.decodeFromString<List<YearlyAggregate>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun recordBreak(record: BreakRecord): List<AchievementDefinition> {
        var newlyUnlocked = emptyList<AchievementDefinition>()

        dataStore.edit { prefs ->
            val records = decodeList<BreakRecord>(prefs[KEY_BREAK_RECORDS]).toMutableList()
            records.add(record)

            val snapshot = decodeOrDefault(prefs[KEY_STATS_SNAPSHOT], StatsSnapshot())
            val updatedSnapshot = updateSnapshot(snapshot, record)

            val todayRecords = records.filter { it.dateString == record.dateString }
            val uniqueExercisesToday = todayRecords.map { it.exerciseName }.toSet().size

            val daysSinceLastBreak = snapshot.lastBreakDateString?.let {
                try {
                    ChronoUnit.DAYS.between(LocalDate.parse(it), LocalDate.parse(record.dateString)).toInt()
                } catch (_: Exception) {
                    null
                }
            }

            val dateObj = try {
                LocalDate.parse(record.dateString)
            } catch (_: Exception) {
                LocalDate.now(clock)
            }

            val breakContext = BreakContext(
                exerciseName = record.exerciseName,
                reps = record.reps,
                timestampMillis = record.timestampMillis,
                hourOfDay = java.time.Instant.ofEpochMilli(record.timestampMillis)
                    .atZone(clock.zone).hour,
                dayOfWeek = dateObj.dayOfWeek.value,
                dateString = record.dateString,
                breaksToday = todayRecords.size,
                uniqueExercisesToday = uniqueExercisesToday,
                daysSinceLastBreak = daysSinceLastBreak,
                isNewYear = dateObj.monthValue == 1 && dateObj.dayOfMonth == 1,
            )

            val achieveState = decodeOrDefault(prefs[KEY_ACHIEVEMENT_STATE], AchievementState())
            newlyUnlocked = AchievementEngine.evaluate(updatedSnapshot, breakContext, achieveState.unlockedIds)

            val updatedAchieveState = if (newlyUnlocked.isNotEmpty()) {
                achieveState.copy(
                    unlockedIds = achieveState.unlockedIds + newlyUnlocked.map { it.id }.toSet(),
                    unlockTimestamps = achieveState.unlockTimestamps +
                        newlyUnlocked.associate { it.id to record.timestampMillis },
                )
            } else {
                achieveState
            }

            // Daily compaction
            val cutoffDate = LocalDate.now(clock).minusDays(ROLLING_WINDOW_DAYS.toLong())
            val (oldRecords, recentRecords) = records.partition {
                try {
                    LocalDate.parse(it.dateString).isBefore(cutoffDate)
                } catch (_: Exception) {
                    false
                }
            }

            val aggregates = decodeList<DailyAggregate>(prefs[KEY_DAILY_AGGREGATES]).toMutableList()
            if (oldRecords.isNotEmpty()) {
                mergeRecordsIntoAggregates(oldRecords, aggregates)
            }

            prefs[KEY_BREAK_RECORDS] = json.encodeToString(recentRecords)
            prefs[KEY_DAILY_AGGREGATES] = json.encodeToString(aggregates.toList())
            prefs[KEY_STATS_SNAPSHOT] = json.encodeToString(updatedSnapshot)
            prefs[KEY_ACHIEVEMENT_STATE] = json.encodeToString(updatedAchieveState)
        }

        return newlyUnlocked
    }

    suspend fun runYearlyCompaction() {
        val today = LocalDate.now(clock)
        if (today.monthValue < 4) return

        val cutoffYear = today.year - 1

        dataStore.edit { prefs ->
            val aggregates = decodeList<DailyAggregate>(prefs[KEY_DAILY_AGGREGATES]).toMutableList()
            val yearlyAggs = decodeList<YearlyAggregate>(prefs[KEY_YEARLY_AGGREGATES]).toMutableList()

            val existingYears = yearlyAggs.map { it.year }.toSet()

            val toCompact = aggregates.filter {
                try {
                    val year = LocalDate.parse(it.dateString).year
                    year < cutoffYear && year !in existingYears
                } catch (_: Exception) {
                    false
                }
            }

            if (toCompact.isEmpty()) return@edit

            val byYear = toCompact.groupBy { LocalDate.parse(it.dateString).year }
            for ((year, dailies) in byYear) {
                yearlyAggs.add(
                    YearlyAggregate(
                        year = year,
                        totalBreaks = dailies.sumOf { it.totalBreaks },
                        totalReps = dailies.sumOf { it.totalReps },
                        exerciseCounts = mergeMaps(dailies.map { it.exerciseCounts }),
                        exerciseReps = mergeMaps(dailies.map { it.exerciseReps }),
                        activeDays = dailies.size,
                    ),
                )
            }

            val compactedDates = toCompact.map { it.dateString }.toSet()
            aggregates.removeAll { it.dateString in compactedDates }

            prefs[KEY_DAILY_AGGREGATES] = json.encodeToString(aggregates.toList())
            prefs[KEY_YEARLY_AGGREGATES] = json.encodeToString(yearlyAggs.toList())
        }
    }

    suspend fun markCustomExerciseCreated() {
        dataStore.edit { prefs ->
            val snapshot = decodeOrDefault(prefs[KEY_STATS_SNAPSHOT], StatsSnapshot())
            if (!snapshot.hasCreatedCustomExercise) {
                prefs[KEY_STATS_SNAPSHOT] = json.encodeToString(
                    snapshot.copy(hasCreatedCustomExercise = true),
                )
            }
        }
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_TRACKING_ENABLED] = enabled }
    }

    suspend fun resetAllStats() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_BREAK_RECORDS)
            prefs.remove(KEY_DAILY_AGGREGATES)
            prefs.remove(KEY_YEARLY_AGGREGATES)
            prefs.remove(KEY_STATS_SNAPSHOT)
            prefs.remove(KEY_ACHIEVEMENT_STATE)
        }
    }

    private fun updateSnapshot(snapshot: StatsSnapshot, record: BreakRecord): StatsSnapshot {
        val today = record.dateString
        val newStreak = when {
            snapshot.lastBreakDateString == today -> snapshot.currentStreakDays
            snapshot.lastBreakDateString != null && isYesterday(snapshot.lastBreakDateString, today) ->
                snapshot.currentStreakDays + 1
            else -> 1
        }

        return snapshot.copy(
            totalBreaksAllTime = snapshot.totalBreaksAllTime + 1,
            totalRepsAllTime = snapshot.totalRepsAllTime + record.reps,
            currentStreakDays = newStreak,
            longestStreakDays = maxOf(snapshot.longestStreakDays, newStreak),
            lastBreakDateString = today,
            firstBreakDateString = snapshot.firstBreakDateString ?: today,
            perExerciseCounts = snapshot.perExerciseCounts.toMutableMap().apply {
                this[record.exerciseName] = (this[record.exerciseName] ?: 0) + 1
            },
            perExerciseReps = snapshot.perExerciseReps.toMutableMap().apply {
                this[record.exerciseName] = (this[record.exerciseName] ?: 0) + record.reps
            },
            uniqueExercisesUsed = snapshot.uniqueExercisesUsed + record.exerciseName,
        )
    }

    private fun isYesterday(previous: String, current: String): Boolean {
        return try {
            val prev = LocalDate.parse(previous)
            val curr = LocalDate.parse(current)
            prev.plusDays(1) == curr
        } catch (_: Exception) {
            false
        }
    }

    private fun mergeRecordsIntoAggregates(records: List<BreakRecord>, aggregates: MutableList<DailyAggregate>) {
        val byDate = records.groupBy { it.dateString }
        for ((date, dayRecords) in byDate) {
            val existing = aggregates.find { it.dateString == date }
            if (existing != null) {
                val index = aggregates.indexOf(existing)
                aggregates[index] = existing.copy(
                    totalBreaks = existing.totalBreaks + dayRecords.size,
                    totalReps = existing.totalReps + dayRecords.sumOf { it.reps },
                    exerciseCounts = mergeTwoMaps(
                        existing.exerciseCounts,
                        dayRecords.groupBy { it.exerciseName }.mapValues { it.value.size },
                    ),
                    exerciseReps = mergeTwoMaps(
                        existing.exerciseReps,
                        dayRecords.groupBy { it.exerciseName }.mapValues { e -> e.value.sumOf { it.reps } },
                    ),
                )
            } else {
                aggregates.add(
                    DailyAggregate(
                        dateString = date,
                        totalBreaks = dayRecords.size,
                        totalReps = dayRecords.sumOf { it.reps },
                        exerciseCounts = dayRecords.groupBy { it.exerciseName }.mapValues { it.value.size },
                        exerciseReps = dayRecords.groupBy { it.exerciseName }
                            .mapValues { e -> e.value.sumOf { it.reps } },
                    ),
                )
            }
        }
    }

    private fun mergeMaps(maps: List<Map<String, Int>>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        for (map in maps) {
            for ((key, value) in map) {
                result[key] = (result[key] ?: 0) + value
            }
        }
        return result
    }

    private fun mergeTwoMaps(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> {
        val result = a.toMutableMap()
        for ((key, value) in b) {
            result[key] = (result[key] ?: 0) + value
        }
        return result
    }

    private inline fun <reified T> decodeList(raw: String?): List<T> {
        if (raw == null) return emptyList()
        return try {
            json.decodeFromString<List<T>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private inline fun <reified T> decodeOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return try {
            json.decodeFromString<T>(raw)
        } catch (_: Exception) {
            default
        }
    }

    companion object {
        private val KEY_BREAK_RECORDS = stringPreferencesKey("break_records")
        private val KEY_DAILY_AGGREGATES = stringPreferencesKey("daily_aggregates")
        private val KEY_YEARLY_AGGREGATES = stringPreferencesKey("yearly_aggregates")
        private val KEY_STATS_SNAPSHOT = stringPreferencesKey("stats_snapshot")
        private val KEY_ACHIEVEMENT_STATE = stringPreferencesKey("achievement_state")
        private val KEY_TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")

        const val ROLLING_WINDOW_DAYS = 90
        const val DEFAULT_TRACKING_ENABLED = true
    }
}
