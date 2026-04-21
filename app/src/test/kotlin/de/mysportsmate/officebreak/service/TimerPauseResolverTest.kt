package de.mysportsmate.officebreak.service

import de.mysportsmate.officebreak.data.DaySchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class TimerPauseResolverTest {

    private val workDay = DaySchedule(
        enabled = true,
        linked = false,
        workStartHour = 8, workStartMinute = 0,
        workEndHour = 17, workEndMinute = 0,
        lunchStartHour = 12, lunchStartMinute = 0,
        lunchEndHour = 13, lunchEndMinute = 0,
    )

    @Test
    fun `null schedule always continues`() {
        val decision = TimerPauseResolver.decide(LocalTime.of(9, 30), null)
        assertEquals(TimerTickDecision.Continue, decision)
    }

    @Test
    fun `during work hours continues`() {
        val decision = TimerPauseResolver.decide(LocalTime.of(10, 0), workDay)
        assertEquals(TimerTickDecision.Continue, decision)
    }

    @Test
    fun `before work start returns WorkEnded`() {
        val decision = TimerPauseResolver.decide(LocalTime.of(7, 30), workDay)
        assertEquals(TimerTickDecision.WorkEnded, decision)
    }

    @Test
    fun `after work end returns WorkEnded`() {
        val decision = TimerPauseResolver.decide(LocalTime.of(18, 0), workDay)
        assertEquals(TimerTickDecision.WorkEnded, decision)
    }

    @Test
    fun `during lunch returns Pause`() {
        val decision = TimerPauseResolver.decide(LocalTime.of(12, 30), workDay)
        assertTrue(decision is TimerTickDecision.Pause)
        assertEquals(LocalTime.of(13, 0), (decision as TimerTickDecision.Pause).lunchEnd)
    }

    @Test
    fun `at lunch end boundary is continue`() {
        val decision = TimerPauseResolver.decide(LocalTime.of(13, 0), workDay)
        assertEquals(TimerTickDecision.Continue, decision)
    }

    @Test
    fun `at lunch start boundary returns Pause`() {
        val decision = TimerPauseResolver.decide(LocalTime.of(12, 0), workDay)
        assertTrue(decision is TimerTickDecision.Pause)
    }

    @Test
    fun `night-shift wraps across midnight`() {
        val nightShift = workDay.copy(
            workStartHour = 22, workStartMinute = 0,
            workEndHour = 6, workEndMinute = 0,
            lunchStartHour = 1, lunchStartMinute = 0,
            lunchEndHour = 2, lunchEndMinute = 0,
        )
        assertEquals(TimerTickDecision.Continue, TimerPauseResolver.decide(LocalTime.of(23, 30), nightShift))
        assertEquals(TimerTickDecision.Continue, TimerPauseResolver.decide(LocalTime.of(3, 0), nightShift))
        assertTrue(TimerPauseResolver.decide(LocalTime.of(1, 30), nightShift) is TimerTickDecision.Pause)
        assertEquals(TimerTickDecision.WorkEnded, TimerPauseResolver.decide(LocalTime.of(12, 0), nightShift))
    }

    @Test
    fun `isTimeInRange non-wrapping`() {
        assertTrue(TimerPauseResolver.isTimeInRange(LocalTime.of(9, 0), LocalTime.of(8, 0), LocalTime.of(17, 0)))
        assertTrue(!TimerPauseResolver.isTimeInRange(LocalTime.of(7, 30), LocalTime.of(8, 0), LocalTime.of(17, 0)))
        assertTrue(!TimerPauseResolver.isTimeInRange(LocalTime.of(17, 0), LocalTime.of(8, 0), LocalTime.of(17, 0)))
        assertTrue(TimerPauseResolver.isTimeInRange(LocalTime.of(8, 0), LocalTime.of(8, 0), LocalTime.of(17, 0)))
    }

    @Test
    fun `isTimeInRange wrapping across midnight`() {
        assertTrue(TimerPauseResolver.isTimeInRange(LocalTime.of(23, 0), LocalTime.of(22, 0), LocalTime.of(6, 0)))
        assertTrue(TimerPauseResolver.isTimeInRange(LocalTime.of(3, 0), LocalTime.of(22, 0), LocalTime.of(6, 0)))
        assertTrue(!TimerPauseResolver.isTimeInRange(LocalTime.of(12, 0), LocalTime.of(22, 0), LocalTime.of(6, 0)))
    }
}
