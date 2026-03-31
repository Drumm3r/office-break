package de.mysportsmate.officebreak.data

import kotlinx.serialization.Serializable

@Serializable
data class AchievementState(
    val unlockedIds: Set<String> = emptySet(),
    val unlockTimestamps: Map<String, Long> = emptyMap(),
)
