package de.mysportsmate.officebreak.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class BreakRecord(
    val exerciseName: String,
    val reps: Int,
    val timestampMillis: Long,
    val dateString: String,
)
