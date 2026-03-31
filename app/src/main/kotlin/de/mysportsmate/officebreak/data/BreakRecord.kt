package de.mysportsmate.officebreak.data

import kotlinx.serialization.Serializable

@Serializable
data class BreakRecord(
    val exerciseName: String,
    val reps: Int,
    val timestampMillis: Long,
    val dateString: String,
)
