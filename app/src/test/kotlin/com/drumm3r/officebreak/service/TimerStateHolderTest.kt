package com.drumm3r.officebreak.service

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStateHolderTest {

    @Test
    fun `initial state is Idle`() {
        val holder = TimerStateHolder()

        assertEquals(TimerState.Idle, holder.state.value)
    }

    @Test
    fun `update changes state`() {
        val holder = TimerStateHolder()
        val running = TimerState.Running(remainingSeconds = 30, totalSeconds = 60)

        holder.update(running)

        assertEquals(running, holder.state.value)
    }

    @Test
    fun `flow emits updates`() = runTest {
        val holder = TimerStateHolder()

        holder.state.test {
            assertEquals(TimerState.Idle, awaitItem())

            holder.update(TimerState.Running(remainingSeconds = 10, totalSeconds = 10))
            assertEquals(TimerState.Running(remainingSeconds = 10, totalSeconds = 10), awaitItem())

            holder.update(TimerState.Expired)
            assertEquals(TimerState.Expired, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }
}
