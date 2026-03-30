package de.mysportsmate.officebreak.service

interface TimerServiceController {
    fun startTimer(durationSeconds: Long, language: String = "system")
    fun resetTimer()
    fun restartTimer(durationSeconds: Long, language: String = "system")
}
