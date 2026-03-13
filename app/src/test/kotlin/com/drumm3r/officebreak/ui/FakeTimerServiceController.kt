package com.drumm3r.officebreak.ui

import com.drumm3r.officebreak.service.TimerServiceController

class FakeTimerServiceController : TimerServiceController {

    val calls = mutableListOf<Call>()

    override fun startTimer(durationSeconds: Long) {
        calls.add(Call.Start(durationSeconds))
    }

    override fun resetTimer() {
        calls.add(Call.Reset)
    }

    override fun restartTimer(durationSeconds: Long) {
        calls.add(Call.Restart(durationSeconds))
    }

    sealed interface Call {
        data class Start(val durationSeconds: Long) : Call
        data object Reset : Call
        data class Restart(val durationSeconds: Long) : Call
    }
}
