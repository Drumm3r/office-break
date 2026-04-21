package de.mysportsmate.officebreak.data

import android.content.Context
import de.mysportsmate.officebreak.R

object ExerciseConfig {

    val resKeyToId: Map<String, Int> = mapOf(
        // Home Workout
        "exercise_push_ups" to R.string.exercise_push_ups,
        "exercise_squats" to R.string.exercise_squats,
        "exercise_deadlifts" to R.string.exercise_deadlifts,
        "exercise_lunges" to R.string.exercise_lunges,
        "exercise_sit_ups" to R.string.exercise_sit_ups,
        "exercise_superman_angels" to R.string.exercise_superman_angels,
        "exercise_plank" to R.string.exercise_plank,
        "exercise_glute_bridge" to R.string.exercise_glute_bridge,
        // Home Mobility
        "exercise_cat_cow" to R.string.exercise_cat_cow,
        "exercise_childs_pose" to R.string.exercise_childs_pose,
        "exercise_downward_dog" to R.string.exercise_downward_dog,
        "exercise_seated_twist" to R.string.exercise_seated_twist,
        "exercise_hip_circles" to R.string.exercise_hip_circles,
        "exercise_standing_forward_fold" to R.string.exercise_standing_forward_fold,
        "exercise_thread_the_needle" to R.string.exercise_thread_the_needle,
        "exercise_pigeon_stretch" to R.string.exercise_pigeon_stretch,
        // Office
        "exercise_shoulder_blade_squeeze" to R.string.exercise_shoulder_blade_squeeze,
        "exercise_chest_opener" to R.string.exercise_chest_opener,
        "exercise_neck_stretch" to R.string.exercise_neck_stretch,
        "exercise_calf_raises" to R.string.exercise_calf_raises,
        "exercise_seated_leg_extension" to R.string.exercise_seated_leg_extension,
        "exercise_wrist_circles" to R.string.exercise_wrist_circles,
        "exercise_ankle_circles" to R.string.exercise_ankle_circles,
        "exercise_seated_cat_cow" to R.string.exercise_seated_cat_cow,
        "exercise_seated_core_bracing" to R.string.exercise_seated_core_bracing,
    )

    private val knownNameToResKey: Map<String, String> = mapOf(
        // English - Home Workout
        "Push Ups" to "exercise_push_ups",
        "Squats" to "exercise_squats",
        "Deadlifts" to "exercise_deadlifts",
        "Lunges" to "exercise_lunges",
        "Sit Ups" to "exercise_sit_ups",
        "Superman Angels" to "exercise_superman_angels",
        "Plank" to "exercise_plank",
        "Glute Bridge" to "exercise_glute_bridge",
        // English - Home Mobility
        "Cat-Cow Stretch" to "exercise_cat_cow",
        "Child's Pose" to "exercise_childs_pose",
        "Downward Dog" to "exercise_downward_dog",
        "Seated Spinal Twist" to "exercise_seated_twist",
        "Hip Circles" to "exercise_hip_circles",
        "Standing Forward Fold" to "exercise_standing_forward_fold",
        "Thread the Needle" to "exercise_thread_the_needle",
        "Pigeon Stretch" to "exercise_pigeon_stretch",
        // English - Office
        "Shoulder Blade Squeeze" to "exercise_shoulder_blade_squeeze",
        "Chest Opener" to "exercise_chest_opener",
        "Neck Stretch" to "exercise_neck_stretch",
        "Calf Raises" to "exercise_calf_raises",
        "Seated Leg Extension" to "exercise_seated_leg_extension",
        "Wrist Circles" to "exercise_wrist_circles",
        "Ankle Circles" to "exercise_ankle_circles",
        "Seated Cat-Cow" to "exercise_seated_cat_cow",
        "Seated Core Bracing" to "exercise_seated_core_bracing",
        // German - Home Workout
        "Liegestütze" to "exercise_push_ups",
        "Kniebeugen" to "exercise_squats",
        "Kniebeuge" to "exercise_squats",
        "Kreuzheben" to "exercise_deadlifts",
        "Ausfallschritte" to "exercise_lunges",
        "Ausfallschritt" to "exercise_lunges",
        "Hüftheben" to "exercise_glute_bridge",
        // German - Home Mobility
        "Katze-Kuh" to "exercise_cat_cow",
        "Kindhaltung" to "exercise_childs_pose",
        "Herabschauender Hund" to "exercise_downward_dog",
        "Rumpfdrehung" to "exercise_seated_twist",
        "Hüftkreise" to "exercise_hip_circles",
        "Stehende Vorbeuge" to "exercise_standing_forward_fold",
        // German - Office
        "Schulterblätter zusammenziehen" to "exercise_shoulder_blade_squeeze",
        "Brustöffner" to "exercise_chest_opener",
        "Nacken dehnen" to "exercise_neck_stretch",
        "Wadenheben" to "exercise_calf_raises",
        "Sitzende Beinstreckung" to "exercise_seated_leg_extension",
        "Handgelenke kreisen" to "exercise_wrist_circles",
        "Fußgelenke kreisen" to "exercise_ankle_circles",
        "Katze-Kuh im Sitzen" to "exercise_seated_cat_cow",
        "Bauchspannung halten" to "exercise_seated_core_bracing",
    )

