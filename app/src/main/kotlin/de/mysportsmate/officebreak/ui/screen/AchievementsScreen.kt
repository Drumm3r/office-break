package de.mysportsmate.officebreak.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.AchievementCategory
import de.mysportsmate.officebreak.data.AchievementDefinition
import de.mysportsmate.officebreak.data.AchievementRegistry
import de.mysportsmate.officebreak.data.AchievementState
import de.mysportsmate.officebreak.data.StatsSnapshot
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    snapshot: StatsSnapshot,
    achievementState: AchievementState,
    onBack: () -> Unit,
) {
    val grouped = AchievementRegistry.all.groupBy { it.category }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.achievements_title_with_count,
                            achievementState.unlockedIds.size,
                            AchievementRegistry.all.size,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.onboarding_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            for ((category, achievements) in grouped) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = categoryTitle(category),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                items(achievements) { achievement ->
                    AchievementItem(
                        achievement = achievement,
                        isUnlocked = achievement.id in achievementState.unlockedIds,
                        unlockTimestamp = achievementState.unlockTimestamps[achievement.id],
                        snapshot = snapshot,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: AchievementDefinition,
    isUnlocked: Boolean,
    unlockTimestamp: Long?,
    snapshot: StatsSnapshot,
) {
    val title = stringResource(achievement.titleResId)
    val description = stringResource(achievement.descriptionResId)

    val alpha = if (isUnlocked) 1f else 0.5f
    val icon = if (isUnlocked) iconForName(achievement.iconName) else Icons.Default.Lock

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isUnlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(40.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )

                if (isUnlocked && unlockTimestamp != null) {
                    val dateStr = Instant.ofEpochMilli(unlockTimestamp)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                if (!isUnlocked && achievement.progressExtractor != null) {
                    val (current, target) = achievement.progressExtractor.invoke(snapshot)
                    val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp),
                        )
                        Text(
                            text = "$current / $target",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryTitle(category: AchievementCategory): String {
    return stringResource(
        when (category) {
            AchievementCategory.BREAK_MILESTONES -> R.string.achievement_category_breaks
            AchievementCategory.STREAK_MILESTONES -> R.string.achievement_category_streaks
            AchievementCategory.REP_MILESTONES -> R.string.achievement_category_reps
            AchievementCategory.VARIETY -> R.string.achievement_category_variety
            AchievementCategory.DAILY_CHALLENGES -> R.string.achievement_category_daily
            AchievementCategory.FUN_SEASONAL -> R.string.achievement_category_fun
            AchievementCategory.EXERCISE_MASTERY -> R.string.achievement_category_mastery
        },
    )
}

private fun iconForName(name: String): ImageVector {
    return when (name) {
        "EmojiEvents" -> Icons.Default.EmojiEvents
        "LocalFireDepartment" -> Icons.Default.LocalFireDepartment
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "Diversity3" -> Icons.Default.AutoAwesome
        "Shuffle" -> Icons.Default.Shuffle
        "Autorenew" -> Icons.Default.Autorenew
        "Create" -> Icons.Default.Create
        "Today" -> Icons.Default.Today
        "WbSunny" -> Icons.Default.WbSunny
        "NightsStay" -> Icons.Default.NightsStay
        "LunchDining" -> Icons.Default.Today
        "Celebration" -> Icons.Default.Celebration
        "Weekend" -> Icons.Default.Weekend
        "Replay" -> Icons.Default.Replay
        "Star" -> Icons.Default.Star
        else -> Icons.Default.EmojiEvents
    }
}


@Preview(showBackground = true)
@Composable
private fun AchievementsScreenPreview() {
    OfficeBreakTheme {
        AchievementsScreen(
            snapshot = StatsSnapshot(
                totalBreaksAllTime = 15,
                totalRepsAllTime = 200,
                currentStreakDays = 3,
                longestStreakDays = 5,
            ),
            achievementState = AchievementState(
                unlockedIds = setOf("breaks_1", "breaks_10"),
                unlockTimestamps = mapOf(
                    "breaks_1" to System.currentTimeMillis(),
                    "breaks_10" to System.currentTimeMillis(),
                ),
            ),
            onBack = {},
        )
    }
}
