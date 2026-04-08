package de.mysportsmate.officebreak.service

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
    fun `WorkEnded is singleton`() {
        val a = TimerState.WorkEnded
        val b = TimerState.WorkEnded

        assertTrue(a === b)
    }

    @Test
    fun `Paused holds remaining and total seconds`() {
        val state = TimerState.Paused(remainingSeconds = 45, totalSeconds = 120)

        assertEquals(45L, state.remainingSeconds)
        assertEquals(120L, state.totalSeconds)
    }

    @Test
    fun `Paused data class equality`() {
        val a = TimerState.Paused(remainingSeconds = 20, totalSeconds = 60)
        val b = TimerState.Paused(remainingSeconds = 20, totalSeconds = 60)

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

    @Test
    fun `formatTime handles large values`() {
        assertEquals("120:00", TimerService.formatTime(7200))
    }

    @Test
    fun `formatTime single digit minutes and seconds`() {
        assertEquals("01:01", TimerService.formatTime(61))
        assertEquals("00:01", TimerService.formatTime(1))
        assertEquals("01:09", TimerService.formatTime(69))
    }
}