    fun resolveDisplayName(context: Context, rawName: String): String {
        val resKey = knownNameToResKey[rawName]
        if (resKey != null) {
            val resId = resKeyToId[resKey]
            if (resId != null) return context.getString(resId)
        }
        return rawName
    }

    private val modeResKeys: Map<ExerciseMode, Set<String>> = mapOf(
        ExerciseMode.HOME_WORKOUT to setOf(
            "exercise_push_ups", "exercise_squats", "exercise_deadlifts", "exercise_lunges",
            "exercise_sit_ups", "exercise_superman_angels", "exercise_plank", "exercise_glute_bridge",
        ),
        ExerciseMode.HOME_MOBILITY to setOf(
            "exercise_cat_cow", "exercise_childs_pose", "exercise_downward_dog", "exercise_seated_twist",
            "exercise_hip_circles", "exercise_standing_forward_fold", "exercise_thread_the_needle",
            "exercise_pigeon_stretch",
        ),
        ExerciseMode.OFFICE to setOf(
            "exercise_shoulder_blade_squeeze", "exercise_chest_opener", "exercise_neck_stretch",
            "exercise_calf_raises", "exercise_seated_leg_extension", "exercise_wrist_circles",
            "exercise_ankle_circles", "exercise_seated_cat_cow", "exercise_seated_core_bracing",
        ),
    )

    fun exerciseBelongsToMode(resKey: String?, mode: ExerciseMode): Boolean =
        resKey != null && modeResKeys[mode]?.contains(resKey) == true

    fun defaultExercises(context: Context): List<Exercise> =
        defaultExercises(context, ExerciseMode.HOME_WORKOUT)

    fun defaultExercises(context: Context, mode: ExerciseMode): List<Exercise> {
        val enabledKeys = modeResKeys[mode] ?: emptySet()

        return allExercises(context).map { exercise ->
            exercise.copy(isEnabled = exercise.nameResKey in enabledKeys)
        }
    }

    fun allExercises(context: Context): List<Exercise> = listOf(
        // Home Workout
        Exercise(name = context.getString(R.string.exercise_push_ups), nameResKey = "exercise_push_ups"),
        Exercise(name = context.getString(R.string.exercise_squats), nameResKey = "exercise_squats"),
        Exercise(name = context.getString(R.string.exercise_deadlifts), nameResKey = "exercise_deadlifts"),
        Exercise(name = context.getString(R.string.exercise_lunges), nameResKey = "exercise_lunges"),
        Exercise(name = context.getString(R.string.exercise_sit_ups), nameResKey = "exercise_sit_ups"),
        Exercise(name = context.getString(R.string.exercise_superman_angels), nameResKey = "exercise_superman_angels"),
        Exercise(name = context.getString(R.string.exercise_plank), nameResKey = "exercise_plank"),
        Exercise(name = context.getString(R.string.exercise_glute_bridge), nameResKey = "exercise_glute_bridge"),
        // Home Mobility
        Exercise(name = context.getString(R.string.exercise_cat_cow), nameResKey = "exercise_cat_cow"),
        Exercise(name = context.getString(R.string.exercise_childs_pose), nameResKey = "exercise_childs_pose"),
        Exercise(name = context.getString(R.string.exercise_downward_dog), nameResKey = "exercise_downward_dog"),
        Exercise(name = context.getString(R.string.exercise_seated_twist), nameResKey = "exercise_seated_twist"),
        Exercise(name = context.getString(R.string.exercise_hip_circles), nameResKey = "exercise_hip_circles"),
        Exercise(name = context.getString(R.string.exercise_standing_forward_fold), nameResKey = "exercise_standing_forward_fold"),
        Exercise(name = context.getString(R.string.exercise_thread_the_needle), nameResKey = "exercise_thread_the_needle"),
        Exercise(name = context.getString(R.string.exercise_pigeon_stretch), nameResKey = "exercise_pigeon_stretch"),
        // Office
        Exercise(name = context.getString(R.string.exercise_shoulder_blade_squeeze), nameResKey = "exercise_shoulder_blade_squeeze"),
        Exercise(name = context.getString(R.string.exercise_chest_opener), nameResKey = "exercise_chest_opener"),
        Exercise(name = context.getString(R.string.exercise_neck_stretch), nameResKey = "exercise_neck_stretch"),
        Exercise(name = context.getString(R.string.exercise_calf_raises), nameResKey = "exercise_calf_raises"),
        Exercise(name = context.getString(R.string.exercise_seated_leg_extension), nameResKey = "exercise_seated_leg_extension"),
        Exercise(name = context.getString(R.string.exercise_wrist_circles), nameResKey = "exercise_wrist_circles"),
        Exercise(name = context.getString(R.string.exercise_ankle_circles), nameResKey = "exercise_ankle_circles"),
        Exercise(name = context.getString(R.string.exercise_seated_cat_cow), nameResKey = "exercise_seated_cat_cow"),
        Exercise(name = context.getString(R.string.exercise_seated_core_bracing), nameResKey = "exercise_seated_core_bracing"),
    )
}
