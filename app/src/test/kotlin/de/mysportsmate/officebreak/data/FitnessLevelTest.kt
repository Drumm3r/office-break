package de.mysportsmate.officebreak.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessLevelTest {

    @Test
    fun `all three levels exist`() {
        assertEquals(3, FitnessLevel.entries.size)
    }

    @Test
    fun `all levels have non-negative timer values`() {
        FitnessLevel.entries.forEach { level ->
            assertTrue("${level.name} hours should be >= 0", level.hours >= 0)
            assertTrue("${level.name} minutes should be >= 0", level.minutes >= 0)
        }
    }

    @Test
    fun `all levels have positive reps`() {
        FitnessLevel.entries.forEach { level ->
            assertTrue("${level.name} reps should be > 0", level.reps > 0)
        }
    }

    @Test
    fun `beginner has longest interval`() {
        val beginnerMinutes = FitnessLevel.BEGINNER.hours * 60 + FitnessLevel.BEGINNER.minutes
        val moderateMinutes = FitnessLevel.MODERATE.hours * 60 + FitnessLevel.MODERATE.minutes
        val athleticMinutes = FitnessLevel.ATHLETIC.hours * 60 + FitnessLevel.ATHLETIC.minutes

        assertTrue(beginnerMinutes >= moderateMinutes)
        assertTrue(moderateMinutes >= athleticMinutes)
    }

    @Test
    fun `reps increase with fitness level`() {
        assertTrue(FitnessLevel.BEGINNER.reps <= FitnessLevel.MODERATE.reps)
        assertTrue(FitnessLevel.MODERATE.reps <= FitnessLevel.ATHLETIC.reps)
    }
}
