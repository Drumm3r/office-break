package de.mysportsmate.officebreak.data

object AchievementRegistry {

    val all: List<AchievementDefinition> = buildList {
        // Break milestones
        breakMilestone(1, "achievement_breaks_1_title", "achievement_breaks_1_desc")
        breakMilestone(10, "achievement_breaks_10_title", "achievement_breaks_10_desc")
        breakMilestone(25, "achievement_breaks_25_title", "achievement_breaks_25_desc")
        breakMilestone(50, "achievement_breaks_50_title", "achievement_breaks_50_desc")
        breakMilestone(100, "achievement_breaks_100_title", "achievement_breaks_100_desc")
        breakMilestone(500, "achievement_breaks_500_title", "achievement_breaks_500_desc")
        breakMilestone(1000, "achievement_breaks_1000_title", "achievement_breaks_1000_desc")

        // Streak milestones
        streakMilestone(3, "achievement_streak_3_title", "achievement_streak_3_desc")
        streakMilestone(7, "achievement_streak_7_title", "achievement_streak_7_desc")
        streakMilestone(14, "achievement_streak_14_title", "achievement_streak_14_desc")
        streakMilestone(30, "achievement_streak_30_title", "achievement_streak_30_desc")
        streakMilestone(60, "achievement_streak_60_title", "achievement_streak_60_desc")
        streakMilestone(100, "achievement_streak_100_title", "achievement_streak_100_desc")
        streakMilestone(365, "achievement_streak_365_title", "achievement_streak_365_desc")

        // Rep milestones
        repMilestone(100, "achievement_reps_100_title", "achievement_reps_100_desc")
        repMilestone(1000, "achievement_reps_1000_title", "achievement_reps_1000_desc")
        repMilestone(5000, "achievement_reps_5000_title", "achievement_reps_5000_desc")
        repMilestone(10000, "achievement_reps_10000_title", "achievement_reps_10000_desc")

        // Variety
        add(
            AchievementDefinition(
                id = "variety_all_rounder",
                category = AchievementCategory.VARIETY,
                titleResKey = "achievement_all_rounder_title",
                descriptionResKey = "achievement_all_rounder_desc",
                iconName = "Diversity3",
                condition = { stats, _ -> stats.uniqueExercisesUsed.size >= 3 },
            ),
        )
        add(
            AchievementDefinition(
                id = "variety_daily_mix",
                category = AchievementCategory.VARIETY,
                titleResKey = "achievement_daily_mix_title",
                descriptionResKey = "achievement_daily_mix_desc",
                iconName = "Shuffle",
                condition = { _, ctx -> ctx.uniqueExercisesToday >= 3 },
            ),
        )
        add(
            AchievementDefinition(
                id = "variety_full_rotation",
                category = AchievementCategory.VARIETY,
                titleResKey = "achievement_full_rotation_title",
                descriptionResKey = "achievement_full_rotation_desc",
                iconName = "Autorenew",
                condition = { stats, _ -> stats.uniqueExercisesUsed.size >= 6 },
            ),
        )
        add(
            AchievementDefinition(
                id = "variety_custom_creator",
                category = AchievementCategory.VARIETY,
                titleResKey = "achievement_custom_creator_title",
                descriptionResKey = "achievement_custom_creator_desc",
                iconName = "Create",
                condition = { stats, _ -> stats.hasCreatedCustomExercise },
            ),
        )

        // Daily challenges
        dailyBreaks(3, "achievement_daily_3_title", "achievement_daily_3_desc")
        dailyBreaks(5, "achievement_daily_5_title", "achievement_daily_5_desc")
        dailyBreaks(10, "achievement_daily_10_title", "achievement_daily_10_desc")

        add(
            AchievementDefinition(
                id = "daily_early_bird",
                category = AchievementCategory.DAILY_CHALLENGES,
                titleResKey = "achievement_early_bird_title",
                descriptionResKey = "achievement_early_bird_desc",
                iconName = "WbSunny",
                condition = { _, ctx -> ctx.hourOfDay < 8 },
            ),
        )
        add(
            AchievementDefinition(
                id = "daily_night_owl",
                category = AchievementCategory.DAILY_CHALLENGES,
                titleResKey = "achievement_night_owl_title",
                descriptionResKey = "achievement_night_owl_desc",
                iconName = "NightsStay",
                condition = { _, ctx -> ctx.hourOfDay >= 20 },
            ),
        )
        add(
            AchievementDefinition(
                id = "daily_lunch_hero",
                category = AchievementCategory.DAILY_CHALLENGES,
                titleResKey = "achievement_lunch_hero_title",
                descriptionResKey = "achievement_lunch_hero_desc",
                iconName = "LunchDining",
                condition = { _, ctx -> ctx.hourOfDay in 12..12 },
            ),
        )

        // Fun / seasonal
        add(
            AchievementDefinition(
                id = "fun_new_year",
                category = AchievementCategory.FUN_SEASONAL,
                titleResKey = "achievement_new_year_title",
                descriptionResKey = "achievement_new_year_desc",
                iconName = "Celebration",
                condition = { _, ctx -> ctx.isNewYear },
            ),
        )
        add(
            AchievementDefinition(
                id = "fun_weekend_warrior",
                category = AchievementCategory.FUN_SEASONAL,
                titleResKey = "achievement_weekend_warrior_title",
                descriptionResKey = "achievement_weekend_warrior_desc",
                iconName = "Weekend",
                condition = { _, ctx -> ctx.dayOfWeek in 6..7 },
            ),
        )
        add(
            AchievementDefinition(
                id = "fun_comeback",
                category = AchievementCategory.FUN_SEASONAL,
                titleResKey = "achievement_comeback_title",
                descriptionResKey = "achievement_comeback_desc",
                iconName = "Replay",
                condition = { _, ctx -> (ctx.daysSinceLastBreak ?: 0) >= 7 },
            ),
        )

        // Exercise mastery
        masteryMilestone(10, "achievement_mastery_10_title", "achievement_mastery_10_desc")
        masteryMilestone(50, "achievement_mastery_50_title", "achievement_mastery_50_desc")
        masteryMilestone(100, "achievement_mastery_100_title", "achievement_mastery_100_desc")
    }

