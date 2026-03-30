package de.mysportsmate.officebreak.data

object AchievementEngine {

    fun evaluate(
        snapshot: StatsSnapshot,
        context: BreakContext,
        currentlyUnlocked: Set<String>,
    ): List<AchievementDefinition> {
        return AchievementRegistry.all
            .filter { it.id !in currentlyUnlocked }
            .filter { it.condition(snapshot, context) }
    }
}
