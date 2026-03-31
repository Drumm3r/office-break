package de.mysportsmate.officebreak.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `BreakRecord roundtrip serialization`() {
        val record = BreakRecord(
            exerciseName = "Push Ups",
            reps = 10,
            timestampMillis = 1711800000000L,
            dateString = "2026-03-30",
        )
        val encoded = json.encodeToString(record)
        val decoded = json.decodeFromString<BreakRecord>(encoded)
        assertEquals(record, decoded)
    }

    @Test
    fun `DailyAggregate roundtrip serialization`() {
        val aggregate = DailyAggregate(
            dateString = "2026-03-30",
            totalBreaks = 5,
            totalReps = 60,
            exerciseCounts = mapOf("Push Ups" to 3, "Squats" to 2),
            exerciseReps = mapOf("Push Ups" to 30, "Squats" to 30),
        )
        val encoded = json.encodeToString(aggregate)
        val decoded = json.decodeFromString<DailyAggregate>(encoded)
        assertEquals(aggregate, decoded)
    }

    @Test
    fun `YearlyAggregate roundtrip serialization`() {
        val aggregate = YearlyAggregate(
            year = 2025,
            totalBreaks = 200,
            totalReps = 2400,
            exerciseCounts = mapOf("Push Ups" to 100, "Squats" to 100),
            exerciseReps = mapOf("Push Ups" to 1200, "Squats" to 1200),
            activeDays = 180,
        )
        val encoded = json.encodeToString(aggregate)
        val decoded = json.decodeFromString<YearlyAggregate>(encoded)
        assertEquals(aggregate, decoded)
    }

    @Test
    fun `StatsSnapshot roundtrip serialization`() {
        val snapshot = StatsSnapshot(
            totalBreaksAllTime = 42,
            totalRepsAllTime = 520,
            currentStreakDays = 5,
            longestStreakDays = 12,
            lastBreakDateString = "2026-03-30",
            firstBreakDateString = "2026-01-01",
            perExerciseCounts = mapOf("Push Ups" to 20, "Squats" to 22),
            perExerciseReps = mapOf("Push Ups" to 240, "Squats" to 280),
            uniqueExercisesUsed = setOf("Push Ups", "Squats"),
            hasCreatedCustomExercise = true,
        )
        val encoded = json.encodeToString(snapshot)
        val decoded = json.decodeFromString<StatsSnapshot>(encoded)
        assertEquals(snapshot, decoded)
    }

    @Test
    fun `StatsSnapshot default values when empty`() {
        val snapshot = StatsSnapshot()
        assertEquals(0, snapshot.totalBreaksAllTime)
        assertEquals(0, snapshot.totalRepsAllTime)
        assertEquals(0, snapshot.currentStreakDays)
        assertEquals(0, snapshot.longestStreakDays)
        assertEquals(null, snapshot.lastBreakDateString)
        assertTrue(snapshot.perExerciseCounts.isEmpty())
    }

    @Test
    fun `AchievementState roundtrip serialization`() {
        val state = AchievementState(
            unlockedIds = setOf("breaks_1", "breaks_10"),
            unlockTimestamps = mapOf("breaks_1" to 1000L, "breaks_10" to 2000L),
        )
        val encoded = json.encodeToString(state)
        val decoded = json.decodeFromString<AchievementState>(encoded)
        assertEquals(state, decoded)
    }

    @Test
    fun `AchievementState default values when empty`() {
        val state = AchievementState()
        assertTrue(state.unlockedIds.isEmpty())
        assertTrue(state.unlockTimestamps.isEmpty())
    }

    @Test
    fun `BreakRecord list roundtrip`() {
        val records = listOf(
            BreakRecord("Push Ups", 10, 1000L, "2026-03-30"),
            BreakRecord("Squats", 15, 2000L, "2026-03-30"),
        )
        val encoded = json.encodeToString(records)
        val decoded = json.decodeFromString<List<BreakRecord>>(encoded)
        assertEquals(records, decoded)
    }

    @Test
    fun `deserialize with unknown keys is ignored`() {
        val rawJson = """{"exerciseName":"Test","reps":5,"timestampMillis":1000,"dateString":"2026-01-01","extra":"field"}"""
        val record = json.decodeFromString<BreakRecord>(rawJson)
        assertEquals("Test", record.exerciseName)
        assertEquals(5, record.reps)
    }
}
