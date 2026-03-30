package de.mysportsmate.officebreak.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementRegistryTest {

    private fun snapshot(
        totalBreaks: Int = 0,
        totalReps: Int = 0,
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        uniqueExercises: Set<String> = emptySet(),
        perExerciseCounts: Map<String, Int> = emptyMap(),
        hasCreatedCustom: Boolean = false,
    ) = StatsSnapshot(
        totalBreaksAllTime = totalBreaks,
        totalRepsAllTime = totalReps,
        currentStreakDays = currentStreak,
        longestStreakDays = longestStreak,
        uniqueExercisesUsed = uniqueExercises,
        perExerciseCounts = perExerciseCounts,
        hasCreatedCustomExercise = hasCreatedCustom,
    )

    private fun context(
        breaksToday: Int = 1,
        uniqueExercisesToday: Int = 1,
        hourOfDay: Int = 12,
        dayOfWeek: Int = 1,
        daysSinceLastBreak: Int? = 1,
        isNewYear: Boolean = false,
    ) = BreakContext(
        exerciseName = "Push Ups",
        reps = 10,
        timestampMillis = System.currentTimeMillis(),
        hourOfDay = hourOfDay,
        dayOfWeek = dayOfWeek,
        dateString = "2026-03-30",
        breaksToday = breaksToday,
        uniqueExercisesToday = uniqueExercisesToday,
        daysSinceLastBreak = daysSinceLastBreak,
        isNewYear = isNewYear,
    )

    // --- byId ---

    @Test
    fun `byId returns correct definition for known ID`() {
        val def = AchievementRegistry.byId("breaks_1")
        assertNotNull(def)
        assertEquals("breaks_1", def!!.id)
    }

    @Test
    fun `byId returns null for unknown ID`() {
        assertNull(AchievementRegistry.byId("nonexistent_achievement"))
    }

    // --- Registry structure ---

    @Test
    fun `all categories are represented`() {
        val categories = AchievementRegistry.all.map { it.category }.toSet()
        AchievementCategory.entries.forEach { category ->
            assertTrue("Missing category: $category", category in categories)
        }
    }

    // --- Break milestones ---

    @Test
    fun `break milestone triggers at exact boundary`() {
        val targets = listOf(1, 10, 25, 50, 100, 500, 1000)
        val ctx = context()
        for (target in targets) {
            val def = AchievementRegistry.byId("breaks_$target")!!
            assertTrue("breaks_$target should trigger at $target", def.condition(snapshot(totalBreaks = target), ctx))
        }
    }

    @Test
    fun `break milestone does not trigger one below boundary`() {
        val targets = listOf(10, 25, 50, 100, 500, 1000)
        val ctx = context()
        for (target in targets) {
            val def = AchievementRegistry.byId("breaks_$target")!!
            assertTrue("breaks_$target should NOT trigger at ${target - 1}", !def.condition(snapshot(totalBreaks = target - 1), ctx))
        }
    }

    @Test
    fun `break milestone progress extractor returns correct values`() {
        val def = AchievementRegistry.byId("breaks_100")!!
        val (current, goal) = def.progressExtractor!!(snapshot(totalBreaks = 42))
        assertEquals(42, current)
        assertEquals(100, goal)
    }

    // --- Streak milestones ---

    @Test
    fun `streak milestone triggers at exact boundary`() {
        val targets = listOf(3, 7, 14, 30, 60, 100, 365)
        val ctx = context()
        for (target in targets) {
            val def = AchievementRegistry.byId("streak_$target")!!
            assertTrue("streak_$target should trigger at $target", def.condition(snapshot(currentStreak = target), ctx))
        }
    }

    @Test
    fun `streak milestone does not trigger one below boundary`() {
        val targets = listOf(3, 7, 14, 30, 60, 100, 365)
        val ctx = context()
        for (target in targets) {
            val def = AchievementRegistry.byId("streak_$target")!!
            assertTrue("streak_$target should NOT trigger at ${target - 1}", !def.condition(snapshot(currentStreak = target - 1), ctx))
        }
    }

    @Test
    fun `streak milestone uses longest streak when current is lower`() {
        val def = AchievementRegistry.byId("streak_7")!!
        assertTrue(def.condition(snapshot(currentStreak = 1, longestStreak = 7), context()))
    }

    @Test
    fun `streak progress extractor uses max of current and longest`() {
        val def = AchievementRegistry.byId("streak_30")!!
        val (current, goal) = def.progressExtractor!!(snapshot(currentStreak = 5, longestStreak = 20))
        assertEquals(20, current)
        assertEquals(30, goal)
    }

    // --- Rep milestones ---

    @Test
    fun `rep milestone triggers at exact boundary`() {
        val targets = listOf(100, 1000, 5000, 10000)
        val ctx = context()
        for (target in targets) {
            val def = AchievementRegistry.byId("reps_$target")!!
            assertTrue("reps_$target should trigger at $target", def.condition(snapshot(totalReps = target), ctx))
        }
    }

    @Test
    fun `rep milestone does not trigger one below boundary`() {
        val targets = listOf(100, 1000, 5000, 10000)
        val ctx = context()
        for (target in targets) {
            val def = AchievementRegistry.byId("reps_$target")!!
            assertTrue("reps_$target should NOT trigger at ${target - 1}", !def.condition(snapshot(totalReps = target - 1), ctx))
        }
    }

    // --- Daily breaks ---

    @Test
    fun `daily breaks milestone triggers at exact boundary`() {
        val targets = listOf(3, 5, 10)
        for (target in targets) {
            val def = AchievementRegistry.byId("daily_breaks_$target")!!
            assertTrue("daily_breaks_$target should trigger at $target", def.condition(snapshot(), context(breaksToday = target)))
        }
    }

    @Test
    fun `daily breaks milestone does not trigger one below boundary`() {
        val targets = listOf(3, 5, 10)
        for (target in targets) {
            val def = AchievementRegistry.byId("daily_breaks_$target")!!
            assertTrue("daily_breaks_$target should NOT trigger at ${target - 1}", !def.condition(snapshot(), context(breaksToday = target - 1)))
        }
    }

    // --- Mastery milestones ---

    @Test
    fun `mastery milestone triggers at exact boundary`() {
        val targets = listOf(10, 50, 100)
        val ctx = context()
        for (target in targets) {
            val def = AchievementRegistry.byId("mastery_any_$target")!!
            assertTrue("mastery_any_$target should trigger at $target", def.condition(snapshot(perExerciseCounts = mapOf("Push Ups" to target)), ctx))
        }
    }

    @Test
    fun `mastery milestone does not trigger one below boundary`() {
        val targets = listOf(10, 50, 100)
        val ctx = context()
        for (target in targets) {
            val def = AchievementRegistry.byId("mastery_any_$target")!!
            assertTrue("mastery_any_$target should NOT trigger at ${target - 1}", !def.condition(snapshot(perExerciseCounts = mapOf("Push Ups" to target - 1)), ctx))
        }
    }

    @Test
    fun `mastery progress extractor uses highest exercise count`() {
        val def = AchievementRegistry.byId("mastery_any_50")!!
        val (current, goal) = def.progressExtractor!!(snapshot(perExerciseCounts = mapOf("A" to 20, "B" to 35)))
        assertEquals(35, current)
        assertEquals(50, goal)
    }

    // --- Variety ---

    @Test
    fun `full rotation triggers at 6 unique exercises`() {
        val def = AchievementRegistry.byId("variety_full_rotation")!!
        val exercises = setOf("A", "B", "C", "D", "E", "F")
        assertTrue(def.condition(snapshot(uniqueExercises = exercises), context()))
    }

    @Test
    fun `full rotation does not trigger at 5 unique exercises`() {
        val def = AchievementRegistry.byId("variety_full_rotation")!!
        val exercises = setOf("A", "B", "C", "D", "E")
        assertTrue(!def.condition(snapshot(uniqueExercises = exercises), context()))
    }

    @Test
    fun `custom creator triggers when flag is set`() {
        val def = AchievementRegistry.byId("variety_custom_creator")!!
        assertTrue(def.condition(snapshot(hasCreatedCustom = true), context()))
    }

    @Test
    fun `custom creator does not trigger when flag is false`() {
        val def = AchievementRegistry.byId("variety_custom_creator")!!
        assertTrue(!def.condition(snapshot(hasCreatedCustom = false), context()))
    }
}
