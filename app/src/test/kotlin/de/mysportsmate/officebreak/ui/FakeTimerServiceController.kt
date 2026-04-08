package de.mysportsmate.officebreak.ui

import de.mysportsmate.officebreak.service.TimerServiceController

class FakeTimerServiceController : TimerServiceController {

    val calls = mutableListOf<Call>()

    override fun startTimer(durationSeconds: Long, language: String, freestyle: Boolean) {
        calls.add(Call.Start(durationSeconds, language, freestyle))
    }

    override fun resetTimer() {
        calls.add(Call.Reset)
    }

    override fun restartTimer(durationSeconds: Long, language: String, freestyle: Boolean) {
        calls.add(Call.Restart(durationSeconds, language, freestyle))
    }

    sealed interface Call {
        data class Start(val durationSeconds: Long, val language: String = "system", val freestyle: Boolean = false) : Call
        data object Reset : Call
        data class Restart(val durationSeconds: Long, val language: String = "system", val freestyle: Boolean = false) : Call
    }
}
