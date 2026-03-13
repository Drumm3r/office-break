package com.drumm3r.officebreak.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.drumm3r.officebreak.MainActivity
import com.drumm3r.officebreak.OfficeBreakApp
import com.drumm3r.officebreak.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface TimerState {
    data object Idle : TimerState
    data class Running(val remainingSeconds: Long, val totalSeconds: Long) : TimerState
    data object Expired : TimerState
}

class TimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmTrack: AudioTrack? = null
    private val timerStateHolder = TimerStateHolder.instance

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val totalSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L)
                if (totalSeconds > 0) {
                    startTimer(totalSeconds)
                }
            }
            ACTION_RESET -> resetTimer()
            ACTION_RESTART -> {
                val totalSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L)
                if (totalSeconds > 0) {
                    startTimer(totalSeconds)
                }
            }
            else -> {
                timerStateHolder.update(TimerState.Idle)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startTimer(totalSeconds: Long) {
        timerJob?.cancel()
        stopAlarmSound()
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(EXPIRED_NOTIFICATION_ID)
        acquireWakeLock(totalSeconds)

        startForeground(NOTIFICATION_ID, buildNotification(formatTime(totalSeconds)))
        timerStateHolder.update(TimerState.Running(
            remainingSeconds = totalSeconds,
            totalSeconds = totalSeconds,
        ))

        timerJob = scope.launch {
            try {
                var remaining = totalSeconds
                while (remaining > 0) {
                    delay(1000L)
                    remaining--
                    timerStateHolder.update(TimerState.Running(
                        remainingSeconds = remaining,
                        totalSeconds = totalSeconds,
                    ))
                    updateNotification(formatTime(remaining))
                }
                timerStateHolder.update(TimerState.Expired)
                wakeScreen()
                stopForeground(STOP_FOREGROUND_REMOVE)
                showExpiredNotification()
                playAlarmSound()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("TimerService", "Timer error", e)
                timerStateHolder.update(TimerState.Idle)
                releaseWakeLock()
            }
        }
    }

    private fun resetTimer() {
        timerJob?.cancel()
        stopAlarmSound()
        timerStateHolder.update(TimerState.Idle)
        releaseWakeLock()
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(EXPIRED_NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(timeText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, OfficeBreakApp.CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text_running, timeText))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(timeText: String) {
        val notification = buildNotification(timeText)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showExpiredNotification() {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(activityIntent)

        val fullScreenIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, OfficeBreakApp.ALERT_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text_expired))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(EXPIRED_NOTIFICATION_ID, notification)
    }

    private fun playAlarmSound() {
        stopAlarmSound()
        try {
            val sampleRate = 44100
            val beepDurationMs = 150
            val pauseDurationMs = 100
            val beepCount = 3
            val frequency = 1000.0

            val beepSamples = (sampleRate * beepDurationMs) / 1000
            val pauseSamples = (sampleRate * pauseDurationMs) / 1000
            val totalSamples = beepCount * beepSamples + (beepCount - 1) * pauseSamples
            val samples = ShortArray(totalSamples)

            var offset = 0
            for (beep in 0 until beepCount) {
                for (i in 0 until beepSamples) {
                    val angle = 2.0 * Math.PI * frequency * i / sampleRate
                    samples[offset + i] = (Math.sin(angle) * Short.MAX_VALUE * 0.8).toInt().toShort()
                }
                offset += beepSamples
                if (beep < beepCount - 1) {
                    offset += pauseSamples
                }
            }

            val bufferSize = samples.size * 2
            alarmTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                .apply {
                    write(samples, 0, samples.size)
                    play()
                }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Failed to play beep sound", e)
        }
    }

    private fun stopAlarmSound() {
        alarmTrack?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
                // Already stopped
            }
            it.release()
        }
        alarmTrack = null
    }

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val screenLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "OfficeBreak::ScreenWakeLock",
        )
        screenLock.acquire(5000L)
    }

    private fun acquireWakeLock(totalSeconds: Long) {
        releaseWakeLock()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "OfficeBreak::TimerWakeLock",
        ).apply {
            acquire(totalSeconds * 1000L + 60_000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    override fun onDestroy() {
        timerJob?.cancel()
        stopAlarmSound()
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.drumm3r.officebreak.ACTION_START"
        const val ACTION_RESET = "com.drumm3r.officebreak.ACTION_RESET"
        const val ACTION_RESTART = "com.drumm3r.officebreak.ACTION_RESTART"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        const val NOTIFICATION_ID = 1
        const val EXPIRED_NOTIFICATION_ID = 2

        fun formatTime(totalSeconds: Long): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60

            return "%02d:%02d".format(minutes, seconds)
        }
    }
}
