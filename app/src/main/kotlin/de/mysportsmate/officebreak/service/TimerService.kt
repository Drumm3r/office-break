package de.mysportsmate.officebreak.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import android.os.SystemClock
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import de.mysportsmate.officebreak.MainActivity
import de.mysportsmate.officebreak.OfficeBreakApp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.AppJson
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.data.resolveEffectiveSchedule
import de.mysportsmate.officebreak.locale.LocaleHelper
import de.mysportsmate.officebreak.widget.WidgetTimerState
import de.mysportsmate.officebreak.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException

sealed interface TimerState {
    data object Idle : TimerState
    data class Running(val remainingSeconds: Long, val totalSeconds: Long) : TimerState
    data class Paused(val remainingSeconds: Long, val totalSeconds: Long) : TimerState
    data object Expired : TimerState
    data object WorkEnded : TimerState
}

class TimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val timerStateHolder = TimerStateHolder.instance
    private var localizedContext: Context = this

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val language = intent?.getStringExtra(EXTRA_LANGUAGE) ?: SettingsRepository.LANGUAGE_SYSTEM
        localizedContext = LocaleHelper.createLocalizedContext(this, language)

        when (intent?.action) {
            ACTION_START -> {
                val totalSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L)
                val freestyle = intent.getBooleanExtra(EXTRA_FREESTYLE, false)
                if (totalSeconds > 0) {
                    startTimer(totalSeconds, freestyle)
                }
            }
            ACTION_RESET -> resetTimer()
            ACTION_RESTART -> {
                val totalSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L)
                val freestyle = intent.getBooleanExtra(EXTRA_FREESTYLE, false)
                if (totalSeconds > 0) {
                    startTimer(totalSeconds, freestyle)
                }
            }
            ACTION_PAUSE_MUSIC -> pauseCustomSound()
            ACTION_RESUME_MUSIC -> resumeCustomSound()
            else -> {
                timerStateHolder.update(TimerState.Idle)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startTimer(totalSeconds: Long, freestyle: Boolean = false) {
        timerJob?.cancel()
        stopAlarmSound()
        stopVibration()
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(EXPIRED_NOTIFICATION_ID)
        acquireWakeLock(totalSeconds)

        val startNotification = buildNotification(localizedContext.getString(R.string.notification_text_running, formatTime(totalSeconds)))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, startNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, startNotification)
        }
        timerStateHolder.update(TimerState.Running(
            remainingSeconds = totalSeconds,
            totalSeconds = totalSeconds,
        ))

        timerJob = scope.launch {
            try {
                writeWidgetState(WidgetTimerState.STATUS_RUNNING, remainingSeconds = totalSeconds, totalSeconds = totalSeconds)
                WidgetUpdater.requestUpdate(this@TimerService, WidgetTimerState.STATUS_RUNNING, totalSeconds, totalSeconds)

                val schedulePrefs = dataStore.data.first()
                val scheduleEnabled = schedulePrefs[SettingsRepository.KEY_WORK_SCHEDULE_ENABLED] ?: false
                val todaySchedule = if (scheduleEnabled) {
                    val scheduleJson = schedulePrefs[SettingsRepository.KEY_WEEK_SCHEDULE]
                    val week = if (scheduleJson != null) {
                        try {
                            AppJson.decodeFromString<List<DaySchedule>>(scheduleJson)
                        } catch (_: Exception) {
                            DEFAULT_WEEK_SCHEDULE
                        }
                    } else {
                        DEFAULT_WEEK_SCHEDULE
                    }
                    val dayIndex = java.time.LocalDate.now().dayOfWeek.ordinal
                    resolveEffectiveSchedule(week, dayIndex)
                } else {
                    null
                }

                var remaining = totalSeconds
                val activeSchedule = if (freestyle) null else todaySchedule
                while (remaining > 0) {
                    delay(1000L)

                    when (val decision = TimerPauseResolver.decide(LocalTime.now(), activeSchedule)) {
                        is TimerTickDecision.WorkEnded -> {
                            timerStateHolder.update(TimerState.WorkEnded)
                            writeWidgetState(WidgetTimerState.STATUS_WORK_ENDED)
                            WidgetUpdater.requestUpdate(this@TimerService, WidgetTimerState.STATUS_WORK_ENDED)
                            releaseWakeLock()
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            return@launch
                        }
                        is TimerTickDecision.Pause -> {
                            timerStateHolder.update(TimerState.Paused(
                                remainingSeconds = remaining,
                                totalSeconds = totalSeconds,
                            ))
                            writeWidgetState(WidgetTimerState.STATUS_PAUSED, remainingSeconds = remaining, totalSeconds = totalSeconds)
                            WidgetUpdater.requestUpdate(this@TimerService, WidgetTimerState.STATUS_PAUSED, remaining, totalSeconds)
                            updateNotification(localizedContext.getString(R.string.notification_text_lunch_pause))
                            while (TimerPauseResolver.decide(LocalTime.now(), activeSchedule) is TimerTickDecision.Pause) {
                                delay(1000L)
                            }
                            timerStateHolder.update(TimerState.Running(
                                remainingSeconds = remaining,
                                totalSeconds = totalSeconds,
                            ))
                            writeWidgetState(WidgetTimerState.STATUS_RUNNING, remainingSeconds = remaining, totalSeconds = totalSeconds)
                            WidgetUpdater.requestUpdate(this@TimerService, WidgetTimerState.STATUS_RUNNING, remaining, totalSeconds)
                        }
                        TimerTickDecision.Continue -> Unit
                    }

                    remaining--
                    timerStateHolder.update(TimerState.Running(
                        remainingSeconds = remaining,
                        totalSeconds = totalSeconds,
                    ))
                    updateNotification(localizedContext.getString(R.string.notification_text_running, formatTime(remaining)))

                    if (remaining > 0) {
                        writeWidgetState(WidgetTimerState.STATUS_RUNNING, remainingSeconds = remaining, totalSeconds = totalSeconds)
                        WidgetUpdater.requestUpdate(this@TimerService, WidgetTimerState.STATUS_RUNNING, remaining, totalSeconds)
                    }
                }
                timerStateHolder.update(TimerState.Expired)
                writeWidgetState(WidgetTimerState.STATUS_EXPIRED)
                WidgetUpdater.requestUpdate(this@TimerService, WidgetTimerState.STATUS_EXPIRED)
                wakeScreen()
                updateNotification(localizedContext.getString(R.string.notification_text_expired))
                showExpiredNotification()
                val soundPrefs = dataStore.data.first()
                val beepVolume = soundPrefs[SettingsRepository.KEY_BEEP_VOLUME] ?: SettingsRepository.DEFAULT_BEEP_VOLUME
                val vibrationOn = soundPrefs[SettingsRepository.KEY_VIBRATION_ENABLED] ?: SettingsRepository.DEFAULT_VIBRATION_ENABLED
                val beepCount = soundPrefs[SettingsRepository.KEY_BEEP_COUNT] ?: SettingsRepository.DEFAULT_BEEP_COUNT
                val customSoundUri = soundPrefs[SettingsRepository.KEY_CUSTOM_SOUND_URI]
                if (beepVolume > 0) {
                    if (customSoundUri != null) {
                        playCustomSound(customSoundUri, beepVolume / 100.0)
                    } else {
                        playAlarmSound(beepCount, beepVolume / 100.0)
                    }
                }
                if (vibrationOn) triggerVibration()
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
        stopVibration()
        timerStateHolder.update(TimerState.Idle)
        releaseWakeLock()
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(EXPIRED_NOTIFICATION_ID)
        scope.launch {
            writeWidgetState(WidgetTimerState.STATUS_IDLE)
            WidgetUpdater.requestUpdate(this@TimerService, WidgetTimerState.STATUS_IDLE)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                setPackage(packageName)
            },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, OfficeBreakApp.CHANNEL_ID)
            .setContentTitle(localizedContext.getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showExpiredNotification() {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            setPackage(packageName)
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
                setPackage(packageName)
            },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                setPackage(packageName)
            },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, OfficeBreakApp.ALERT_CHANNEL_ID)
            .setContentTitle(localizedContext.getString(R.string.notification_title))
            .setContentText(localizedContext.getString(R.string.notification_text_expired))
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

    private fun playAlarmSound(beepCount: Int = 3, volume: Double = 0.8) {
        stopAlarmSound()
        try {
            val sampleRate = 44100
            val beepDurationMs = 150
            val pauseDurationMs = 100
            val frequency = 1000.0

            val beepSamples = (sampleRate * beepDurationMs) / 1000
            val pauseSamples = (sampleRate * pauseDurationMs) / 1000
            val totalSamples = beepCount * beepSamples + (beepCount - 1) * pauseSamples
            val samples = ShortArray(totalSamples)

            var offset = 0
            for (beep in 0 until beepCount) {
                for (i in 0 until beepSamples) {
                    val angle = 2.0 * Math.PI * frequency * i / sampleRate
                    samples[offset + i] = (Math.sin(angle) * Short.MAX_VALUE * volume).toInt().toShort()
                }
                offset += beepSamples
                if (beep < beepCount - 1) {
                    offset += pauseSamples
                }
            }

            val bufferSize = samples.size * 2
            val track = AudioTrack.Builder()
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
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            alarmTrack = track
            track.play()
            track.write(samples, 0, samples.size)
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Failed to play beep sound", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerVibration() {
        stopVibration()
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator = vib

            val pattern = longArrayOf(0, 150, 100, 150, 100, 150)
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Failed to trigger vibration", e)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun playCustomSound(uriString: String, volume: Double) {
        stopAlarmSound()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@TimerService, Uri.parse(uriString))
                prepare()
                val vol = volume.toFloat()
                setVolume(vol, vol)
            }
            mediaPlayer = player
            player.setOnCompletionListener {
                timerStateHolder.updateMusicPlaying(false)
                it.release()
                if (mediaPlayer === it) mediaPlayer = null
            }
            player.start()
            timerStateHolder.updateMusicPlaying(true)
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Failed to play custom sound, falling back to beep", e)
            mediaPlayer?.release()
            mediaPlayer = null
            timerStateHolder.updateMusicPlaying(false)
            playAlarmSound(volume = volume)
        }
    }

    private fun stopCustomSound() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: IllegalStateException) {
                // Already stopped
            }
            it.release()
        }
        mediaPlayer = null
        timerStateHolder.updateMusicPlaying(false)
    }

    private fun pauseCustomSound() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
        } catch (_: IllegalStateException) {
            // Player in invalid state; ignore
        }
        timerStateHolder.updateMusicPlaying(false)
    }

    private fun resumeCustomSound() {
        val player = mediaPlayer
        if (player != null) {
            try {
                player.start()
                timerStateHolder.updateMusicPlaying(true)
                return
            } catch (_: IllegalStateException) {
                // Player invalid; fall through to restart from file
            }
        }
        scope.launch {
            val soundPrefs = dataStore.data.first()
            val beepVolume = soundPrefs[SettingsRepository.KEY_BEEP_VOLUME] ?: SettingsRepository.DEFAULT_BEEP_VOLUME
            val customSoundUri = soundPrefs[SettingsRepository.KEY_CUSTOM_SOUND_URI]
            if (customSoundUri != null && beepVolume > 0) {
                playCustomSound(customSoundUri, beepVolume / 100.0)
            }
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
        stopCustomSound()
    }

    // Uses deprecated flags because setTurnScreenOn() is Activity-only and this is a Service.
    // The full-screen notification intent (showExpiredNotification) is the primary screen-wake
    // mechanism; this is a fallback for devices that don't honor full-screen intents.
    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val screenLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP,
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
        stopVibration()
        scope.cancel()
        releaseWakeLock()
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(2000L) {
                writeWidgetState(WidgetTimerState.STATUS_IDLE)
                WidgetUpdater.requestUpdate(this@TimerService, WidgetTimerState.STATUS_IDLE)
            }
        }
        super.onDestroy()
    }

    private suspend fun writeWidgetState(
        status: String,
        remainingSeconds: Long = 0L,
        totalSeconds: Long = 0L,
    ) {
        try {
            val endRealtime = if (status == WidgetTimerState.STATUS_RUNNING && remainingSeconds > 0) {
                SystemClock.elapsedRealtime() + remainingSeconds * 1000L
            } else {
                0L
            }
            dataStore.edit {
                it[KEY_WIDGET_TIMER_STATUS] = status
                it[KEY_WIDGET_TIMER_END_REALTIME] = endRealtime
                it[KEY_WIDGET_TIMER_TOTAL_SECONDS] = totalSeconds
                it[KEY_WIDGET_TIMER_REMAINING_SECONDS] = remainingSeconds
            }
        } catch (_: Exception) {
            // Non-critical
        }
    }

    companion object {
        val KEY_WIDGET_TIMER_STATUS = stringPreferencesKey("widget_timer_status")
        val KEY_WIDGET_TIMER_END_REALTIME = longPreferencesKey("widget_timer_end_realtime")
        val KEY_WIDGET_TIMER_TOTAL_SECONDS = longPreferencesKey("widget_timer_total_seconds")
        val KEY_WIDGET_TIMER_REMAINING_SECONDS = longPreferencesKey("widget_timer_remaining_seconds")

        const val ACTION_START = "de.mysportsmate.officebreak.ACTION_START"
        const val ACTION_RESET = "de.mysportsmate.officebreak.ACTION_RESET"
        const val ACTION_RESTART = "de.mysportsmate.officebreak.ACTION_RESTART"
        const val ACTION_PAUSE_MUSIC = "de.mysportsmate.officebreak.ACTION_PAUSE_MUSIC"
        const val ACTION_RESUME_MUSIC = "de.mysportsmate.officebreak.ACTION_RESUME_MUSIC"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        const val EXTRA_LANGUAGE = "language"
        const val EXTRA_FREESTYLE = "freestyle"
        const val NOTIFICATION_ID = 1
        const val EXPIRED_NOTIFICATION_ID = 2

        fun formatTime(totalSeconds: Long): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60

            return "%02d:%02d".format(minutes, seconds)
        }
    }
}
