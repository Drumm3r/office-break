package de.mysportsmate.officebreak.data

enum class AchievementCategory {
    BREAK_MILESTONES,
    STREAK_MILESTONES,
    REP_MILESTONES,
    VARIETY,
    DAILY_CHALLENGES,
    FUN_SEASONAL,
    EXERCISE_MASTERY,
}

data class AchievementDefinition(
    val id: String,
    val category: AchievementCategory,
    val titleResKey: String,
    val descriptionResKey: String,
    val iconName: String,
    val condition: (StatsSnapshot, BreakContext) -> Boolean,
    val progressExtractor: ((StatsSnapshot) -> Pair<Int, Int>)? = null,
)

data class BreakContext(
    val exerciseName: String,
    val reps: Int,
    val timestampMillis: Long,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val dateString: String,
    val breaksToday: Int,
    val uniqueExercisesToday: Int,
    val daysSinceLastBreak: Int?,
    val isNewYear: Boolean,
)
