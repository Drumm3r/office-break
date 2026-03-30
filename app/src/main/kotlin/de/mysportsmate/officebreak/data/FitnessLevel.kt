package de.mysportsmate.officebreak.data

enum class FitnessLevel(
    val hours: Int,
    val minutes: Int,
    val reps: Int,
) {
    BEGINNER(hours = 1, minutes = 0, reps = 5),
    MODERATE(hours = 0, minutes = 45, reps = 10),
    ATHLETIC(hours = 0, minutes = 30, reps = 15),
}
