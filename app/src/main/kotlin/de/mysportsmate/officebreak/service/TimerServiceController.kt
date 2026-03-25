package de.mysportsmate.officebreak.service

interface TimerServiceController {
    fun startTimer(durationSeconds: Long)
    fun resetTimer()
    fun restartTimer(durationSeconds: Long)
}
