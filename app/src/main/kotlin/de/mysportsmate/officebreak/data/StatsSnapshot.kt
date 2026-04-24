package de.mysportsmate.officebreak.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class StatsSnapshot(
    val totalBreaksAllTime: Int = 0,
    val totalRepsAllTime: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastBreakDateString: String? = null,
    val firstBreakDateString: String? = null,
    val perExerciseCounts: Map<String, Int> = emptyMap(),
    val perExerciseReps: Map<String, Int> = emptyMap(),
    val uniqueExercisesUsed: Set<String> = emptySet(),
    val hasCreatedCustomExercise: Boolean = false,
)
