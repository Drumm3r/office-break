package de.mysportsmate.officebreak.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import de.mysportsmate.officebreak.MainActivity
import de.mysportsmate.officebreak.OfficeBreakApp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.AppJson
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.locale.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WorkScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "onReceive action=${intent?.action}")

        if (intent?.action == ACTION_START_TIMER) {
            handleStartTimerAction(context)
            return
        }

        val state = TimerStateHolder.instance.state.value
        Log.d(TAG, "Timer state: $state, notifications enabled: ${NotificationManagerCompat.from(context).areNotificationsEnabled()}")

        if (state is TimerState.Idle) {
            showWorkStartNotification(context)
        } else {
            Log.d(TAG, "Timer not idle, skipping notification")
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleForTomorrow(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showWorkStartNotification(context: Context) {
        val localizedContext = LocaleHelper.applyLocaleToContext(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val startTimerIntent = PendingIntent.getBroadcast(
            context,
            START_ACTION_REQUEST_CODE,
            Intent().apply {
                setClass(context, WorkScheduleReceiver::class.java)
                setPackage(context.packageName)
                action = ACTION_START_TIMER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, OfficeBreakApp.ALERT_CHANNEL_ID)
            .setContentTitle(localizedContext.getString(R.string.notification_work_start_title))
            .setContentText(localizedContext.getString(R.string.notification_work_start_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification,
                localizedContext.getString(R.string.notification_work_start_action),
                startTimerIntent,
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun handleStartTimerAction(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(NOTIFICATION_ID)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.dataStore.data.first()
                val hours = prefs[intPreferencesKey("timer_hours")] ?: SettingsRepository.DEFAULT_HOURS
                val minutes = prefs[intPreferencesKey("timer_minutes")] ?: SettingsRepository.DEFAULT_MINUTES
                val totalSeconds = (hours * 3600L) + (minutes * 60L)

                if (totalSeconds > 0) {
                    val language = prefs[stringPreferencesKey("language")] ?: SettingsRepository.LANGUAGE_SYSTEM
                    val serviceIntent = Intent(context, TimerService::class.java).apply {
                        action = TimerService.ACTION_START
                        putExtra(TimerService.EXTRA_DURATION_SECONDS, totalSeconds)
                        putExtra(TimerService.EXTRA_LANGUAGE, language)
                        putExtra(TimerService.EXTRA_FREESTYLE, false)
                    }
                    context.startForegroundService(serviceIntent)

                    val activityIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        setPackage(context.packageName)
                    }
                    context.startActivity(activityIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkScheduleReceiver", "Failed to start timer from notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleForTomorrow(context: Context) {
        try {
            val prefs = context.dataStore.data.first()
            val enabled = prefs[androidx.datastore.preferences.core.booleanPreferencesKey("work_schedule_enabled")] ?: false
            if (enabled) {
                val scheduleJson = prefs[androidx.datastore.preferences.core.stringPreferencesKey("week_schedule")]
                val schedule = if (scheduleJson != null) {
                    try {
                        AppJson.decodeFromString<List<DaySchedule>>(scheduleJson)
                    } catch (_: Exception) {
                        DEFAULT_WEEK_SCHEDULE
                    }
                } else {
                    DEFAULT_WEEK_SCHEDULE
                }
                WorkScheduleManager.scheduleNextWorkStartReminder(context, schedule)
            }
        } catch (e: Exception) {
            android.util.Log.e("WorkScheduleReceiver", "Failed to reschedule alarm", e)
        }
    }

    companion object {
        private const val TAG = "WorkScheduleReceiver"
        private const val NOTIFICATION_ID = 3
        const val ACTION_START_TIMER = "de.mysportsmate.officebreak.ACTION_START_TIMER_FROM_NOTIFICATION"
        private const val START_ACTION_REQUEST_CODE = 1002
    }
}
