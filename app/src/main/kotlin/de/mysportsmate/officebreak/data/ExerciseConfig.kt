package de.mysportsmate.officebreak.data

import android.content.Context
import de.mysportsmate.officebreak.R

object ExerciseConfig {
    fun defaultExercises(context: Context): List<Exercise> = listOf(
        Exercise(name = context.getString(R.string.exercise_push_ups)),
        Exercise(name = context.getString(R.string.exercise_squats)),
        Exercise(name = context.getString(R.string.exercise_deadlifts)),
        Exercise(name = context.getString(R.string.exercise_lunges)),
        Exercise(name = context.getString(R.string.exercise_sit_ups)),
        Exercise(name = context.getString(R.string.exercise_superman_angels)),
    )
}
