package de.mysportsmate.officebreak.service

import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.DaySchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WorkScheduleManagerTest {

    private fun calendarAt(
        year: Int = 2026,
        month: Int = Calendar.APRIL,
        day: Int = 20, // Monday
        hour: Int = 7,
        minute: Int = 0,
    ): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }

    @Test
    fun `finds today reminder when before work start`() {
        val now = calendarAt(hour = 7, minute = 0) // before 08:00 work start
        val result = WorkScheduleManager.computeNextWorkStartTime(DEFAULT_WEEK_SCHEDULE, now, reminderDelayMinutes = 10)
        assertNotNull(result)
        assertEquals(8, result!!.get(Calendar.HOUR_OF_DAY))
        assertEquals(10, result.get(Calendar.MINUTE))
    }

    @Test
    fun `skips today when past reminder time and finds next day`() {
        val now = calendarAt(hour = 10, minute = 0) // past today's 08:10 reminder
        val result = WorkScheduleManager.computeNextWorkStartTime(DEFAULT_WEEK_SCHEDULE, now, reminderDelayMinutes = 10)
        assertNotNull(result)
        // Next day = Tuesday
        assertEquals(21, result!!.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `skips disabled weekend and finds Monday`() {
        // Friday 18:00, past Friday's reminder. Saturday and Sunday are disabled in DEFAULT_WEEK_SCHEDULE.
        val friday = calendarAt(day = 24, hour = 18, minute = 0)
        val result = WorkScheduleManager.computeNextWorkStartTime(DEFAULT_WEEK_SCHEDULE, friday, reminderDelayMinutes = 10)
        assertNotNull(result)
        // Next Monday = 27
        assertEquals(27, result!!.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, result.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `returns null when no day enabled`() {
        val allDisabled = DEFAULT_WEEK_SCHEDULE.map { DaySchedule(enabled = false, linked = false) }
        val now = calendarAt()
        val result = WorkScheduleManager.computeNextWorkStartTime(allDisabled, now, reminderDelayMinutes = 10)
        assertNull(result)
    }

    @Test
    fun `reminder delay is additive`() {
        val now = calendarAt(hour = 7, minute = 0)
        val default = WorkScheduleManager.computeNextWorkStartTime(DEFAULT_WEEK_SCHEDULE, now, reminderDelayMinutes = 10)!!
        val custom = WorkScheduleManager.computeNextWorkStartTime(DEFAULT_WEEK_SCHEDULE, now, reminderDelayMinutes = 30)!!
        // same hour, different minute
        assertEquals(8, custom.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, custom.get(Calendar.MINUTE))
        assertEquals(10, default.get(Calendar.MINUTE))
    }
}
