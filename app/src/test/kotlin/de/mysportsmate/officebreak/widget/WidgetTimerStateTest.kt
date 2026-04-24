package de.mysportsmate.officebreak.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetTimerStateTest {

    private val now = 1_000_000L

    @Test
    fun `computeEndRealtime returns zero when status is not running`() {
        val result = WidgetTimerState.computeEndRealtime("expired", 60, now)

        assertEquals(0L, result)
    }

    @Test
    fun `computeEndRealtime returns zero when running with zero remaining seconds`() {
        val result = WidgetTimerState.computeEndRealtime("running", 0, now)

        assertEquals(0L, result)
    }

    @Test
    fun `computeEndRealtime returns zero when running with negative remaining seconds`() {
        val result = WidgetTimerState.computeEndRealtime("running", -5, now)

        assertEquals(0L, result)
    }

    @Test
    fun `computeEndRealtime returns now plus remaining milliseconds when running`() {
        val result = WidgetTimerState.computeEndRealtime("running", 60, now)

        assertEquals(now + 60_000L, result)
    }

    @Test
    fun `resolveDisplay shows idle when no state stored`() {
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "idle",
            endRealtime = 0L,
            storedRemaining = 0L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("idle", 0L), display)
    }

    @Test
    fun `resolveDisplay shows running with live countdown when end is in the future`() {
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "running",
            endRealtime = now + 30_000L,
            storedRemaining = 0L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("running", 30L), display)
    }

    @Test
    fun `resolveDisplay falls back to idle when running but endRealtime is zero`() {
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "running",
            endRealtime = 0L,
            storedRemaining = 0L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("idle", 0L), display)
    }

    @Test
    fun `resolveDisplay falls back to idle when running but end time has passed`() {
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "running",
            endRealtime = now - 1L,
            storedRemaining = 0L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("idle", 0L), display)
    }

    @Test
    fun `resolveDisplay shows paused with stored remaining seconds`() {
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "paused",
            endRealtime = 0L,
            storedRemaining = 42L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("paused", 42L), display)
    }

    @Test
    fun `resolveDisplay falls back to storedStatus when paused has zero remaining`() {
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "paused",
            endRealtime = 0L,
            storedRemaining = 0L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("paused", 0L), display)
    }

    @Test
    fun `resolveDisplay shows expired status with zero remaining on expiry push`() {
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "expired",
            endRealtime = 0L,
            storedRemaining = 0L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("expired", 0L), display)
    }

    @Test
    fun `resolveDisplay shows work_ended status when shift ends`() {
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "work_ended",
            endRealtime = 0L,
            storedRemaining = 0L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("work_ended", 0L), display)
    }

    @Test
    fun `expired status is not overridden by running fallback rule`() {
        // Regression: the "running + remainingMs <= 0 -> idle" rule must not
        // apply to other statuses. Previously a race could land a stale running
        // push after the expired push, showing idle where the user expected
        // "Break time!". With correct storedStatus=expired the widget stays expired.
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "expired",
            endRealtime = now - 5_000L,
            storedRemaining = 0L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("expired", 0L), display)
    }

    @Test
    fun `running countdown truncates sub-second remainder`() {
        val display = WidgetTimerState.resolveDisplay(
            storedStatus = "running",
            endRealtime = now + 59_500L,
            storedRemaining = 0L,
            nowRealtime = now,
        )

        assertEquals(WidgetTimerDisplay("running", 59L), display)
    }
}
