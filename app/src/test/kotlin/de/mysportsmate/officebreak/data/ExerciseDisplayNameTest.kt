package de.mysportsmate.officebreak.data

import android.content.Context
import de.mysportsmate.officebreak.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExerciseDisplayNameTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk()
        every { context.getString(R.string.exercise_push_ups) } returns "Push Ups"
    }

    @Test
    fun `displayName returns localized string when nameResKey is valid`() {
        val exercise = Exercise(name = "Liegest\u00FCtze", nameResKey = "exercise_push_ups")
        assertEquals("Push Ups", exercise.displayName(context))
    }

    @Test
    fun `displayName returns raw name when nameResKey is null`() {
        val exercise = Exercise(name = "My Custom Exercise")
        assertEquals("My Custom Exercise", exercise.displayName(context))
    }

    @Test
    fun `displayName returns raw name when nameResKey is unknown`() {
        val exercise = Exercise(name = "Custom", nameResKey = "exercise_nonexistent")
        assertEquals("Custom", exercise.displayName(context))
    }
}
