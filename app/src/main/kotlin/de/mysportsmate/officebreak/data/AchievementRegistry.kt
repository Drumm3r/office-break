package de.mysportsmate.officebreak.data

import de.mysportsmate.officebreak.R

object AchievementRegistry {

    val all: List<AchievementDefinition> = buildList {
        // Break milestones
        breakMilestone(1, R.string.achievement_breaks_1_title, R.string.achievement_breaks_1_desc)
        breakMilestone(10, R.string.achievement_breaks_10_title, R.string.achievement_breaks_10_desc)
        breakMilestone(25, R.string.achievement_breaks_25_title, R.string.achievement_breaks_25_desc)
        breakMilestone(50, R.string.achievement_breaks_50_title, R.string.achievement_breaks_50_desc)
        breakMilestone(100, R.string.achievement_breaks_100_title, R.string.achievement_breaks_100_desc)
        breakMilestone(500, R.string.achievement_breaks_500_title, R.string.achievement_breaks_500_desc)
        breakMilestone(1000, R.string.achievement_breaks_1000_title, R.string.achievement_breaks_1000_desc)

        // Streak milestones
        streakMilestone(3, R.string.achievement_streak_3_title, R.string.achievement_streak_3_desc)
        streakMilestone(7, R.string.achievement_streak_7_title, R.string.achievement_streak_7_desc)
        streakMilestone(14, R.string.achievement_streak_14_title, R.string.achievement_streak_14_desc)
        streakMilestone(30, R.string.achievement_streak_30_title, R.string.achievement_streak_30_desc)
        streakMilestone(60, R.string.achievement_streak_60_title, R.string.achievement_streak_60_desc)
        streakMilestone(100, R.string.achievement_streak_100_title, R.string.achievement_streak_100_desc)
        streakMilestone(365, R.string.achievement_streak_365_title, R.string.achievement_streak_365_desc)

        // Rep milestones
        repMilestone(100, R.string.achievement_reps_100_title, R.string.achievement_reps_100_desc)
        repMilestone(1000, R.string.achievement_reps_1000_title, R.string.achievement_reps_1000_desc)
        repMilestone(5000, R.string.achievement_reps_5000_title, R.string.achievement_reps_5000_desc)
        repMilestone(10000, R.string.achievement_reps_10000_title, R.string.achievement_reps_10000_desc)

        // Variety
        add(
            AchievementDefinition(
                id = "variety_all_rounder",
                category = AchievementCategory.VARIETY,
                titleResId = R.string.achievement_all_rounder_title,
                descriptionResId = R.string.achievement_all_rounder_desc,
                iconName = "Diversity3",
                condition = { stats, _ -> stats.uniqueExercisesUsed.size >= 3 },
            ),
        )
        add(
            AchievementDefinition(
                id = "variety_daily_mix",
                category = AchievementCategory.VARIETY,
                titleResId = R.string.achievement_daily_mix_title,
                descriptionResId = R.string.achievement_daily_mix_desc,
                iconName = "Shuffle",
                condition = { _, ctx -> ctx.uniqueExercisesToday >= 3 },
            ),
        )
        add(
            AchievementDefinition(
                id = "variety_full_rotation",
                category = AchievementCategory.VARIETY,
                titleResId = R.string.achievement_full_rotation_title,
                descriptionResId = R.string.achievement_full_rotation_desc,
                iconName = "Autorenew",
                condition = { stats, _ -> stats.uniqueExercisesUsed.size >= 6 },
            ),
        )
        add(
            AchievementDefinition(
                id = "variety_custom_creator",
                category = AchievementCategory.VARIETY,
                titleResId = R.string.achievement_custom_creator_title,
                descriptionResId = R.string.achievement_custom_creator_desc,
                iconName = "Create",
                condition = { stats, _ -> stats.hasCreatedCustomExercise },
            ),
        )

        // Daily challenges
        dailyBreaks(3, R.string.achievement_daily_3_title, R.string.achievement_daily_3_desc)
        dailyBreaks(5, R.string.achievement_daily_5_title, R.string.achievement_daily_5_desc)
        dailyBreaks(10, R.string.achievement_daily_10_title, R.string.achievement_daily_10_desc)

        add(
            AchievementDefinition(
                id = "daily_early_bird",
                category = AchievementCategory.DAILY_CHALLENGES,
                titleResId = R.string.achievement_early_bird_title,
                descriptionResId = R.string.achievement_early_bird_desc,
                iconName = "WbSunny",
                condition = { _, ctx -> ctx.hourOfDay < 8 },
            ),
        )
        add(
            AchievementDefinition(
                id = "daily_night_owl",
                category = AchievementCategory.DAILY_CHALLENGES,
                titleResId = R.string.achievement_night_owl_title,
                descriptionResId = R.string.achievement_night_owl_desc,
                iconName = "NightsStay",
                condition = { _, ctx -> ctx.hourOfDay >= 20 },
            ),
        )
        add(
            AchievementDefinition(
                id = "daily_lunch_hero",
                category = AchievementCategory.DAILY_CHALLENGES,
                titleResId = R.string.achievement_lunch_hero_title,
                descriptionResId = R.string.achievement_lunch_hero_desc,
                iconName = "LunchDining",
                condition = { _, ctx -> ctx.hourOfDay == 12 },
            ),
        )

        // Fun / seasonal
        add(
            AchievementDefinition(
                id = "fun_new_year",
                category = AchievementCategory.FUN_SEASONAL,
                titleResId = R.string.achievement_new_year_title,
                descriptionResId = R.string.achievement_new_year_desc,
                iconName = "Celebration",
                condition = { _, ctx -> ctx.isNewYear },
            ),
        )
        add(
            AchievementDefinition(
                id = "fun_weekend_warrior",
                category = AchievementCategory.FUN_SEASONAL,
                titleResId = R.string.achievement_weekend_warrior_title,
                descriptionResId = R.string.achievement_weekend_warrior_desc,
                iconName = "Weekend",
                condition = { _, ctx -> ctx.dayOfWeek in 6..7 },
            ),
        )
        add(
            AchievementDefinition(
                id = "fun_comeback",
                category = AchievementCategory.FUN_SEASONAL,
                titleResId = R.string.achievement_comeback_title,
                descriptionResId = R.string.achievement_comeback_desc,
                iconName = "Replay",
                condition = { _, ctx -> (ctx.daysSinceLastBreak ?: 0) >= 7 },
            ),
        )

        // Exercise mastery
        masteryMilestone(10, R.string.achievement_mastery_10_title, R.string.achievement_mastery_10_desc)
        masteryMilestone(50, R.string.achievement_mastery_50_title, R.string.achievement_mastery_50_desc)
        masteryMilestone(100, R.string.achievement_mastery_100_title, R.string.achievement_mastery_100_desc)
    }

