package com.drumm3r.officebreak.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {

    @Test
    fun `Idle is singleton`() {
        val a = TimerState.Idle
        val b = TimerState.Idle

        assertTrue(a === b)
    }

    @Test
    fun `Expired is singleton`() {
        val a = TimerState.Expired
        val b = TimerState.Expired

        assertTrue(a === b)
    }

    @Test
    fun `Running holds remaining and total seconds`() {
        val state = TimerState.Running(remainingSeconds = 30, totalSeconds = 60)

        assertEquals(30L, state.remainingSeconds)
        assertEquals(60L, state.totalSeconds)
    }

    @Test
    fun `Running data class equality`() {
        val a = TimerState.Running(remainingSeconds = 10, totalSeconds = 100)
        val b = TimerState.Running(remainingSeconds = 10, totalSeconds = 100)

        assertEquals(a, b)
    }

    @Test
    fun `formatTime formats correctly`() {
        assertEquals("00:00", TimerService.formatTime(0))
        assertEquals("00:59", TimerService.formatTime(59))
        assertEquals("01:00", TimerService.formatTime(60))
        assertEquals("01:30", TimerService.formatTime(90))
        assertEquals("60:00", TimerService.formatTime(3600))
    }
}
