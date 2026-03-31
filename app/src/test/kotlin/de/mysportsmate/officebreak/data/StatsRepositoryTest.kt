package de.mysportsmate.officebreak.data

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatsRepositoryTest {

    private lateinit var dataStore: FakeDataStore
    private lateinit var repository: StatsRepository

    private val fixedClock = Clock.fixed(
        Instant.parse("2026-03-30T12:00:00Z"),
        ZoneId.of("UTC"),
    )

    @Before
    fun setUp() {
        dataStore = FakeDataStore()
        repository = StatsRepository(dataStore = dataStore, clock = fixedClock)
    }

    private fun record(
        name: String = "Push Ups",
        reps: Int = 10,
        dateString: String = "2026-03-30",
        timestampMillis: Long = 1711800000000L,
    ) = BreakRecord(
        exerciseName = name,
        reps = reps,
        timestampMillis = timestampMillis,
        dateString = dateString,
    )

    @Test
    fun `trackingEnabled emits default when empty`() = runTest {
        repository.trackingEnabled.test {
            assertEquals(true, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `setTrackingEnabled persists and re-emits`() = runTest {
        repository.trackingEnabled.test {
            assertEquals(true, awaitItem())
            repository.setTrackingEnabled(false)
            assertEquals(false, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `statsSnapshot emits default when empty`() = runTest {
        repository.statsSnapshot.test {
            assertEquals(StatsSnapshot(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `recordBreak updates snapshot`() = runTest {
        repository.recordBreak(record())

        repository.statsSnapshot.test {
            val snapshot = awaitItem()
            assertEquals(1, snapshot.totalBreaksAllTime)
            assertEquals(10, snapshot.totalRepsAllTime)
            assertEquals(1, snapshot.currentStreakDays)
            assertEquals(1, snapshot.longestStreakDays)
            assertEquals("2026-03-30", snapshot.lastBreakDateString)
            assertEquals("2026-03-30", snapshot.firstBreakDateString)
            assertEquals(mapOf("Push Ups" to 1), snapshot.perExerciseCounts)
            assertEquals(mapOf("Push Ups" to 10), snapshot.perExerciseReps)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `recordBreak appends to break records`() = runTest {
        repository.recordBreak(record())
        repository.recordBreak(record(name = "Squats", reps = 15))

        repository.breakRecords.test {
            val records = awaitItem()
            assertEquals(2, records.size)
            assertEquals("Push Ups", records[0].exerciseName)
            assertEquals("Squats", records[1].exerciseName)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `streak increments on consecutive days`() = runTest {
        repository.recordBreak(record(dateString = "2026-03-29"))
        repository.recordBreak(record(dateString = "2026-03-30"))

        repository.statsSnapshot.test {
            val snapshot = awaitItem()
            assertEquals(2, snapshot.currentStreakDays)
            assertEquals(2, snapshot.longestStreakDays)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `streak resets on non-consecutive days`() = runTest {
        repository.recordBreak(record(dateString = "2026-03-27"))
        repository.recordBreak(record(dateString = "2026-03-30"))

        repository.statsSnapshot.test {
            val snapshot = awaitItem()
            assertEquals(1, snapshot.currentStreakDays)
            assertEquals(1, snapshot.longestStreakDays)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `same day break does not change streak`() = runTest {
        repository.recordBreak(record(dateString = "2026-03-30"))
        repository.recordBreak(record(dateString = "2026-03-30"))

        repository.statsSnapshot.test {
            val snapshot = awaitItem()
            assertEquals(1, snapshot.currentStreakDays)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `recordBreak returns newly unlocked achievements`() = runTest {
        val unlocked = repository.recordBreak(record())
        assertTrue(unlocked.any { it.id == "breaks_1" })
    }

    @Test
    fun `recordBreak does not re-trigger unlocked achievements`() = runTest {
        repository.recordBreak(record())
        val secondUnlocked = repository.recordBreak(record())
        assertTrue(secondUnlocked.none { it.id == "breaks_1" })
    }

    @Test
    fun `resetAllStats clears everything`() = runTest {
        repository.recordBreak(record())
        repository.resetAllStats()

        repository.statsSnapshot.test {
            assertEquals(StatsSnapshot(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        repository.breakRecords.test {
            assertEquals(emptyList<BreakRecord>(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        repository.achievementState.test {
            assertEquals(AchievementState(), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `markCustomExerciseCreated sets flag`() = runTest {
        repository.markCustomExerciseCreated()

        repository.statsSnapshot.test {
            val snapshot = awaitItem()
            assertTrue(snapshot.hasCreatedCustomExercise)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `daily compaction moves old records to aggregates`() = runTest {
        // Record a break 100 days ago (older than 90-day window)
        val oldDate = LocalDate.now(fixedClock).minusDays(100).toString()
        repository.recordBreak(record(dateString = oldDate))

        // Record a recent break to trigger compaction
        repository.recordBreak(record(dateString = "2026-03-30"))

        repository.breakRecords.test {
            val records = awaitItem()
            // Old record should have been compacted
            assertTrue(records.none { it.dateString == oldDate })
            assertEquals(1, records.size)
            cancelAndConsumeRemainingEvents()
        }

        repository.dailyAggregates.test {
            val aggregates = awaitItem()
            assertTrue(aggregates.any { it.dateString == oldDate })
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `multiple breaks same day do not increment streak`() = runTest {
        repository.recordBreak(record(dateString = "2026-03-30"))
        repository.recordBreak(record(dateString = "2026-03-30", name = "Squats"))
        repository.recordBreak(record(dateString = "2026-03-30", name = "Lunges"))

        repository.statsSnapshot.test {
            val snapshot = awaitItem()
            assertEquals(1, snapshot.currentStreakDays)
            assertEquals(3, snapshot.totalBreaksAllTime)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `streak resets after two day gap`() = runTest {
        repository.recordBreak(record(dateString = "2026-03-27"))
        repository.recordBreak(record(dateString = "2026-03-28"))
        // Skip 29, break on 30 → gap of 2 days, streak resets
        repository.recordBreak(record(dateString = "2026-03-30"))

        repository.statsSnapshot.test {
            val snapshot = awaitItem()
            assertEquals(1, snapshot.currentStreakDays)
            assertEquals(2, snapshot.longestStreakDays)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `per exercise reps aggregate across multiple breaks`() = runTest {
        repository.recordBreak(record(name = "Push Ups", reps = 10))
        repository.recordBreak(record(name = "Push Ups", reps = 15))
        repository.recordBreak(record(name = "Squats", reps = 20))

        repository.statsSnapshot.test {
            val snapshot = awaitItem()
            assertEquals(mapOf("Push Ups" to 25, "Squats" to 20), snapshot.perExerciseReps)
            assertEquals(mapOf("Push Ups" to 2, "Squats" to 1), snapshot.perExerciseCounts)
            assertEquals(45, snapshot.totalRepsAllTime)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `yearly compaction moves old aggregates to yearly`() = runTest {
        // Use a clock after April 1st so yearly compaction runs
        val aprilClock = Clock.fixed(
            Instant.parse("2026-04-15T12:00:00Z"),
            ZoneId.of("UTC"),
        )
        val aprilRepo = StatsRepository(dataStore = dataStore, clock = aprilClock)

        // Record a break from 2024 (older than cutoff year 2025)
        val oldDate = "2024-06-15"
        aprilRepo.recordBreak(record(dateString = oldDate))
        // Trigger second record to compact the old one to daily aggregate
        aprilRepo.recordBreak(record(dateString = "2026-04-15"))

        // cutoffYear = 2026 - 1 = 2025, so 2024 aggregates should be compacted
        aprilRepo.runYearlyCompaction()

        aprilRepo.yearlyAggregates.test {
            val yearly = awaitItem()
            assertTrue(yearly.any { it.year == 2024 })
            cancelAndConsumeRemainingEvents()
        }

        aprilRepo.dailyAggregates.test {
            val daily = awaitItem()
            assertTrue(daily.none { it.dateString == oldDate })
            cancelAndConsumeRemainingEvents()
        }
    }
}
