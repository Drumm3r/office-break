package de.mysportsmate.officebreak.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayScheduleTest {

    // --- validated() ---

    @Test
    fun `validated returns unchanged when workStart equals workEnd`() {
        val schedule = DaySchedule(workStartHour = 10, workStartMinute = 0, workEndHour = 10, workEndMinute = 0)
        val result = schedule.validated()
        assertEquals(schedule, result)
    }

    @Test
    fun `validated clamps lunch for night shift schedule`() {
        val schedule = DaySchedule(
            workStartHour = 22, workStartMinute = 0,
            workEndHour = 6, workEndMinute = 0,
            lunchStartHour = 1, lunchStartMinute = 0,
            lunchEndHour = 2, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        // Lunch at 01:00-02:00 is within the night shift (22:00-06:00)
        assertEquals(1, result.lunchStartHour)
        assertEquals(0, result.lunchStartMinute)
        assertEquals(2, result.lunchEndHour)
        assertEquals(0, result.lunchEndMinute)
    }

    @Test
    fun `validated clamps lunch outside night shift to work start`() {
        val schedule = DaySchedule(
            workStartHour = 22, workStartMinute = 0,
            workEndHour = 6, workEndMinute = 0,
            lunchStartHour = 10, lunchStartMinute = 0,
            lunchEndHour = 11, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        // 10:00 is outside 22:00-06:00, should clamp to workStart (22:00)
        assertEquals(22, result.lunchStartHour)
        assertEquals(0, result.lunchStartMinute)
    }

    @Test
    fun `validated clamps lunchStart before workStart to workStart`() {
        val schedule = DaySchedule(
            workStartHour = 8, workStartMinute = 0,
            workEndHour = 17, workEndMinute = 0,
            lunchStartHour = 6, lunchStartMinute = 0,
            lunchEndHour = 13, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        assertEquals(8, result.lunchStartHour)
        assertEquals(0, result.lunchStartMinute)
    }

    @Test
    fun `validated clamps lunchStart after workEnd to workEnd minus 1`() {
        val schedule = DaySchedule(
            workStartHour = 8, workStartMinute = 0,
            workEndHour = 17, workEndMinute = 0,
            lunchStartHour = 18, lunchStartMinute = 0,
            lunchEndHour = 19, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        // workEndMin = 17*60 = 1020, clamped to 1019 = 16:59
        assertEquals(16, result.lunchStartHour)
        assertEquals(59, result.lunchStartMinute)
    }

    @Test
    fun `validated clamps lunchEnd before lunchStart to lunchStart plus 1`() {
        val schedule = DaySchedule(
            workStartHour = 8, workStartMinute = 0,
            workEndHour = 17, workEndMinute = 0,
            lunchStartHour = 12, lunchStartMinute = 0,
            lunchEndHour = 11, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        // lunchStartMin = 720, lunchEnd clamped to 721 = 12:01
        assertEquals(12, result.lunchEndHour)
        assertEquals(1, result.lunchEndMinute)
    }

    @Test
    fun `validated clamps lunchEnd after workEnd to workEnd`() {
        val schedule = DaySchedule(
            workStartHour = 8, workStartMinute = 0,
            workEndHour = 17, workEndMinute = 0,
            lunchStartHour = 12, lunchStartMinute = 0,
            lunchEndHour = 20, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        assertEquals(17, result.lunchEndHour)
        assertEquals(0, result.lunchEndMinute)
    }

    @Test
    fun `validated leaves valid schedule unchanged`() {
        val schedule = DaySchedule(
            workStartHour = 8, workStartMinute = 0,
            workEndHour = 17, workEndMinute = 0,
            lunchStartHour = 12, lunchStartMinute = 0,
            lunchEndHour = 13, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        assertEquals(12, result.lunchStartHour)
        assertEquals(0, result.lunchStartMinute)
        assertEquals(13, result.lunchEndHour)
        assertEquals(0, result.lunchEndMinute)
    }

    @Test
    fun `validated handles lunch at exact workStart boundary`() {
        val schedule = DaySchedule(
            workStartHour = 8, workStartMinute = 0,
            workEndHour = 17, workEndMinute = 0,
            lunchStartHour = 8, lunchStartMinute = 0,
            lunchEndHour = 9, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        assertEquals(8, result.lunchStartHour)
        assertEquals(0, result.lunchStartMinute)
        assertEquals(9, result.lunchEndHour)
        assertEquals(0, result.lunchEndMinute)
    }

    @Test
    fun `validated handles lunch at exact workEnd boundary`() {
        val schedule = DaySchedule(
            workStartHour = 8, workStartMinute = 0,
            workEndHour = 17, workEndMinute = 0,
            lunchStartHour = 16, lunchStartMinute = 0,
            lunchEndHour = 17, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        assertEquals(16, result.lunchStartHour)
        assertEquals(0, result.lunchStartMinute)
        assertEquals(17, result.lunchEndHour)
        assertEquals(0, result.lunchEndMinute)
    }

    @Test
    fun `validated ensures minimum 1 minute lunch duration`() {
        val schedule = DaySchedule(
            workStartHour = 8, workStartMinute = 0,
            workEndHour = 17, workEndMinute = 0,
            lunchStartHour = 12, lunchStartMinute = 0,
            lunchEndHour = 12, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        assertEquals(12, result.lunchStartHour)
        assertEquals(0, result.lunchStartMinute)
        // lunchEnd clamped to lunchStart + 1 = 12:01
        assertEquals(12, result.lunchEndHour)
        assertEquals(1, result.lunchEndMinute)
    }

    @Test
    fun `validated preserves enabled and linked flags`() {
        val schedule = DaySchedule(
            enabled = true,
            linked = true,
            workStartHour = 8, workStartMinute = 0,
            workEndHour = 17, workEndMinute = 0,
            lunchStartHour = 6, lunchStartMinute = 0,
            lunchEndHour = 20, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        assertTrue(result.enabled)
        assertTrue(result.linked)
    }

    @Test
    fun `validated preserves work hours when clamping lunch`() {
        val schedule = DaySchedule(
            workStartHour = 9, workStartMinute = 30,
            workEndHour = 18, workEndMinute = 0,
            lunchStartHour = 6, lunchStartMinute = 0,
            lunchEndHour = 20, lunchEndMinute = 0,
        )
        val result = schedule.validated()
        assertEquals(9, result.workStartHour)
        assertEquals(30, result.workStartMinute)
        assertEquals(18, result.workEndHour)
        assertEquals(0, result.workEndMinute)
    }

    // --- resolveEffectiveSchedule() ---

    @Test
    fun `resolveEffectiveSchedule returns null for negative index`() {
        assertNull(resolveEffectiveSchedule(DEFAULT_WEEK_SCHEDULE, -1))
    }

    @Test
    fun `resolveEffectiveSchedule returns null for index 7`() {
        assertNull(resolveEffectiveSchedule(DEFAULT_WEEK_SCHEDULE, 7))
    }

    @Test
    fun `resolveEffectiveSchedule returns null for disabled day`() {
        // Saturday (index 5) is disabled
        assertNull(resolveEffectiveSchedule(DEFAULT_WEEK_SCHEDULE, 5))
    }

    @Test
    fun `resolveEffectiveSchedule returns day directly when not linked`() {
        val customSchedule = listOf(
            DaySchedule(enabled = true, linked = false, workStartHour = 9, workStartMinute = 0, workEndHour = 18, workEndMinute = 0),
            DaySchedule(enabled = true, linked = false, workStartHour = 10, workStartMinute = 0, workEndHour = 16, workEndMinute = 0),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
        )
        val result = resolveEffectiveSchedule(customSchedule, 1)
        assertNotNull(result)
        assertEquals(10, result!!.workStartHour)
        assertEquals(16, result.workEndHour)
    }

    @Test
    fun `resolveEffectiveSchedule copies times from previous non-linked day`() {
        // Monday is source (not linked), Tuesday is linked
        val result = resolveEffectiveSchedule(DEFAULT_WEEK_SCHEDULE, 1)
        assertNotNull(result)
        // Should inherit Monday's times
        assertEquals(DEFAULT_WEEK_SCHEDULE[0].workStartHour, result!!.workStartHour)
        assertEquals(DEFAULT_WEEK_SCHEDULE[0].workEndHour, result.workEndHour)
        assertEquals(DEFAULT_WEEK_SCHEDULE[0].lunchStartHour, result.lunchStartHour)
        assertEquals(DEFAULT_WEEK_SCHEDULE[0].lunchEndHour, result.lunchEndHour)
    }

    @Test
    fun `resolveEffectiveSchedule skips disabled days when walking back`() {
        val schedule = listOf(
            DaySchedule(enabled = true, linked = false, workStartHour = 9, workStartMinute = 0, workEndHour = 18, workEndMinute = 0),
            DaySchedule(enabled = false, linked = false), // Disabled
            DaySchedule(enabled = true, linked = true),   // Should inherit from Mon (skip Tue)
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
        )
        val result = resolveEffectiveSchedule(schedule, 2)
        assertNotNull(result)
        assertEquals(9, result!!.workStartHour)
        assertEquals(18, result.workEndHour)
    }

    @Test
    fun `resolveEffectiveSchedule skips linked days when walking back`() {
        val schedule = listOf(
            DaySchedule(enabled = true, linked = false, workStartHour = 7, workStartMinute = 30, workEndHour = 16, workEndMinute = 0),
            DaySchedule(enabled = true, linked = true),  // Linked
            DaySchedule(enabled = true, linked = true),  // Linked
            DaySchedule(enabled = true, linked = true),  // Should resolve to Mon
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
        )
        val result = resolveEffectiveSchedule(schedule, 3)
        assertNotNull(result)
        assertEquals(7, result!!.workStartHour)
        assertEquals(30, result.workStartMinute)
        assertEquals(16, result.workEndHour)
    }

    @Test
    fun `resolveEffectiveSchedule wraps around week boundary`() {
        // All linked except Friday
        val schedule = listOf(
            DaySchedule(enabled = true, linked = true),   // Mon - linked
            DaySchedule(enabled = true, linked = true),   // Tue - linked
            DaySchedule(enabled = true, linked = true),   // Wed - linked
            DaySchedule(enabled = true, linked = true),   // Thu - linked
            DaySchedule(enabled = true, linked = false, workStartHour = 10, workStartMinute = 0, workEndHour = 19, workEndMinute = 0), // Fri - source
            DaySchedule(enabled = false, linked = false),  // Sat
            DaySchedule(enabled = false, linked = false),  // Sun
        )
        // Mon is linked -> walks back: Sun(disabled), Sat(disabled), Fri(source!)
        val result = resolveEffectiveSchedule(schedule, 0)
        assertNotNull(result)
        assertEquals(10, result!!.workStartHour)
        assertEquals(19, result.workEndHour)
    }

    @Test
    fun `resolveEffectiveSchedule returns day unchanged when all days are linked`() {
        val schedule = listOf(
            DaySchedule(enabled = true, linked = true, workStartHour = 8, workStartMinute = 0),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = true, linked = true),
        )
        // All linked, no source found -> returns day itself
        val result = resolveEffectiveSchedule(schedule, 0)
        assertNotNull(result)
        assertEquals(8, result!!.workStartHour)
    }

    @Test
    fun `resolveEffectiveSchedule preserves enabled and linked of target day`() {
        val schedule = listOf(
            DaySchedule(enabled = true, linked = false, workStartHour = 9, workStartMinute = 0, workEndHour = 18, workEndMinute = 0),
            DaySchedule(enabled = true, linked = true),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
            DaySchedule(enabled = false, linked = false),
        )
        val result = resolveEffectiveSchedule(schedule, 1)
        assertNotNull(result)
        assertTrue(result!!.enabled)
        assertTrue(result.linked)
    }

    @Test
    fun `resolveEffectiveSchedule with DEFAULT_WEEK_SCHEDULE monday returns monday`() {
        val result = resolveEffectiveSchedule(DEFAULT_WEEK_SCHEDULE, 0)
        assertNotNull(result)
        assertEquals(DEFAULT_WEEK_SCHEDULE[0], result)
    }

    @Test
    fun `resolveEffectiveSchedule with DEFAULT_WEEK_SCHEDULE saturday returns null`() {
        assertNull(resolveEffectiveSchedule(DEFAULT_WEEK_SCHEDULE, 5))
    }

    @Test
    fun `resolveEffectiveSchedule with DEFAULT_WEEK_SCHEDULE sunday returns null`() {
        assertNull(resolveEffectiveSchedule(DEFAULT_WEEK_SCHEDULE, 6))
    }

    // --- DEFAULT_WEEK_SCHEDULE ---

    @Test
    fun `DEFAULT_WEEK_SCHEDULE has 7 days`() {
        assertEquals(7, DEFAULT_WEEK_SCHEDULE.size)
    }

    @Test
    fun `DEFAULT_WEEK_SCHEDULE weekdays are enabled`() {
        for (i in 0..4) {
            assertTrue("Day $i should be enabled", DEFAULT_WEEK_SCHEDULE[i].enabled)
        }
    }

    @Test
    fun `DEFAULT_WEEK_SCHEDULE weekend is disabled`() {
        assertFalse(DEFAULT_WEEK_SCHEDULE[5].enabled)
        assertFalse(DEFAULT_WEEK_SCHEDULE[6].enabled)
    }

    @Test
    fun `DEFAULT_WEEK_SCHEDULE monday is not linked`() {
        assertFalse(DEFAULT_WEEK_SCHEDULE[0].linked)
    }

    @Test
    fun `DEFAULT_WEEK_SCHEDULE tuesday through friday are linked`() {
        for (i in 1..4) {
            assertTrue("Day $i should be linked", DEFAULT_WEEK_SCHEDULE[i].linked)
        }
    }

    @Test
    fun `DEFAULT_WEEK_SCHEDULE default work hours are 8 to 17`() {
        val monday = DEFAULT_WEEK_SCHEDULE[0]
        assertEquals(8, monday.workStartHour)
        assertEquals(0, monday.workStartMinute)
        assertEquals(17, monday.workEndHour)
        assertEquals(0, monday.workEndMinute)
    }

    @Test
    fun `DEFAULT_WEEK_SCHEDULE default lunch hours are 12 to 13`() {
        val monday = DEFAULT_WEEK_SCHEDULE[0]
        assertEquals(12, monday.lunchStartHour)
        assertEquals(0, monday.lunchStartMinute)
        assertEquals(13, monday.lunchEndHour)
        assertEquals(0, monday.lunchEndMinute)
    }
}
