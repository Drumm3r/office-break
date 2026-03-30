package de.mysportsmate.officebreak.data

import android.content.Context
import de.mysportsmate.officebreak.R

object ExerciseConfig {

    val resKeyToId: Map<String, Int> = mapOf(
        "exercise_push_ups" to R.string.exercise_push_ups,
        "exercise_squats" to R.string.exercise_squats,
        "exercise_deadlifts" to R.string.exercise_deadlifts,
        "exercise_lunges" to R.string.exercise_lunges,
        "exercise_sit_ups" to R.string.exercise_sit_ups,
        "exercise_superman_angels" to R.string.exercise_superman_angels,
    )

    private val knownNameToResKey: Map<String, String> = mapOf(
        "Push Ups" to "exercise_push_ups",
        "Squats" to "exercise_squats",
        "Deadlifts" to "exercise_deadlifts",
        "Lunges" to "exercise_lunges",
        "Sit Ups" to "exercise_sit_ups",
        "Superman Angels" to "exercise_superman_angels",
        "Liegestütze" to "exercise_push_ups",
        "Kniebeuge" to "exercise_squats",
        "Kreuzheben" to "exercise_deadlifts",
        "Ausfallschritt" to "exercise_lunges",
    )

    fun resolveDisplayName(context: Context, rawName: String): String {
        val resKey = knownNameToResKey[rawName]
        if (resKey != null) {
            val resId = resKeyToId[resKey]
            if (resId != null) return context.getString(resId)
        }
        return rawName
    }

    fun defaultExercises(context: Context): List<Exercise> = listOf(
        Exercise(name = context.getString(R.string.exercise_push_ups), nameResKey = "exercise_push_ups"),
        Exercise(name = context.getString(R.string.exercise_squats), nameResKey = "exercise_squats"),
        Exercise(name = context.getString(R.string.exercise_deadlifts), nameResKey = "exercise_deadlifts"),
        Exercise(name = context.getString(R.string.exercise_lunges), nameResKey = "exercise_lunges"),
        Exercise(name = context.getString(R.string.exercise_sit_ups), nameResKey = "exercise_sit_ups"),
        Exercise(name = context.getString(R.string.exercise_superman_angels), nameResKey = "exercise_superman_angels"),
    )
}