    fun byId(id: String): AchievementDefinition? = all.find { it.id == id }

    private fun MutableList<AchievementDefinition>.breakMilestone(
        target: Int,
        titleResKey: String,
        descriptionResKey: String,
    ) {
        add(
            AchievementDefinition(
                id = "breaks_$target",
                category = AchievementCategory.BREAK_MILESTONES,
                titleResKey = titleResKey,
                descriptionResKey = descriptionResKey,
                iconName = "EmojiEvents",
                condition = { stats, _ -> stats.totalBreaksAllTime >= target },
                progressExtractor = { stats -> stats.totalBreaksAllTime to target },
            ),
        )
    }

    private fun MutableList<AchievementDefinition>.streakMilestone(
        target: Int,
        titleResKey: String,
        descriptionResKey: String,
    ) {
        add(
            AchievementDefinition(
                id = "streak_$target",
                category = AchievementCategory.STREAK_MILESTONES,
                titleResKey = titleResKey,
                descriptionResKey = descriptionResKey,
                iconName = "LocalFireDepartment",
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
        titleResKey: String,
        descriptionResKey: String,
    ) {
        add(
            AchievementDefinition(
                id = "reps_$target",
                category = AchievementCategory.REP_MILESTONES,
                titleResKey = titleResKey,
                descriptionResKey = descriptionResKey,
                iconName = "FitnessCenter",
                condition = { stats, _ -> stats.totalRepsAllTime >= target },
                progressExtractor = { stats -> stats.totalRepsAllTime to target },
            ),
        )
    }

    private fun MutableList<AchievementDefinition>.dailyBreaks(
        target: Int,
        titleResKey: String,
        descriptionResKey: String,
    ) {
        add(
            AchievementDefinition(
                id = "daily_breaks_$target",
                category = AchievementCategory.DAILY_CHALLENGES,
                titleResKey = titleResKey,
                descriptionResKey = descriptionResKey,
                iconName = "Today",
                condition = { _, ctx -> ctx.breaksToday >= target },
            ),
        )
    }

    private fun MutableList<AchievementDefinition>.masteryMilestone(
        target: Int,
        titleResKey: String,
        descriptionResKey: String,
    ) {
        add(
            AchievementDefinition(
                id = "mastery_any_$target",
                category = AchievementCategory.EXERCISE_MASTERY,
                titleResKey = titleResKey,
                descriptionResKey = descriptionResKey,
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
