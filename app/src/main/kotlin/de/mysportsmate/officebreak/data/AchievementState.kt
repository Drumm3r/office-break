package de.mysportsmate.officebreak.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class AchievementState(
    val unlockedIds: Set<String> = emptySet(),
    val unlockTimestamps: Map<String, Long> = emptyMap(),
)
