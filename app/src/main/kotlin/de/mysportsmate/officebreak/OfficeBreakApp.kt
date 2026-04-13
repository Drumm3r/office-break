package de.mysportsmate.officebreak

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService
import de.mysportsmate.officebreak.data.AppJson
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.service.WorkScheduleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OfficeBreakApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        rescheduleWorkStartAlarm()
    }

    private fun createNotificationChannel() {
        val timerChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            getString(R.string.notification_channel_alert_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_alert_description)
        }

        getSystemService<NotificationManager>()?.createNotificationChannels(
            listOf(timerChannel, alertChannel),
        )
    }

    private fun rescheduleWorkStartAlarm() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = dataStore.data.first()
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
                    WorkScheduleManager.scheduleNextWorkStartReminder(this@OfficeBreakApp, schedule)
                }
            } catch (e: Exception) {
                android.util.Log.e("OfficeBreakApp", "Failed to reschedule work start alarm", e)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val ALERT_CHANNEL_ID = "timer_alert_channel"
    }
}
