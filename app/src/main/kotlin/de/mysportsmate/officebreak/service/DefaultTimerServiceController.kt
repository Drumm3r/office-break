package de.mysportsmate.officebreak.service

import android.content.Context
import android.content.Intent

class DefaultTimerServiceController(
    private val context: Context,
) : TimerServiceController {

    override fun startTimer(durationSeconds: Long, language: String, freestyle: Boolean) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DURATION_SECONDS, durationSeconds)
            putExtra(TimerService.EXTRA_LANGUAGE, language)
            putExtra(TimerService.EXTRA_FREESTYLE, freestyle)
        }
        context.startForegroundService(intent)
    }

    override fun resetTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
        }
        context.startService(intent)
    }

    override fun restartTimer(durationSeconds: Long, language: String, freestyle: Boolean) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESTART
            putExtra(TimerService.EXTRA_DURATION_SECONDS, durationSeconds)
            putExtra(TimerService.EXTRA_LANGUAGE, language)
            putExtra(TimerService.EXTRA_FREESTYLE, freestyle)
        }
        context.startForegroundService(intent)
    }
}
