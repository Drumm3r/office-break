package de.mysportsmate.officebreak.service

interface TimerServiceController {
    fun startTimer(durationSeconds: Long, language: String = "system", freestyle: Boolean = false)
    fun resetTimer()
    fun restartTimer(durationSeconds: Long, language: String = "system", freestyle: Boolean = false)
    fun pauseMusic()
    fun resumeMusic()
}
