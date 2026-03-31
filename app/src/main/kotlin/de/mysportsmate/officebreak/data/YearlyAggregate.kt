package de.mysportsmate.officebreak.data

import kotlinx.serialization.Serializable

@Serializable
data class YearlyAggregate(
    val year: Int,
    val totalBreaks: Int,
    val totalReps: Int,
    val exerciseCounts: Map<String, Int>,
    val exerciseReps: Map<String, Int>,
    val activeDays: Int,
)
