package com.drumm3r.officebreak.service

import android.content.Context
import android.content.Intent

class DefaultTimerServiceController(
    private val context: Context,
) : TimerServiceController {

    override fun startTimer(durationSeconds: Long) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DURATION_SECONDS, durationSeconds)
        }
        context.startForegroundService(intent)
    }

    override fun resetTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
        }
        context.startService(intent)
    }

    override fun restartTimer(durationSeconds: Long) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESTART
            putExtra(TimerService.EXTRA_DURATION_SECONDS, durationSeconds)
        }
        context.startForegroundService(intent)
    }
}
