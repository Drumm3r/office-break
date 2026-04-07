package de.mysportsmate.officebreak.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import de.mysportsmate.officebreak.MainActivity
import de.mysportsmate.officebreak.OfficeBreakApp
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.data.AppJson
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.locale.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WorkScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val state = TimerStateHolder.instance.state.value
        if (state is TimerState.Idle) {
            showWorkStartNotification(context)
        }

        rescheduleForTomorrow(context)
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, OfficeBreakApp.ALERT_CHANNEL_ID)
            .setContentTitle(localizedContext.getString(R.string.notification_work_start_title))
            .setContentText(localizedContext.getString(R.string.notification_work_start_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun rescheduleForTomorrow(context: Context) {
        try {
            val prefs = runBlocking { context.dataStore.data.first() }
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
        private const val NOTIFICATION_ID = 3
    }
}