    fun byId(id: String): AchievementDefinition? = all.find { it.id == id }

    private fun MutableList<AchievementDefinition>.breakMilestone(
        target: Int,
        titleResId: Int,
        descriptionResId: Int,
    ) {
        add(
            AchievementDefinition(
                id = "breaks_$target",
                category = AchievementCategory.BREAK_MILESTONES,
                titleResId = titleResId,
                descriptionResId = descriptionResId,
                iconName = "EmojiEvents",
                condition = { stats, _ -> stats.totalBreaksAllTime >= target },
                progressExtractor = { stats -> stats.totalBreaksAllTime to target },
            ),
        )
    }

    private fun MutableList<AchievementDefinition>.streakMilestone(
        target: Int,
        titleResId: Int,
        descriptionResId: Int,
    ) {
        add(
            AchievementDefinition(
                id = "streak_$target",
                category = AchievementCategory.STREAK_MILESTONES,
                titleResId = titleResId,
                descriptionResId = descriptionResId,
                iconName = "LocalFireDepartment",
                // Intentional OR: unlocks based on best-ever streak so users keep credit after streak resets
                condition = { stats, _ ->
                    stats.currentStreakDays >= target || stats.longestStreakDays >= target
                },
                progressExtractor = { stats ->
                    maxOf(stats.currentStreakDays, stats.longestStreakDays) to target
                },
            ),
        )
    }

    private fun MutableList<AchievementDefinition>.repMilestone(
        target: Int,
        titleResId: Int,
        descriptionResId: Int,
    ) {
        add(
            AchievementDefinition(
                id = "reps_$target",
                category = AchievementCategory.REP_MILESTONES,
                titleResId = titleResId,
                descriptionResId = descriptionResId,
                iconName = "FitnessCenter",
                condition = { stats, _ -> stats.totalRepsAllTime >= target },
                progressExtractor = { stats -> stats.totalRepsAllTime to target },
            ),
        )
    }

    private fun MutableList<AchievementDefinition>.dailyBreaks(
        target: Int,
        titleResId: Int,
        descriptionResId: Int,
    ) {
        add(
            AchievementDefinition(
                id = "daily_breaks_$target",
                category = AchievementCategory.DAILY_CHALLENGES,
                titleResId = titleResId,
                descriptionResId = descriptionResId,
                iconName = "Today",
                condition = { _, ctx -> ctx.breaksToday >= target },
            ),
        )
    }

    private fun MutableList<AchievementDefinition>.masteryMilestone(
        target: Int,
        titleResId: Int,
        descriptionResId: Int,
    ) {
        add(
            AchievementDefinition(
                id = "mastery_any_$target",
                category = AchievementCategory.EXERCISE_MASTERY,
                titleResId = titleResId,
                descriptionResId = descriptionResId,
                iconName = "Star",
                condition = { stats, _ ->
                    stats.perExerciseCounts.any { (_, count) -> count >= target }
                },
                progressExtractor = { stats ->
                    (stats.perExerciseCounts.values.maxOrNull() ?: 0) to target
                },
            ),
        )
    }
}
