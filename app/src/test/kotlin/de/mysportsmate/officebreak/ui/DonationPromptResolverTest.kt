package de.mysportsmate.officebreak.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class DonationPromptResolverTest {

    private val installedAt = 1_700_000_000_000L
    private val initialDays = 21L
    private val snoozeDays = 60L

    private fun daysFromInstall(days: Long): Long = installedAt + TimeUnit.DAYS.toMillis(days)

    private fun shouldShow(
        installTimestamp: Long = installedAt,
        lastShown: Long = 0L,
        dismissed: Boolean = false,
        timerActive: Boolean = false,
        nowMillis: Long,
    ): Boolean = DonationPromptResolver.shouldShow(
        installTimestamp = installTimestamp,
        lastShown = lastShown,
        dismissed = dismissed,
        timerActive = timerActive,
        nowMillis = nowMillis,
        initialThresholdDays = initialDays,
        snoozeDays = snoozeDays,
    )

    @Test
    fun `fresh install with no history does not show at day 0`() {
        assertFalse(shouldShow(nowMillis = installedAt))
    }

    @Test
    fun `does not show before 21-day threshold`() {
        assertFalse(shouldShow(nowMillis = daysFromInstall(20)))
    }

    @Test
    fun `shows at 21 days when idle and never shown`() {
        assertTrue(shouldShow(nowMillis = daysFromInstall(21)))
    }

    @Test
    fun `does not show when timer is active`() {
        assertFalse(shouldShow(timerActive = true, nowMillis = daysFromInstall(21)))
    }

    @Test
    fun `does not show when dismissed`() {
        assertFalse(shouldShow(dismissed = true, nowMillis = daysFromInstall(365)))
    }

    @Test
    fun `snooze blocks popup within 60 days of last shown`() {
        assertFalse(
            shouldShow(
                lastShown = daysFromInstall(21),
                nowMillis = daysFromInstall(50),
            ),
        )
    }

    @Test
    fun `popup returns after snooze period elapsed`() {
        assertTrue(
            shouldShow(
                lastShown = daysFromInstall(21),
                nowMillis = daysFromInstall(85),
            ),
        )
    }

    @Test
    fun `uninitialized install timestamp does not show`() {
        assertFalse(shouldShow(installTimestamp = 0L, nowMillis = daysFromInstall(365)))
    }

    @Test
    fun `negative drift where now is before install does not show`() {
        assertFalse(shouldShow(nowMillis = installedAt - TimeUnit.DAYS.toMillis(5)))
    }

    @Test
    fun `custom thresholds are respected`() {
        assertTrue(
            DonationPromptResolver.shouldShow(
                installTimestamp = installedAt,
                lastShown = 0L,
                dismissed = false,
                timerActive = false,
                nowMillis = daysFromInstall(7),
                initialThresholdDays = 7L,
                snoozeDays = 30L,
            ),
        )
    }
}
