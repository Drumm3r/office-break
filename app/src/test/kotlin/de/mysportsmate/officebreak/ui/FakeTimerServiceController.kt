package de.mysportsmate.officebreak.ui

import de.mysportsmate.officebreak.service.TimerServiceController

class FakeTimerServiceController : TimerServiceController {

    val calls = mutableListOf<Call>()

    override fun startTimer(durationSeconds: Long, language: String) {
        calls.add(Call.Start(durationSeconds, language))
    }

    override fun resetTimer() {
        calls.add(Call.Reset)
    }

    override fun restartTimer(durationSeconds: Long, language: String) {
        calls.add(Call.Restart(durationSeconds, language))
    }

    sealed interface Call {
        data class Start(val durationSeconds: Long, val language: String = "system") : Call
        data object Reset : Call
        data class Restart(val durationSeconds: Long, val language: String = "system") : Call
    }
}
