package de.mysportsmate.officebreak.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.BreakRecord
import de.mysportsmate.officebreak.data.ExerciseConfig
import de.mysportsmate.officebreak.data.StatsSnapshot
import de.mysportsmate.officebreak.ui.theme.OfficeBreakTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    snapshot: StatsSnapshot,
    breakRecords: List<BreakRecord>,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (snapshot.totalBreaksAllTime == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Overview section
            Text(
                text = stringResource(R.string.stats_overview),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow(
                        label = stringResource(R.string.stats_total_breaks),
                        value = snapshot.totalBreaksAllTime.toString(),
                    )
                    StatRow(
                        label = stringResource(R.string.stats_total_reps),
                        value = snapshot.totalRepsAllTime.toString(),
                    )
                    StatRow(
                        label = stringResource(R.string.stats_current_streak),
                        value = stringResource(R.string.stats_days, snapshot.currentStreakDays),
                    )
                    StatRow(
                        label = stringResource(R.string.stats_longest_streak),
                        value = stringResource(R.string.stats_days, snapshot.longestStreakDays),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // This week section
            val today = LocalDate.now()
            val weekStart = today.minusDays(6)
            val thisWeekRecords = breakRecords.filter {
                try {
                    val date = LocalDate.parse(it.dateString)
                    !date.isBefore(weekStart) && !date.isAfter(today)
                } catch (_: Exception) {
                    false
                }
            }
            val lastWeekStart = today.minusDays(13)
            val lastWeekEnd = today.minusDays(7)
            val lastWeekRecords = breakRecords.filter {
                try {
                    val date = LocalDate.parse(it.dateString)
                    !date.isBefore(lastWeekStart) && !date.isAfter(lastWeekEnd)
                } catch (_: Exception) {
                    false
                }
            }

            val breaksThisWeek = thisWeekRecords.size
            val repsThisWeek = thisWeekRecords.sumOf { it.reps }
            val breaksAvgPerDay = if (breaksThisWeek > 0) "%.1f".format(breaksThisWeek.toFloat() / 7f) else "0"
            val repsAvgPerDay = if (repsThisWeek > 0) "%.1f".format(repsThisWeek.toFloat() / 7f) else "0"

            val breaksLastWeek = lastWeekRecords.size
            val repsLastWeek = lastWeekRecords.sumOf { it.reps }
            val breaksAvgPerDayLastWeek = if (breaksLastWeek > 0) "%.1f".format(breaksLastWeek.toFloat() / 7f) else "0"
            val repsAvgPerDayLastWeek = if (repsLastWeek > 0) "%.1f".format(repsLastWeek.toFloat() / 7f) else "0"

            Text(
                text = stringResource(R.string.stats_this_week),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow(
                        label = stringResource(R.string.stats_breaks_this_week),
                        value = stringResource(
                            R.string.stats_last_week_format,
                            breaksThisWeek.toString(),
                            breaksLastWeek.toString(),
                        ),
                    )
                    StatRow(
                        label = stringResource(R.string.stats_average_per_day),
                        value = stringResource(
                            R.string.stats_last_week_format,
                            breaksAvgPerDay,
                            breaksAvgPerDayLastWeek,
                        ),
                    )
                    StatRow(
                        label = stringResource(R.string.stats_reps_this_week),
                        value = stringResource(
                            R.string.stats_last_week_format,
                            repsThisWeek.toString(),
                            repsLastWeek.toString(),
                        ),
                    )
                    StatRow(
                        label = stringResource(R.string.stats_reps_average_per_day),
                        value = stringResource(
                            R.string.stats_last_week_format,
                            repsAvgPerDay,
                            repsAvgPerDayLastWeek,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Favorite exercise
            val context = LocalContext.current
            if (snapshot.perExerciseCounts.isNotEmpty()) {
                val favorite = snapshot.perExerciseCounts.maxByOrNull { it.value }

                Text(
                    text = stringResource(R.string.stats_favorite_exercise),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (favorite != null) {
                            StatRow(
                                label = ExerciseConfig.resolveDisplayName(context, favorite.key),
                                value = stringResource(R.string.stats_times_format, favorite.value),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Exercise distribution
                Text(
                    text = stringResource(R.string.stats_exercise_distribution),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        snapshot.perExerciseCounts
                            .entries
                            .sortedByDescending { it.value }
                            .forEach { (name, count) ->
                                val reps = snapshot.perExerciseReps[name] ?: 0
                                StatRow(
                                    label = ExerciseConfig.resolveDisplayName(context, name),
                                    value = stringResource(R.string.stats_exercise_detail, count, reps),
                                )
                            }
                    }
                }
            }

            // Recent activity (last 7 days, only days with activity)
            val recordsByDay = breakRecords.groupBy { it.dateString }
            val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            val recentDays = (0..6)
                .map { today.minusDays(it.toLong()) }
                .filter { recordsByDay.containsKey(it.toString()) }

            if (recentDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.stats_recent_activity),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        for (date in recentDays) {
                            val count = recordsByDay[date.toString()]?.size ?: 0
                            StatRow(
                                label = date.format(dateFormatter),
                                value = stringResource(R.string.stats_breaks_count, count),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    OfficeBreakTheme {
        val today = LocalDate.now()
        val sampleRecords = listOf(
            BreakRecord("Push Ups", 10, System.currentTimeMillis(), today.toString()),
            BreakRecord("Squats", 15, System.currentTimeMillis(), today.minusDays(1).toString()),
            BreakRecord("Push Ups", 12, System.currentTimeMillis(), today.minusDays(2).toString()),
            BreakRecord("Lunges", 8, System.currentTimeMillis(), today.minusDays(8).toString()),
            BreakRecord("Push Ups", 10, System.currentTimeMillis(), today.minusDays(9).toString()),
        )
        StatsScreen(
            snapshot = StatsSnapshot(
                totalBreaksAllTime = 42,
                totalRepsAllTime = 520,
                currentStreakDays = 5,
                longestStreakDays = 12,
                perExerciseCounts = mapOf("Push Ups" to 15, "Squats" to 12, "Lunges" to 10),
                perExerciseReps = mapOf("Push Ups" to 180, "Squats" to 150, "Lunges" to 120),
            ),
            breakRecords = sampleRecords,
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenEmptyPreview() {
    OfficeBreakTheme {
        StatsScreen(
            snapshot = StatsSnapshot(),
            breakRecords = emptyList(),
            onBack = {},
        )
    }
}
