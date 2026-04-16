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
        // Home Workout
        every { context.getString(R.string.exercise_push_ups) } returns "Push Ups"
        every { context.getString(R.string.exercise_squats) } returns "Squats"
        every { context.getString(R.string.exercise_deadlifts) } returns "Deadlifts"
        every { context.getString(R.string.exercise_lunges) } returns "Lunges"
        every { context.getString(R.string.exercise_sit_ups) } returns "Sit Ups"
        every { context.getString(R.string.exercise_superman_angels) } returns "Superman Angels"
        every { context.getString(R.string.exercise_plank) } returns "Plank"
        every { context.getString(R.string.exercise_glute_bridge) } returns "Glute Bridge"
        // Home Mobility
        every { context.getString(R.string.exercise_cat_cow) } returns "Cat-Cow Stretch"
        every { context.getString(R.string.exercise_childs_pose) } returns "Child's Pose"
        every { context.getString(R.string.exercise_downward_dog) } returns "Downward Dog"
        every { context.getString(R.string.exercise_seated_twist) } returns "Seated Spinal Twist"
        every { context.getString(R.string.exercise_hip_circles) } returns "Hip Circles"
        every { context.getString(R.string.exercise_standing_forward_fold) } returns "Standing Forward Fold"
        every { context.getString(R.string.exercise_thread_the_needle) } returns "Thread the Needle"
        every { context.getString(R.string.exercise_pigeon_stretch) } returns "Pigeon Stretch"
        // Office
        every { context.getString(R.string.exercise_shoulder_blade_squeeze) } returns "Shoulder Blade Squeeze"
        every { context.getString(R.string.exercise_chest_opener) } returns "Chest Opener"
        every { context.getString(R.string.exercise_neck_stretch) } returns "Neck Stretch"
        every { context.getString(R.string.exercise_calf_raises) } returns "Calf Raises"
        every { context.getString(R.string.exercise_seated_leg_extension) } returns "Seated Leg Extension"
        every { context.getString(R.string.exercise_wrist_circles) } returns "Wrist Circles"
        every { context.getString(R.string.exercise_ankle_circles) } returns "Ankle Circles"
        every { context.getString(R.string.exercise_seated_cat_cow) } returns "Seated Cat-Cow"
        every { context.getString(R.string.exercise_seated_core_bracing) } returns "Seated Core Bracing"
    }

    @Test
    fun `resKeyToId covers all 24 exercises`() {
        assertEquals(25, ExerciseConfig.resKeyToId.size)
    }

    @Test
    fun `all known English name mappings resolve to valid resource keys`() {
        val knownNames = listOf(
            "Push Ups", "Squats", "Deadlifts", "Lunges", "Sit Ups", "Superman Angels",
            "Plank", "Glute Bridge",
            "Cat-Cow Stretch", "Child's Pose", "Downward Dog", "Seated Spinal Twist",
            "Hip Circles", "Standing Forward Fold", "Thread the Needle", "Pigeon Stretch",
            "Shoulder Blade Squeeze", "Chest Opener", "Neck Stretch", "Calf Raises",
            "Seated Leg Extension", "Wrist Circles", "Ankle Circles", "Seated Cat-Cow",
            "Seated Core Bracing",
        )
        for (name in knownNames) {
            val resolved = ExerciseConfig.resolveDisplayName(context, name)
            assertTrue("'$name' should resolve to a localized name, got '$resolved'", resolved.isNotEmpty())
        }
    }

    @Test
    fun `all known German name mappings resolve to valid resource keys`() {
        val knownNames = listOf(
            "Liegest\u00FCtze", "Kniebeugen", "Kniebeuge", "Kreuzheben",
            "Ausfallschritte", "Ausfallschritt", "H\u00FCftheben",
            "Katze-Kuh", "Kindhaltung", "Herabschauender Hund", "Rumpfdrehung",
            "H\u00FCftkreise", "Stehende Vorbeuge",
            "Schulterbl\u00E4tter zusammenziehen", "Brust\u00F6ffner", "Nacken dehnen",
            "Wadenheben", "Sitzende Beinstreckung", "Handgelenke kreisen",
            "Fu\u00DFgelenke kreisen", "Katze-Kuh im Sitzen",
            "Bauchspannung halten",
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
    fun `defaultExercises without mode returns all exercises with HOME_WORKOUT enabled`() {
        val exercises = ExerciseConfig.defaultExercises(context)
        assertEquals(25, exercises.size)
        assertEquals(8, exercises.count { it.isEnabled })
        assertEquals("Push Ups", exercises[0].name)
        assertTrue(exercises[0].isEnabled)
    }

    @Test
    fun `defaultExercises HOME_WORKOUT enables 8 workout exercises`() {
        val exercises = ExerciseConfig.defaultExercises(context, ExerciseMode.HOME_WORKOUT)
        assertEquals(25, exercises.size)
        val enabled = exercises.filter { it.isEnabled }
        assertEquals(8, enabled.size)
        assertTrue(enabled.all { it.nameResKey != null })
        assertTrue(enabled.any { it.name == "Push Ups" })
        assertTrue(enabled.any { it.name == "Plank" })
    }

    @Test
    fun `defaultExercises HOME_MOBILITY enables 8 mobility exercises`() {
        val exercises = ExerciseConfig.defaultExercises(context, ExerciseMode.HOME_MOBILITY)
        assertEquals(25, exercises.size)
        val enabled = exercises.filter { it.isEnabled }
        assertEquals(8, enabled.size)
        assertTrue(enabled.any { it.name == "Cat-Cow Stretch" })
        assertTrue(enabled.any { it.name == "Pigeon Stretch" })
    }

    @Test
    fun `defaultExercises OFFICE enables 9 office exercises`() {
        val exercises = ExerciseConfig.defaultExercises(context, ExerciseMode.OFFICE)
        assertEquals(25, exercises.size)
        val enabled = exercises.filter { it.isEnabled }
        assertEquals(9, enabled.size)
        assertTrue(enabled.any { it.name == "Shoulder Blade Squeeze" })
        assertTrue(enabled.any { it.name == "Seated Core Bracing" })
    }

    @Test
    fun `allExercises returns 25 exercises all enabled by default`() {
        val exercises = ExerciseConfig.allExercises(context)
        assertEquals(25, exercises.size)
        assertTrue(exercises.all { it.isEnabled })
        assertTrue(exercises.all { it.nameResKey != null })
    }
}
