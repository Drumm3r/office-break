package de.mysportsmate.officebreak.data

import android.content.Context
import de.mysportsmate.officebreak.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExerciseConfigTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk()
        every { context.getString(R.string.exercise_push_ups) } returns "Push Ups"
        every { context.getString(R.string.exercise_squats) } returns "Squats"
        every { context.getString(R.string.exercise_deadlifts) } returns "Deadlifts"
        every { context.getString(R.string.exercise_lunges) } returns "Lunges"
        every { context.getString(R.string.exercise_sit_ups) } returns "Sit Ups"
        every { context.getString(R.string.exercise_superman_angels) } returns "Superman Angels"
    }

    @Test
    fun `resKeyToId covers all 6 default exercises`() {
        val expectedKeys = setOf(
            "exercise_push_ups",
            "exercise_squats",
            "exercise_deadlifts",
            "exercise_lunges",
            "exercise_sit_ups",
            "exercise_superman_angels",
        )
        assertEquals(expectedKeys, ExerciseConfig.resKeyToId.keys)
    }

    @Test
    fun `all known name mappings resolve to valid resource keys`() {
        val knownNames = listOf(
            "Push Ups", "Squats", "Deadlifts", "Lunges", "Sit Ups", "Superman Angels",
            "Liegest\u00FCtze", "Kniebeuge", "Kreuzheben", "Ausfallschritt",
        )
        for (name in knownNames) {
            val resolved = ExerciseConfig.resolveDisplayName(context, name)
            assertTrue("'$name' should resolve to a localized name, got '$resolved'", resolved.isNotEmpty())
        }
    }

    @Test
    fun `resolveDisplayName returns localized name for English exercise`() {
        assertEquals("Push Ups", ExerciseConfig.resolveDisplayName(context, "Push Ups"))
    }

    @Test
    fun `resolveDisplayName returns localized name for German exercise`() {
        assertEquals("Push Ups", ExerciseConfig.resolveDisplayName(context, "Liegest\u00FCtze"))
    }

    @Test
    fun `resolveDisplayName returns raw name for unknown custom exercise`() {
        assertEquals("My Custom Exercise", ExerciseConfig.resolveDisplayName(context, "My Custom Exercise"))
    }

    @Test
    fun `resolveDisplayName returns raw name for empty string`() {
        assertEquals("", ExerciseConfig.resolveDisplayName(context, ""))
    }

    @Test
    fun `defaultExercises returns 6 exercises all enabled`() {
        val exercises = ExerciseConfig.defaultExercises(context)
        assertEquals(6, exercises.size)
        assertTrue(exercises.all { it.isEnabled })
    }

    @Test
    fun `defaultExercises all have resource keys`() {
        val exercises = ExerciseConfig.defaultExercises(context)
        assertTrue(exercises.all { it.nameResKey != null })
    }
}
