package de.mysportsmate.officebreak.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEngineTest {

    private fun baseContext(
        exerciseName: String = "Push Ups",
        reps: Int = 10,
        hourOfDay: Int = 12,
        dayOfWeek: Int = 1,
        breaksToday: Int = 1,
        uniqueExercisesToday: Int = 1,
        daysSinceLastBreak: Int? = 1,
        isNewYear: Boolean = false,
    ) = BreakContext(
        exerciseName = exerciseName,
        reps = reps,
        timestampMillis = System.currentTimeMillis(),
        hourOfDay = hourOfDay,
        dayOfWeek = dayOfWeek,
        dateString = "2026-03-30",
        breaksToday = breaksToday,
        uniqueExercisesToday = uniqueExercisesToday,
        daysSinceLastBreak = daysSinceLastBreak,
        isNewYear = isNewYear,
    )

    @Test
    fun `first break unlocks breaks_1`() {
        val snapshot = StatsSnapshot(totalBreaksAllTime = 1)
        val result = AchievementEngine.evaluate(snapshot, baseContext(), emptySet())
        assertTrue(result.any { it.id == "breaks_1" })
    }

    @Test
    fun `already unlocked achievement is not re-triggered`() {
        val snapshot = StatsSnapshot(totalBreaksAllTime = 1)
        val result = AchievementEngine.evaluate(snapshot, baseContext(), setOf("breaks_1"))
        assertTrue(result.none { it.id == "breaks_1" })
    }

    @Test
    fun `streak_3 unlocks at 3 days`() {
        val snapshot = StatsSnapshot(currentStreakDays = 3)
        val result = AchievementEngine.evaluate(snapshot, baseContext(), emptySet())
        assertTrue(result.any { it.id == "streak_3" })
    }

    @Test
    fun `streak achievement uses longestStreakDays too`() {
        val snapshot = StatsSnapshot(currentStreakDays = 1, longestStreakDays = 7)
        val result = AchievementEngine.evaluate(snapshot, baseContext(), emptySet())
        assertTrue(result.any { it.id == "streak_7" })
    }

    @Test
    fun `rep milestone at 100`() {
        val snapshot = StatsSnapshot(totalRepsAllTime = 100)
        val result = AchievementEngine.evaluate(snapshot, baseContext(), emptySet())
        assertTrue(result.any { it.id == "reps_100" })
    }

    @Test
    fun `early bird at 7am`() {
        val result = AchievementEngine.evaluate(StatsSnapshot(), baseContext(hourOfDay = 7), emptySet())
        assertTrue(result.any { it.id == "daily_early_bird" })
    }

    @Test
    fun `early bird not at 8am`() {
        val result = AchievementEngine.evaluate(StatsSnapshot(), baseContext(hourOfDay = 8), emptySet())
        assertTrue(result.none { it.id == "daily_early_bird" })
    }

    @Test
    fun `night owl at 20`() {
        val result = AchievementEngine.evaluate(StatsSnapshot(), baseContext(hourOfDay = 20), emptySet())
        assertTrue(result.any { it.id == "daily_night_owl" })
    }

    @Test
    fun `lunch hero at 12`() {
        val result = AchievementEngine.evaluate(StatsSnapshot(), baseContext(hourOfDay = 12), emptySet())
        assertTrue(result.any { it.id == "daily_lunch_hero" })
    }

    @Test
    fun `weekend warrior on saturday`() {
        val result = AchievementEngine.evaluate(StatsSnapshot(), baseContext(dayOfWeek = 6), emptySet())
        assertTrue(result.any { it.id == "fun_weekend_warrior" })
    }

    @Test
    fun `weekend warrior not on weekday`() {
        val result = AchievementEngine.evaluate(StatsSnapshot(), baseContext(dayOfWeek = 3), emptySet())
        assertTrue(result.none { it.id == "fun_weekend_warrior" })
    }

    @Test
    fun `new year achievement`() {
        val result = AchievementEngine.evaluate(StatsSnapshot(), baseContext(isNewYear = true), emptySet())
        assertTrue(result.any { it.id == "fun_new_year" })
    }

    @Test
    fun `comeback after 7 days`() {
        val result = AchievementEngine.evaluate(
            StatsSnapshot(),
            baseContext(daysSinceLastBreak = 7),
            emptySet(),
        )
        assertTrue(result.any { it.id == "fun_comeback" })
    }

    @Test
    fun `comeback not after 6 days`() {
        val result = AchievementEngine.evaluate(
            StatsSnapshot(),
            baseContext(daysSinceLastBreak = 6),
            emptySet(),
        )
        assertTrue(result.none { it.id == "fun_comeback" })
    }

    @Test
    fun `daily breaks 3 at 3 breaks`() {
        val result = AchievementEngine.evaluate(StatsSnapshot(), baseContext(breaksToday = 3), emptySet())
        assertTrue(result.any { it.id == "daily_breaks_3" })
    }

    @Test
    fun `daily mix at 3 unique exercises`() {
        val result = AchievementEngine.evaluate(
            StatsSnapshot(),
            baseContext(uniqueExercisesToday = 3),
            emptySet(),
        )
        assertTrue(result.any { it.id == "variety_daily_mix" })
    }

    @Test
    fun `all rounder with 3 unique exercises used`() {
        val snapshot = StatsSnapshot(uniqueExercisesUsed = setOf("A", "B", "C"))
        val result = AchievementEngine.evaluate(snapshot, baseContext(), emptySet())
        assertTrue(result.any { it.id == "variety_all_rounder" })
    }

    @Test
    fun `custom creator when flag is set`() {
        val snapshot = StatsSnapshot(hasCreatedCustomExercise = true)
        val result = AchievementEngine.evaluate(snapshot, baseContext(), emptySet())
        assertTrue(result.any { it.id == "variety_custom_creator" })
    }

    @Test
    fun `exercise mastery at 10`() {
        val snapshot = StatsSnapshot(perExerciseCounts = mapOf("Push Ups" to 10))
        val result = AchievementEngine.evaluate(snapshot, baseContext(), emptySet())
        assertTrue(result.any { it.id == "mastery_any_10" })
    }

    @Test
    fun `multiple achievements can unlock simultaneously`() {
        val snapshot = StatsSnapshot(
            totalBreaksAllTime = 1,
            totalRepsAllTime = 100,
            currentStreakDays = 3,
        )
        val result = AchievementEngine.evaluate(snapshot, baseContext(), emptySet())
        assertTrue(result.size >= 3)
        assertTrue(result.any { it.id == "breaks_1" })
        assertTrue(result.any { it.id == "reps_100" })
        assertTrue(result.any { it.id == "streak_3" })
    }

    @Test
    fun `registry has expected number of achievements`() {
        assertEquals(34, AchievementRegistry.all.size)
    }

    @Test
    fun `all achievement ids are unique`() {
        val ids = AchievementRegistry.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
